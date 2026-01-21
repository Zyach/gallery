package com.google.ai.edge.gallery.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentCallbacks2
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.ai.edge.gallery.BuildConfig
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.data.LlmHttpPrefs
import com.google.ai.edge.gallery.data.AllowedModel
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelAllowlist
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.gallery.ui.llmchat.LlmModelInstance
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.io.ByteArrayInputStream
import java.io.FileWriter
import java.io.InputStreamReader
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Foreground service exposing a minimal HTTP API for local LLM inference.
 * GET /ping -> {status:"ok"}
 * POST /generate {prompt} -> {text, usage}
 */
class LlmHttpService : Service() {

  private var server: NanoServer? = null
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }
  private var currentPort: Int = DEFAULT_PORT
  private var defaultModel: Model? = null
  private val modelCache: MutableMap<String, Model> = mutableMapOf()
  private var cachedAllowlist: ModelAllowlist? = null
  private var lastAllowlistSource: String = "unknown"
  private val logTag = "LlmHttpService"
  private val maxLogChars = 2000
  private val maxBodyBytes = 512 * 1024
  private val logFileMaxBytes = 512 * 1024L
  private val requestCounter = AtomicLong(0)
  private val logExecutor = Executors.newSingleThreadExecutor()

  override fun onCreate() {
    super.onCreate()
    val channelId = "llm-http"
    val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val ch = NotificationChannel(channelId, "LLM HTTP", NotificationManager.IMPORTANCE_LOW)
      mgr.createNotificationChannel(ch)
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val port = intent?.getIntExtra(EXTRA_PORT, DEFAULT_PORT) ?: LlmHttpPrefs.getPort(this)
    currentPort = port

    val model = pickDefaultLlmModel()
    if (model == null) {
      stopSelf()
      return START_NOT_STICKY
    }
    defaultModel = model
    modelCache[model.name] = model

    val notification: Notification =
      NotificationCompat.Builder(this, "llm-http")
        .setContentTitle("LLM HTTP Bridge")
        .setContentText("Serving on 127.0.0.1:$port")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .build()
    startForeground(42, notification)

    server?.stop()
    server = NanoServer(port)
    server?.start()

    // Kick off a warm-up in background so the first real request is faster.
    Thread { server?.warmUpModel(model) }.start()

    return START_STICKY
  }

  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
      System.gc()
    }
  }

  override fun onDestroy() {
    server?.stop()
    logExecutor.shutdown()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun pickDefaultLlmModel(): Model? {
    val allowlist = allowlistModels()
    val base = allowlist.firstOrNull { allowed ->
      allowed.taskTypes.any { it.startsWith("llm", ignoreCase = true) }
    }?.toModel() ?: return null

    // If user imported model into __imports, prefer that path.
    val imported = File(getExternalFilesDir(null), "__imports/gemma-3n-E2B-it-int4.litertlm")
    return if (imported.exists()) base.copy(localModelFilePathOverride = imported.absolutePath) else base
  }

  private fun readAllowlistFromExternalFiles(): ModelAllowlist? {
    return try {
      val baseDir = getExternalFilesDir(null)
      var file = File(baseDir, "model_allowlist.json")
      if (!file.exists()) {
        val fallbackDir = File("/sdcard/Android/data/${packageName}/files")
        file = File(fallbackDir, "model_allowlist.json")
      }
      if (file.exists()) {
        val content = file.readText()
        val allowlist = Gson().fromJson(content, ModelAllowlist::class.java)
        lastAllowlistSource = "external:${file.absolutePath}"
        Log.i(logTag, "Loaded model_allowlist.json models=${allowlist?.models?.size ?: 0} source=$lastAllowlistSource")
        return allowlist
      }

      // Fallback: bundled allowlist in assets.
      val assetStream = assets.open("model_allowlist.json")
      val assetText = InputStreamReader(assetStream).readText()
      val allowlist = Gson().fromJson(assetText, ModelAllowlist::class.java)
      lastAllowlistSource = "asset"
      Log.i(logTag, "Loaded asset model_allowlist.json models=${allowlist?.models?.size ?: 0} source=$lastAllowlistSource")
      allowlist
    } catch (e: Exception) {
      lastAllowlistSource = "error"
      Log.e(logTag, "Failed to read model_allowlist.json", e)
      null
    }
  }

  private fun normalizeModelKey(value: String): String =
    value.lowercase().replace(Regex("[^a-z0-9]"), "")

  private fun allowlistModels(): List<AllowedModel> {
    val fresh = readAllowlistFromExternalFiles()
    if (fresh != null && fresh.models.isNotEmpty()) {
      cachedAllowlist = fresh
      return fresh.models
    }
    if (lastAllowlistSource == "unknown") {
      lastAllowlistSource = "empty"
    }
    return cachedAllowlist?.models ?: emptyList()
  }

  private fun resolveModelId(requested: String?): String {
    if (requested.isNullOrBlank()) return "local"
    return requested
  }

  private fun selectModel(requestedModel: String?): Model? {
    val requested = requestedModel?.trim().orEmpty()
    if (requested.isEmpty() || requested.equals("local", ignoreCase = true) ||
      requested.equals("default", ignoreCase = true)) {
      return defaultModel
    }

    val key = normalizeModelKey(requested)
    val allowlist = allowlistModels()
    val allowed = allowlist.firstOrNull { model ->
      normalizeModelKey(model.name) == key || normalizeModelKey(model.modelId) == key
    }
    if (allowed == null) return defaultModel

    return modelCache.getOrPut(allowed.name) {
      val base = allowed.toModel()
      val importsDir = File(getExternalFilesDir(null), "__imports")
      val namedImport = File(importsDir, "${allowed.name}.litertlm")
      if (namedImport.exists()) base.copy(localModelFilePathOverride = namedImport.absolutePath)
      else base
    }
  }

  private fun nextRequestId(): String = "r${requestCounter.incrementAndGet()}"

  private fun payloadLoggingEnabled(): Boolean =
    BuildConfig.DEBUG || LlmHttpPrefs.isPayloadLoggingEnabled(this)

  private fun logEvent(message: String) {
    val line = "LLM_HTTP $message"
    Log.i(logTag, line)
    if (payloadLoggingEnabled()) {
      logToFile(line)
    }
  }

  private fun logPayload(label: String, body: String, requestId: String) {
    if (!payloadLoggingEnabled()) return
    val trimmed =
      if (body.length <= maxLogChars) body else body.take(maxLogChars) + "...(truncated)"
    val line = "LLM_HTTP payload id=$requestId label=\"$label\" bytes=${body.length} data=\"$trimmed\""
    Log.d(logTag, line)
    logToFile(line)
  }

  private fun logToFile(line: String) {
    val baseDir = getExternalFilesDir(null) ?: return
    val logDir = File(baseDir, "edgegallery")
    val logFile = File(logDir, "llm_http.log")
    val stampedLine = "${System.currentTimeMillis()} $line\n"
    logExecutor.execute {
      try {
        if (!logDir.exists()) logDir.mkdirs()
        if (logFile.exists() && logFile.length() > logFileMaxBytes) {
          val rotated = File(logDir, "llm_http.log.1")
          if (rotated.exists()) rotated.delete()
          logFile.renameTo(rotated)
        }
        FileWriter(logFile, true).use { it.append(stampedLine) }
      } catch (_: Exception) {
      }
    }
  }

  private inner class NanoServer(port: Int) : NanoHTTPD(port) {
    private val executor = Executors.newSingleThreadExecutor()
    private val inferenceLock = Any()

    override fun serve(session: IHTTPSession): Response {
      return try {
        when (session.method) {
          Method.GET -> when (session.uri) {
            "/ping" -> okJsonText("{\"status\":\"ok\"}")
            "/v1/models" -> okJsonText(modelsPayload())
            "/debug/models" -> okJsonText(modelsPayload())
            else -> notFound()
          }
          Method.POST -> when (session.uri) {
            "/generate" -> handleGenerate(session)
            "/v1/chat/completions" -> handleChatCompletion(session)
            "/v1/responses" -> handleResponses(session)
            else -> notFound()
          }
          else -> methodNotAllowed()
        }
      } catch (t: Throwable) {
        newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, t.message)
      }
    }

    private fun handleGenerate(session: IHTTPSession): Response {
      val requestId = nextRequestId()
      val payload = HashMap<String, String>()
      session.parseBody(payload)
      val body = payload["postData"] ?: return badRequest("empty body")
      val bodyBytes = body.toByteArray(Charsets.UTF_8).size
      if (bodyBytes > maxBodyBytes) {
        logEvent("request_rejected id=$requestId endpoint=/generate reason=payload_too_large bytes=$bodyBytes")
        return payloadTooLarge()
      }

      logPayload("POST /generate raw", body, requestId)

      val req = json.decodeFromString<GenReq>(body)
      logPayload("POST /generate prompt", req.prompt, requestId)
      val resolvedModel = selectModel(null) ?: return badRequest("llm error")
      logEvent(
        "request_start id=$requestId endpoint=/generate bodyBytes=$bodyBytes promptChars=${req.prompt.length} model=default"
      )
      val text = runLlm(
        model = resolvedModel,
        prompt = req.prompt,
        requestId = requestId,
        endpoint = "/generate",
      )
        ?: return badRequest("llm error")
      val res = GenRes(text = text, usage = Usage(prompt_tokens = 0, completion_tokens = 0))
      return okJsonText(json.encodeToString(res))
    }

    private fun handleChatCompletion(session: IHTTPSession): Response {
      val requestId = nextRequestId()
      val payload = HashMap<String, String>()
      session.parseBody(payload)
      val body = payload["postData"] ?: return badRequest("empty body")
      val bodyBytes = body.toByteArray(Charsets.UTF_8).size
      if (bodyBytes > maxBodyBytes) {
        logEvent("request_rejected id=$requestId endpoint=/v1/chat/completions reason=payload_too_large bytes=$bodyBytes")
        return payloadTooLarge()
      }

      logPayload("POST /v1/chat/completions raw", body, requestId)

      val req = json.decodeFromString<ChatRequest>(body)
      if ((req.tools.isNullOrEmpty()) && req.tool_choice == "required") {
        return badRequest("tool_choice required but tools empty")
      }
      val prompt = req.messages.joinToString("\n") { it.content }
      logPayload("POST /v1/chat/completions prompt", prompt, requestId)
      val requestedId = resolveModelId(req.model)
      val resolvedModel = selectModel(req.model) ?: return badRequest("llm error")
      val resolvedName = resolvedModel.name
      logEvent(
        "request_start id=$requestId endpoint=/v1/chat/completions bodyBytes=$bodyBytes promptChars=${prompt.length} model=$requestedId resolved=$resolvedName"
      )
      if (prompt.isBlank()) {
        val fallback = ChatResponse(
          id = "chatcmpl-local",
          created = System.currentTimeMillis() / 1000,
          model = resolvedName,
          choices = listOf(
            ChatChoice(
              index = 0,
              message = ChatMessage(role = "assistant", content = "Hola desde Edge (fallback)"),
              finish_reason = "stop",
            )
          ),
          usage = Usage(prompt_tokens = 0, completion_tokens = 0),
        )
        logEvent("request_empty id=$requestId endpoint=/v1/chat/completions")
        return okJsonText(json.encodeToString(fallback))
      }
      // Tool-calls short path: if tools present and we are allowed to emit tool_calls
      if (!req.tools.isNullOrEmpty() && req.tool_choice != "none") {
        val toolCall = synthesizeToolCall(req, prompt)
        val resp = ChatResponse(
          id = "chatcmpl-local",
          created = System.currentTimeMillis() / 1000,
          model = resolvedName,
          choices = listOf(
            ChatChoice(
              index = 0,
              message = ChatMessage(role = "assistant", content = "", tool_calls = listOf(toolCall)),
              finish_reason = "tool_calls",
            )
          ),
          usage = Usage(prompt_tokens = 0, completion_tokens = 0),
        )
        logEvent("request_tool_call id=$requestId endpoint=/v1/chat/completions tool=${toolCall.function.name}")
        return okJsonText(json.encodeToString(resp))
      }

      val text = runLlm(
        model = resolvedModel,
        prompt = prompt,
        timeoutSeconds = 30,
        requestId = requestId,
        endpoint = "/v1/chat/completions",
      )
        ?: return badRequest("llm error")

      val resp = ChatResponse(
        id = "chatcmpl-local",
        created = System.currentTimeMillis() / 1000,
        model = resolvedName,
        choices = listOf(
          ChatChoice(
            index = 0,
            message = ChatMessage(role = "assistant", content = text),
            finish_reason = "stop",
          )
        ),
        usage = Usage(prompt_tokens = 0, completion_tokens = 0),
      )
      return okJsonText(json.encodeToString(resp))
    }

    private fun handleResponses(session: IHTTPSession): Response {
      val requestId = nextRequestId()
      val payload = HashMap<String, String>()
      session.parseBody(payload)
      val body = payload["postData"] ?: return badRequest("empty body")
      val bodyBytes = body.toByteArray(Charsets.UTF_8).size
      if (bodyBytes > maxBodyBytes) {
        logEvent("request_rejected id=$requestId endpoint=/v1/responses reason=payload_too_large bytes=$bodyBytes")
        return payloadTooLarge()
      }

      logPayload("POST /v1/responses raw", body, requestId)

      val req = json.decodeFromString<ResponsesRequest>(body)
      if ((req.tools.isNullOrEmpty()) && req.tool_choice == "required") {
        return badRequest("tool_choice required but tools empty")
      }
      val requestedId = resolveModelId(req.model)
      val resolvedModel = selectModel(req.model) ?: return badRequest("llm error")
      val resolvedName = resolvedModel.name
      val modelId = resolvedName
      val prompt = extractText(req.messages ?: req.input)

      logPayload("POST /v1/responses prompt", prompt, requestId)
      logEvent(
        "request_start id=$requestId endpoint=/v1/responses bodyBytes=$bodyBytes promptChars=${prompt.length} model=$requestedId resolved=$resolvedName"
      )

      if (prompt.isBlank()) {
        logEvent("request_empty id=$requestId endpoint=/v1/responses")
        return emptyResponse(modelId, stream = req.stream == true)
      }

      // Tool-calls short path
      if (!req.tools.isNullOrEmpty() && req.tool_choice != "none") {
        val toolCall = synthesizeToolCallResponses(req, prompt)
        val respBody = ResponsesResponse(
          id = "resp-local",
          created = System.currentTimeMillis() / 1000,
          model = modelId,
          output = listOf(
            RespMessage(
              role = "assistant",
              content = listOf(RespContent(type = "output_tool_call", text = json.encodeToString(toolCall))),
              finish_reason = "tool_calls",
            )
          ),
          usage = Usage(prompt_tokens = 0, completion_tokens = 0),
        )
        val wantsStream = req.stream == true
        if (wantsStream) return sseResponseToolCall(modelId, toolCall)
        return okJsonText(json.encodeToString(respBody))
      }

      val text = runLlm(
        model = resolvedModel,
        prompt = prompt,
        timeoutSeconds = 90,
        requestId = requestId,
        endpoint = "/v1/responses",
      )
        ?: return badRequest("llm error")

      val respBody = ResponsesResponse(
        id = "resp-local",
        created = System.currentTimeMillis() / 1000,
        model = modelId,
        output = listOf(
          RespMessage(
            role = "assistant",
            content = listOf(RespContent(type = "text", text = text)),
            finish_reason = "stop",
          )
        ),
        usage = Usage(prompt_tokens = 0, completion_tokens = 0),
      )

      val wantsStream = req.stream == true
      if (wantsStream) return sseResponse(modelId, text)

      return okJsonText(json.encodeToString(respBody))
    }

    // Warm up the model once after service start to reduce first-token latency.
    fun warmUpModel(model: Model) {
      runLlm(model = model, prompt = "Hola", timeoutSeconds = 10, requestId = "warmup", endpoint = "warmup")
    }

    private fun extractText(msgs: List<InputMsg>?): String {
      if (msgs == null) return ""
      var lastUserText: String? = null
      msgs.forEach { m ->
        if (m.role.equals("user", ignoreCase = true)) {
          // Keep only the last text block of the last user message
          val lastBlock = m.content.lastOrNull { it.type == "text" || it.type == "input_text" }
          if (lastBlock != null) lastUserText = lastBlock.text
        }
      }
      return lastUserText ?: ""
    }

    private fun synthesizeToolCall(req: ChatRequest, prompt: String): ToolCall {
      val tool = req.tools!!.first()
      val function = tool.function
      val argsObj = JsonObject(mapOf("query" to JsonPrimitive(prompt)))
      return ToolCall(
        id = "call_${System.currentTimeMillis()}",
        function = ToolCallFunction(
          name = function.name,
          arguments = json.encodeToString(argsObj),
        ),
      )
    }

    private fun synthesizeToolCallResponses(req: ResponsesRequest, prompt: String): ToolCall {
      val tool = req.tools!!.first()
      val function = tool.function
      val argsObj = JsonObject(mapOf("query" to JsonPrimitive(prompt)))
      return ToolCall(
        id = "call_${System.currentTimeMillis()}",
        function = ToolCallFunction(
          name = function.name,
          arguments = json.encodeToString(argsObj),
        ),
      )
    }

    private fun runLlm(
      model: Model,
      prompt: String,
      timeoutSeconds: Long = 30,
      requestId: String,
      endpoint: String,
    ): String? {
      // Lazy init model once.
      synchronized(this) {
        if (model.instance == null) {
          var err = ""
          LlmChatModelHelper.initialize(
            context = this@LlmHttpService,
            model = model,
            supportImage = false,
            supportAudio = false,
            onDone = { err = it },
          )
          if (err.isNotEmpty()) return null
        }
      }

      val sb = StringBuilder()
      val latch = CountDownLatch(1)
      var error: String? = null
      val startMs = SystemClock.elapsedRealtime()
      var firstTokenMs: Long? = null
      executor.execute {
        synchronized(inferenceLock) {
          try {
            LlmChatModelHelper.runInference(
              model = model,
              input = prompt,
              resultListener = { partial, done ->
                if (partial.isNotEmpty()) {
                  if (firstTokenMs == null) {
                    firstTokenMs = SystemClock.elapsedRealtime() - startMs
                  }
                  sb.append(partial)
                }
                if (done) latch.countDown()
              },
              cleanUpListener = {},
              onError = { e -> error = e; latch.countDown() },
            )
          } catch (t: Throwable) {
            error = t.message
            latch.countDown()
          }
        }
      }
      val completed = latch.await(timeoutSeconds, TimeUnit.SECONDS)
      if (!completed) {
        error = "timeout"
        cancelInference(model = model, requestId = requestId, endpoint = endpoint, reason = "timeout")
        LlmChatModelHelper.resetConversation(
          model = model,
          supportImage = false,
          supportAudio = false,
        )
      } else if (error != null) {
        cancelInference(model = model, requestId = requestId, endpoint = endpoint, reason = "error")
      }
      val totalMs = SystemClock.elapsedRealtime() - startMs
      val ttfbMs = firstTokenMs?.toString() ?: "-1"
      val output = sb.toString()
      return if (error != null) {
        logEvent(
          "request_error id=$requestId endpoint=$endpoint error=$error totalMs=$totalMs ttfbMs=$ttfbMs outputChars=${output.length}"
        )
        null
      } else {
        logEvent(
          "request_done id=$requestId endpoint=$endpoint totalMs=$totalMs ttfbMs=$ttfbMs outputChars=${output.length}"
        )
        output
      }
    }

    private fun cancelInference(model: Model, requestId: String, endpoint: String, reason: String) {
      val instance = model.instance as? LlmModelInstance ?: return
      try {
        instance.conversation.cancelProcess()
        logEvent("request_cancel id=$requestId endpoint=$endpoint reason=$reason")
      } catch (e: Exception) {
        logEvent("request_cancel_error id=$requestId endpoint=$endpoint reason=$reason error=${e.message}")
      }
    }

    private fun sseResponse(modelId: String, text: String): Response {
      val now = System.currentTimeMillis() / 1000
      val respId = "resp-$now"
      val msgId = "msg-$now"

      fun esc(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")

      fun emit(event: String, payload: String): String =
        "event: $event\n" + "data: $payload\n\n"

      val created = """{"type":"response.created","response":{"id":"$respId","object":"response","created_at":$now,"status":"in_progress","model":"$modelId","output":[]}}"""
      val inProgress = """{"type":"response.in_progress","response":{"id":"$respId","object":"response","created_at":$now,"status":"in_progress","model":"$modelId","output":[]}}"""
      val itemAdded = """{"type":"response.output_item.added","item":{"id":"$msgId","type":"message","status":"in_progress","content":[],"role":"assistant"},"output_index":0,"sequence_number":0}"""
      val partAdded = """{"type":"response.content_part.added","content_index":0,"item_id":"$msgId","output_index":0,"part":{"type":"output_text","annotations":[],"logprobs":[],"text":""}}"""
      val delta = """{"type":"response.output_text.delta","content_index":0,"delta":"${esc(text)}","item_id":"$msgId","output_index":0}"""
      val deltaDone = """{"type":"response.output_text.done","content_index":0,"item_id":"$msgId","output_index":0,"text":"${esc(text)}"}"""
      val partDone = """{"type":"response.content_part.done","content_index":0,"item_id":"$msgId","output_index":0,"part":{"type":"output_text","annotations":[],"logprobs":[],"text":"${esc(text)}"}}"""
      val itemDone = """{"type":"response.output_item.done","item":{"id":"$msgId","type":"message","status":"completed","content":[{"type":"output_text","annotations":[],"logprobs":[],"text":"${esc(text)}"}],"role":"assistant"},"output_index":0}"""
      val completed = """{"type":"response.completed","response":{"id":"$respId","object":"response","created_at":$now,"status":"completed","model":"$modelId","output":[{"id":"$msgId","type":"message","status":"completed","content":[{"type":"output_text","annotations":[],"logprobs":[],"text":"${esc(text)}"}],"role":"assistant"}],"usage":{"input_tokens":0,"output_tokens":0,"total_tokens":0}}}"""

      val ssePayload = buildString {
        append(emit("response.created", created))
        append(emit("response.in_progress", inProgress))
        append(emit("response.output_item.added", itemAdded))
        append(emit("response.content_part.added", partAdded))
        append(emit("response.output_text.delta", delta))
        append(emit("response.output_text.done", deltaDone))
        append(emit("response.content_part.done", partDone))
        append(emit("response.output_item.done", itemDone))
        append(emit("response.completed", completed))
        append("data: [DONE]\n\n")
      }

      val input = ByteArrayInputStream(ssePayload.toByteArray(Charsets.UTF_8))
      val resp = newChunkedResponse(Response.Status.OK, "text/event-stream", input)
      resp.addHeader("Cache-Control", "no-cache")
      resp.addHeader("Connection", "keep-alive")
      return resp
    }

    private fun sseResponseToolCall(modelId: String, toolCall: ToolCall): Response {
      val now = System.currentTimeMillis() / 1000
      val respId = "resp-$now"
      val msgId = "msg-$now"

      fun emit(event: String, payload: String): String = "event: $event\n" + "data: $payload\n\n"

      val toolJson = json.encodeToString(toolCall)

      val created = """{"type":"response.created","response":{"id":"$respId","object":"response","created_at":$now,"status":"in_progress","model":"$modelId","output":[]}}"""
      val inProgress = """{"type":"response.in_progress","response":{"id":"$respId","object":"response","created_at":$now,"status":"in_progress","model":"$modelId","output":[]}}"""
      val itemAdded = """{"type":"response.output_item.added","item":{"id":"$msgId","type":"message","status":"in_progress","content":[],"role":"assistant"},"output_index":0,"sequence_number":0}"""
      val partAdded = """{"type":"response.content_part.added","content_index":0,"item_id":"$msgId","output_index":0,"part":{"type":"output_tool_call","tool_call":$toolJson}}"""
      val partDone = """{"type":"response.content_part.done","content_index":0,"item_id":"$msgId","output_index":0,"part":{"type":"output_tool_call","tool_call":$toolJson}}"""
      val itemDone = """{"type":"response.output_item.done","item":{"id":"$msgId","type":"message","status":"completed","content":[{"type":"output_tool_call","tool_call":$toolJson}],"role":"assistant"},"output_index":0}"""
      val completed = """{"type":"response.completed","response":{"id":"$respId","object":"response","created_at":$now,"status":"completed","model":"$modelId","output":[{"id":"$msgId","type":"message","status":"completed","content":[{"type":"output_tool_call","tool_call":$toolJson}],"role":"assistant"}],"usage":{"input_tokens":0,"output_tokens":0,"total_tokens":0}}}"""

      val ssePayload = buildString {
        append(emit("response.created", created))
        append(emit("response.in_progress", inProgress))
        append(emit("response.output_item.added", itemAdded))
        append(emit("response.content_part.added", partAdded))
        append(emit("response.content_part.done", partDone))
        append(emit("response.output_item.done", itemDone))
        append(emit("response.completed", completed))
        append("data: [DONE]\n\n")
      }

      val input = ByteArrayInputStream(ssePayload.toByteArray(Charsets.UTF_8))
      val resp = newChunkedResponse(Response.Status.OK, "text/event-stream", input)
      resp.addHeader("Cache-Control", "no-cache")
      resp.addHeader("Connection", "keep-alive")
      return resp
    }

    private fun emptyResponse(modelId: String, stream: Boolean): Response {
      val respBody = ResponsesResponse(
        id = "resp-local",
        created = System.currentTimeMillis() / 1000,
        model = modelId,
        output = listOf(
          RespMessage(
            role = "assistant",
            content = listOf(RespContent(type = "text", text = "")),
            finish_reason = "stop",
          )
        ),
        usage = Usage(prompt_tokens = 0, completion_tokens = 0),
      )
      if (!stream) return okJsonText(json.encodeToString(respBody))

      val now = System.currentTimeMillis() / 1000
      val respId = "resp-$now"
      val msgId = "msg-$now"

      fun emit(event: String, payload: String): String = "event: $event\n" + "data: $payload\n\n"

      val created = """{"type":"response.created","response":{"id":"$respId","object":"response","created_at":$now,"status":"in_progress","model":"$modelId","output":[]}}"""
      val inProgress = """{"type":"response.in_progress","response":{"id":"$respId","object":"response","created_at":$now,"status":"in_progress","model":"$modelId","output":[]}}"""
      val itemAdded = """{"type":"response.output_item.added","item":{"id":"$msgId","type":"message","status":"in_progress","content":[],"role":"assistant"},"output_index":0,"sequence_number":0}"""
      val partAdded = """{"type":"response.content_part.added","content_index":0,"item_id":"$msgId","output_index":0,"part":{"type":"output_text","annotations":[],"logprobs":[],"text":""}}"""
      val delta = """{"type":"response.output_text.delta","content_index":0,"delta":"","item_id":"$msgId","output_index":0}"""
      val deltaDone = """{"type":"response.output_text.done","content_index":0,"item_id":"$msgId","output_index":0,"text":""}"""
      val partDone = """{"type":"response.content_part.done","content_index":0,"item_id":"$msgId","output_index":0,"part":{"type":"output_text","annotations":[],"logprobs":[],"text":""}}"""
      val itemDone = """{"type":"response.output_item.done","item":{"id":"$msgId","type":"message","status":"completed","content":[{"type":"output_text","annotations":[],"logprobs":[],"text":""}],"role":"assistant"},"output_index":0}"""
      val completed = """{"type":"response.completed","response":{"id":"$respId","object":"response","created_at":$now,"status":"completed","model":"$modelId","output":[{"id":"$msgId","type":"message","status":"completed","content":[{"type":"output_text","annotations":[],"logprobs":[],"text":""}],"role":"assistant"}],"usage":{"input_tokens":0,"output_tokens":0,"total_tokens":0}}}"""

      val ssePayload = buildString {
        append(emit("response.created", created))
        append(emit("response.in_progress", inProgress))
        append(emit("response.output_item.added", itemAdded))
        append(emit("response.content_part.added", partAdded))
        append(emit("response.output_text.delta", delta))
        append(emit("response.output_text.done", deltaDone))
        append(emit("response.content_part.done", partDone))
        append(emit("response.output_item.done", itemDone))
        append(emit("response.completed", completed))
        append("data: [DONE]\n\n")
      }

      val input = ByteArrayInputStream(ssePayload.toByteArray(Charsets.UTF_8))
      val resp = newChunkedResponse(Response.Status.OK, "text/event-stream", input)
      resp.addHeader("Cache-Control", "no-cache")
      resp.addHeader("Connection", "keep-alive")
      return resp
    }

    private fun okJsonText(body: String): Response =
      newFixedLengthResponse(Response.Status.OK, "application/json", body)

    private fun badRequest(msg: String): Response =
      newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, msg)

    private fun notFound(): Response =
      newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")

    private fun methodNotAllowed(): Response =
      newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method not allowed")

    private fun payloadTooLarge(): Response =
      newFixedLengthResponse(
        Response.Status.BAD_REQUEST,
        MIME_PLAINTEXT,
        "Payload too large",
      )
  }

  private fun modelsPayload(): String {
    val allowed = allowlistModels()
    if (allowed.isEmpty()) {
      val fallbackId = defaultModel?.name ?: "local"
      Log.w(logTag, "Models list empty. source=$lastAllowlistSource fallback=$fallbackId")
      return json.encodeToString(ModelList(data = listOf(ModelItem(id = fallbackId))))
    }
    Log.i(logTag, "Models list size=${allowed.size} source=$lastAllowlistSource")
    val models = allowed.map { item -> ModelItem(id = item.name) }
    return json.encodeToString(ModelList(data = models))
  }

  @Serializable data class GenReq(val prompt: String)

  @Serializable data class Usage(val prompt_tokens: Int, val completion_tokens: Int)

  @Serializable data class ModelItem(val id: String, val `object`: String = "model")

  @Serializable data class ModelList(val `object`: String = "list", val data: List<ModelItem>)

  // Responses API minimal models
  @Serializable data class ResponsesRequest(
    val model: String? = null,
    val input: List<InputMsg>? = null,
    val messages: List<InputMsg>? = null,
    val stream: Boolean? = null,
    val tools: List<ToolSpec>? = null,
    val tool_choice: String? = null,
  )
  @Serializable data class InputMsg(
    val role: String,
    val content: List<InputContent>,
  )
  @Serializable data class InputContent(
    val type: String,
    val text: String,
  )
  @Serializable data class ResponsesResponse(
    val id: String,
    val `object`: String = "response",
    val created: Long,
    val model: String,
    val output: List<RespMessage>,
    val usage: Usage,
  )
  @Serializable data class RespMessage(
    val role: String = "assistant",
    val content: List<RespContent>,
    val finish_reason: String = "stop",
  )
  @Serializable data class RespContent(
    val type: String = "text",
    val text: String,
  )

  @Serializable data class GenRes(val text: String, val usage: Usage)

  @Serializable data class ChatMessage(
    val role: String,
    val content: String,
    val tool_calls: List<ToolCall>? = null,
  )
  @Serializable data class ToolCallFunction(val name: String, val arguments: String)
  @Serializable data class ToolCall(val id: String, val type: String = "function", val function: ToolCallFunction)
  @Serializable data class ToolFunctionDef(
    val name: String,
    val description: String? = null,
    val parameters: JsonElement? = null,
  )
  @Serializable data class ToolSpec(val type: String = "function", val function: ToolFunctionDef)

  @Serializable data class ChatRequest(
    val model: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val stream: Boolean? = null,
    val tools: List<ToolSpec>? = null,
    val tool_choice: String? = null,
  )

  @Serializable data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    val finish_reason: String,
  )

  @Serializable data class ChatResponse(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: Usage,
  )

  companion object {
    const val EXTRA_PORT = "extra_port"
    const val DEFAULT_PORT = 9006
  }
}
