package com.google.ai.edge.gallery.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.data.LlmHttpPrefs
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelAllowlist
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.ByteArrayInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Foreground service exposing a minimal HTTP API for local LLM inference.
 * GET /ping -> {status:"ok"}
 * POST /generate {prompt} -> {text, usage}
 */
class LlmHttpService : Service() {

  private var server: NanoServer? = null
  private val json = Json { ignoreUnknownKeys = true }
  private var currentPort: Int = DEFAULT_PORT
  private var selectedModel: Model? = null
  private val modelsPayload = """{"object":"list","data":[{"id":"local","object":"model"}]}"""

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

    val notification: Notification =
      NotificationCompat.Builder(this, "llm-http")
        .setContentTitle("LLM HTTP Bridge")
        .setContentText("Serving on 127.0.0.1:$port")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .build()
    startForeground(42, notification)

    val model = pickFirstLlmModel()
    if (model == null) {
      stopSelf()
      return START_NOT_STICKY
    }
    selectedModel = model

    server?.stop()
    server = NanoServer(port, model)
    server?.start()

    // Kick off a warm-up in background so the first real request is faster.
    Thread { server?.warmUpModel() }.start()

    return START_STICKY
  }

  override fun onDestroy() {
    server?.stop()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun pickFirstLlmModel(): Model? {
    val allowlist = readAllowlistFromExternalFiles()
    val base = allowlist?.models?.firstOrNull { allowed ->
      allowed.taskTypes.any { it.startsWith("llm", ignoreCase = true) }
    }?.toModel() ?: return null

    // If user imported model into __imports, prefer that path.
    val imported = File(getExternalFilesDir(null), "__imports/gemma-3n-E2B-it-int4.litertlm")
    return if (imported.exists()) base.copy(localModelFilePathOverride = imported.absolutePath) else base
  }

  private fun readAllowlistFromExternalFiles(): ModelAllowlist? {
    return try {
      val baseDir = getExternalFilesDir(null)
      val file = File(baseDir, "model_allowlist.json")
      if (!file.exists()) return null
      val content = file.readText()
      Gson().fromJson(content, ModelAllowlist::class.java)
    } catch (e: Exception) {
      null
    }
  }

  private inner class NanoServer(port: Int, private val model: Model) : NanoHTTPD(port) {
    private val executor = Executors.newSingleThreadExecutor()
    private val inferenceLock = Any()

    override fun serve(session: IHTTPSession): Response {
      return try {
        when (session.method) {
          Method.GET -> when (session.uri) {
            "/ping" -> okJsonText("{\"status\":\"ok\"}")
            "/v1/models" -> okJsonText(modelsPayload)
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
      val payload = HashMap<String, String>()
      session.parseBody(payload)
      val body = payload["postData"] ?: return badRequest("empty body")

      val req = json.decodeFromString<GenReq>(body)
      val text = runLlm(req.prompt) ?: return badRequest("llm error")
      val res = GenRes(text = text, usage = Usage(prompt_tokens = 0, completion_tokens = 0))
      return okJsonText(json.encodeToString(res))
    }

    private fun handleChatCompletion(session: IHTTPSession): Response {
      val payload = HashMap<String, String>()
      session.parseBody(payload)
      val body = payload["postData"] ?: return badRequest("empty body")

      val req = json.decodeFromString<ChatRequest>(body)
      val prompt = req.messages.joinToString("\n") { it.content }
      if (prompt.isBlank()) {
        val fallback = ChatResponse(
          id = "chatcmpl-local",
          created = System.currentTimeMillis() / 1000,
          model = req.model ?: "local-llm",
          choices = listOf(
            ChatChoice(
              index = 0,
              message = ChatMessage(role = "assistant", content = "Hola desde Edge (fallback)"),
              finish_reason = "stop",
            )
          ),
          usage = Usage(prompt_tokens = 0, completion_tokens = 0),
        )
        return okJsonText(json.encodeToString(fallback))
      }
      val text = runLlm(prompt, timeoutSeconds = 45) ?: return badRequest("llm error")

      val resp = ChatResponse(
        id = "chatcmpl-local",
        created = System.currentTimeMillis() / 1000,
        model = req.model ?: "local-llm",
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
      val payload = HashMap<String, String>()
      session.parseBody(payload)
      val body = payload["postData"] ?: return badRequest("empty body")

      val req = json.decodeFromString<ResponsesRequest>(body)
      val modelId = req.model ?: "local"
      val prompt = extractText(req.messages ?: req.input)

      // Always return something quickly to keep Code happy; fall back to a short string when
      // the model is still warming up or times out.
      val text = if (prompt.isBlank()) {
        "Hola desde Edge (fallback)"
      } else {
        runLlm(prompt, timeoutSeconds = 45) ?: "Hola desde Edge (timeout)"
      }

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
    fun warmUpModel() {
      runLlm("Hola", timeoutSeconds = 10)
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

    private fun runLlm(prompt: String, timeoutSeconds: Long = 30): String? {
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
      executor.execute {
        synchronized(inferenceLock) {
          try {
            LlmChatModelHelper.runInference(
              model = model,
              input = prompt,
              resultListener = { partial, done ->
                if (partial.isNotEmpty()) sb.append(partial)
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
      }
      return if (error != null) null else sb.toString()
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
  }

  @Serializable data class GenReq(val prompt: String)

  @Serializable data class Usage(val prompt_tokens: Int, val completion_tokens: Int)

  // Responses API minimal models
  @Serializable data class ResponsesRequest(
    val model: String? = null,
    val input: List<InputMsg>? = null,
    val messages: List<InputMsg>? = null,
    val stream: Boolean? = null,
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

  @Serializable data class ChatMessage(val role: String, val content: String)

  @Serializable data class ChatRequest(
    val model: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val stream: Boolean? = null,
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
