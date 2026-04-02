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
import com.google.ai.edge.gallery.data.ModelAllowlistJson
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.gallery.ui.llmchat.LlmModelInstance
import fi.iki.elonen.NanoHTTPD
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    return LlmHttpModelFactory.buildDefaultModel(
      allowlist = allowlistModels(),
      importsDir = File(getExternalFilesDir(null), "__imports"),
    )
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
        val allowlist = ModelAllowlistJson.decode(content)
        lastAllowlistSource = "external:${file.absolutePath}"
        Log.i(logTag, "Loaded model_allowlist.json models=${allowlist?.models?.size ?: 0} source=$lastAllowlistSource")
        return allowlist
      }

      // Fallback: bundled allowlist in assets.
      val assetStream = assets.open("model_allowlist.json")
      val assetText = InputStreamReader(assetStream).readText()
      val allowlist = ModelAllowlistJson.decode(assetText)
      lastAllowlistSource = "asset"
      Log.i(logTag, "Loaded asset model_allowlist.json models=${allowlist?.models?.size ?: 0} source=$lastAllowlistSource")
      allowlist
    } catch (e: Exception) {
      lastAllowlistSource = "error"
      Log.e(logTag, "Failed to read model_allowlist.json", e)
      null
    }
  }

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
    return LlmHttpBridgeUtils.resolveRequestedModelId(requested)
  }

  private fun selectModel(requestedModel: String?): Model? {
    val requested = requestedModel?.trim().orEmpty()
    if (requested.isEmpty() || requested.equals("local", ignoreCase = true) ||
      requested.equals("default", ignoreCase = true)) {
      return defaultModel
    }

    val allowlist = allowlistModels()
    val allowed = LlmHttpModelResolver.selectAllowedModel(allowlist, requested)
    if (allowed == null) return defaultModel

    return modelCache.getOrPut(allowed.name) {
      LlmHttpModelFactory.buildAllowedModel(
        allowedModel = allowed,
        importsDir = File(getExternalFilesDir(null), "__imports"),
      )
    }
  }

  private fun nextRequestId(): String = "r${requestCounter.incrementAndGet()}"

  private fun payloadLoggingEnabled(): Boolean =
    LlmHttpPrefs.isPayloadLoggingEnabled(this)

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

  private inner class NanoServer(port: Int) : NanoHTTPD("127.0.0.1", port) {
    private val executor = Executors.newSingleThreadExecutor()
    private val inferenceLock = Any()

    override fun serve(session: IHTTPSession): Response {
      return try {
        if (!LlmHttpRouteResolver.isSupportedMethod(session.method)) {
          return methodNotAllowed()
        }

        val route = LlmHttpRouteResolver.resolve(session.method, session.uri) ?: return notFound()
        if (route.requiresAuth) {
          requireAuth(session)?.let { return it }
        }

        when (route.handler) {
          LlmHttpRouteHandler.PING -> okJsonText("{\"status\":\"ok\"}")
          LlmHttpRouteHandler.MODELS -> okJsonText(modelsPayload())
          LlmHttpRouteHandler.GENERATE -> handleGenerate(session)
          LlmHttpRouteHandler.CHAT_COMPLETIONS -> handleChatCompletion(session)
          LlmHttpRouteHandler.RESPONSES -> handleResponses(session)
        }
      } catch (t: Throwable) {
        jsonError(Response.Status.INTERNAL_ERROR, t.message ?: "internal_error")
      }
    }

    private fun handleGenerate(session: IHTTPSession): Response {
      val requestId = nextRequestId()
      val payload = HashMap<String, String>()
      session.parseBody(payload)
      val parsedBody = LlmHttpBodyParser.parse(payload["postData"], maxBodyBytes)
      if (payload["postData"] == null) {
        return badRequest("empty body")
      }
      if (parsedBody == null) {
        val bodyBytes = LlmHttpBodyParser.bodySizeBytes(payload["postData"]!!)
        logEvent("request_rejected id=$requestId endpoint=/generate reason=payload_too_large bytes=$bodyBytes")
        return payloadTooLarge()
      }
      val body = parsedBody.body
      val bodyBytes = parsedBody.bodyBytes

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
      val parsedBody = LlmHttpBodyParser.parse(payload["postData"], maxBodyBytes)
      if (payload["postData"] == null) {
        return badRequest("empty body")
      }
      if (parsedBody == null) {
        val bodyBytes = LlmHttpBodyParser.bodySizeBytes(payload["postData"]!!)
        logEvent("request_rejected id=$requestId endpoint=/v1/chat/completions reason=payload_too_large bytes=$bodyBytes")
        return payloadTooLarge()
      }
      val body = parsedBody.body
      val bodyBytes = parsedBody.bodyBytes

      logPayload("POST /v1/chat/completions raw", body, requestId)

      val req = json.decodeFromString<ChatRequest>(body)
      if ((req.tools.isNullOrEmpty()) && req.tool_choice == "required") {
        return badRequest("tool_choice required but tools empty")
      }
      val prompt = req.messages.joinToString("\n") { it.content }
      logPayload("POST /v1/chat/completions prompt", prompt, requestId)
      val requestedId = resolveModelId(req.model)
      val resolvedModel = selectModel(req.model) ?: return notFound("model_not_found")
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
        val toolCall =
          LlmHttpRequestAdapter.synthesizeToolCall(
            tool = req.tools!!.first(),
            prompt = prompt,
            callId = "call_${System.currentTimeMillis()}",
          )
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
      val parsedBody = LlmHttpBodyParser.parse(payload["postData"], maxBodyBytes)
      if (payload["postData"] == null) {
        return badRequest("empty body")
      }
      if (parsedBody == null) {
        val bodyBytes = LlmHttpBodyParser.bodySizeBytes(payload["postData"]!!)
        logEvent("request_rejected id=$requestId endpoint=/v1/responses reason=payload_too_large bytes=$bodyBytes")
        return payloadTooLarge()
      }
      val body = parsedBody.body
      val bodyBytes = parsedBody.bodyBytes

      logPayload("POST /v1/responses raw", body, requestId)

      val req = json.decodeFromString<ResponsesRequest>(body)
      if ((req.tools.isNullOrEmpty()) && req.tool_choice == "required") {
        return badRequest("tool_choice required but tools empty")
      }
      val requestedId = resolveModelId(req.model)
      val resolvedModel = selectModel(req.model) ?: return notFound("model_not_found")
      val resolvedName = resolvedModel.name
      val modelId = resolvedName
      val prompt = LlmHttpRequestAdapter.extractLatestUserText(req.messages ?: req.input)

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
        val toolCall =
          LlmHttpRequestAdapter.synthesizeToolCall(
            tool = req.tools!!.first(),
            prompt = prompt,
            callId = "call_${System.currentTimeMillis()}",
          )
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
            systemInstruction = null,
          )
          if (err.isNotEmpty()) return null
        }
      }

      val sb = StringBuilder()
      val inferenceLatch = CountDownLatch(1)
      val requestLifecycleLatch = CountDownLatch(1)
      var error: String? = null
      val startMs = SystemClock.elapsedRealtime()
      var firstTokenMs: Long? = null
      executor.execute {
        synchronized(inferenceLock) {
          try {
            // Keep the model warm, but reset conversation state for every HTTP request while
            // holding the same lock used for generation so overlapping requests cannot invalidate
            // an in-flight conversation and each request starts from a clean conversation state.
            LlmChatModelHelper.resetConversation(
              model = model,
              supportImage = false,
              supportAudio = false,
              systemInstruction = null,
            )
            LlmChatModelHelper.runInference(
              model = model,
              input = prompt,
              resultListener = { partial, done, _ ->
                if (partial.isNotEmpty()) {
                  if (firstTokenMs == null) {
                    firstTokenMs = SystemClock.elapsedRealtime() - startMs
                  }
                  sb.append(partial)
                }
                if (done) inferenceLatch.countDown()
              },
              cleanUpListener = {},
              onError = { e -> error = e; inferenceLatch.countDown() },
            )

            // Hold the request lock for the full lifetime of the async inference so no later
            // request can reset the conversation while this one is still generating.
            val completed = inferenceLatch.await(timeoutSeconds, TimeUnit.SECONDS)
            if (!completed && error == null) {
              error = "timeout"
              cancelInference(
                model = model,
                requestId = requestId,
                endpoint = endpoint,
                reason = "timeout",
              )
              LlmChatModelHelper.resetConversation(
                model = model,
                supportImage = false,
                supportAudio = false,
                systemInstruction = null,
              )
            } else if (error != null) {
              cancelInference(model = model, requestId = requestId, endpoint = endpoint, reason = "error")
            }
          } catch (t: Throwable) {
            error = t.message
            inferenceLatch.countDown()
          } finally {
            requestLifecycleLatch.countDown()
          }
        }
      }
      val completed = requestLifecycleLatch.await(timeoutSeconds + 5, TimeUnit.SECONDS)
      if (!completed && error == null) {
        error = "timeout"
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
      val ssePayload = LlmHttpResponseRenderer.buildTextSsePayload(modelId, text)
      return chunkedSseResponse(ssePayload)
    }

    private fun sseResponseToolCall(modelId: String, toolCall: ToolCall): Response {
      val toolJson = json.encodeToString(toolCall)
      val ssePayload = LlmHttpResponseRenderer.buildToolCallSsePayload(modelId, toolJson)
      return chunkedSseResponse(ssePayload)
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
      return sseResponse(modelId, "")
    }

    private fun chunkedSseResponse(payload: String): Response {
      val input = ByteArrayInputStream(payload.toByteArray(Charsets.UTF_8))
      val resp = newChunkedResponse(Response.Status.OK, "text/event-stream", input)
      resp.addHeader("Cache-Control", "no-cache")
      resp.addHeader("Connection", "keep-alive")
      return resp
    }

    private fun okJsonText(body: String): Response =
      newFixedLengthResponse(Response.Status.OK, "application/json", body)

    private fun requireAuth(session: IHTTPSession): Response? {
      val expectedToken = LlmHttpPrefs.getBearerToken(this@LlmHttpService)
      if (expectedToken.isBlank()) return null

      val header = session.headers["authorization"] ?: session.headers["Authorization"] ?: ""
      if (LlmHttpBridgeUtils.isBearerAuthorized(expectedToken, header)) return null

      return unauthorized("unauthorized")
    }

    private fun jsonError(status: Response.Status, error: String): Response =
      newFixedLengthResponse(status, "application/json", LlmHttpResponseRenderer.renderJsonError(error))

    private fun badRequest(msg: String): Response =
      jsonError(Response.Status.BAD_REQUEST, msg)

    private fun notFound(error: String = "not_found"): Response =
      jsonError(Response.Status.NOT_FOUND, error)

    private fun unauthorized(error: String): Response {
      val response = jsonError(Response.Status.UNAUTHORIZED, error)
      response.addHeader("WWW-Authenticate", "Bearer")
      return response
    }

    private fun methodNotAllowed(): Response =
      jsonError(Response.Status.METHOD_NOT_ALLOWED, "method_not_allowed")

    private fun payloadTooLarge(): Response =
      jsonError(Response.Status.BAD_REQUEST, "payload_too_large")
  }

  private fun modelsPayload(): String {
    val allowed = allowlistModels()
    val fallbackId = defaultModel?.name ?: "local"
    if (allowed.isEmpty()) {
      Log.w(logTag, "Models list empty. source=$lastAllowlistSource fallback=$fallbackId")
    } else {
      Log.i(logTag, "Models list size=${allowed.size} source=$lastAllowlistSource")
    }
    return LlmHttpResponseRenderer.renderModelListPayload(
      json = json,
      modelIds = allowed.map { item -> item.name },
      fallbackId = fallbackId,
    )
  }

  @Serializable data class GenReq(val prompt: String)

  companion object {
    const val EXTRA_PORT = "extra_port"
    const val DEFAULT_PORT = 9006
  }
}
