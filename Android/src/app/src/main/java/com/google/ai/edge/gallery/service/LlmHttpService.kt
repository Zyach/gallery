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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

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
      val text = runLlm(prompt) ?: return badRequest("llm error")

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
      val prompt = extractText(req.messages ?: req.input)
      val text = runLlm(prompt) ?: return badRequest("llm error")

      val resp = ResponsesResponse(
        id = "resp-local",
        created = System.currentTimeMillis() / 1000,
        model = req.model ?: "local",
        output = listOf(
          RespMessage(
            role = "assistant",
            content = listOf(RespContent(type = "text", text = text)),
            finish_reason = "stop",
          )
        ),
        usage = Usage(prompt_tokens = 0, completion_tokens = 0),
      )

      // Minimal implementation: sin streaming (stream ignorado)
      return okJsonText(json.encodeToString(resp))
    }

    private fun extractText(msgs: List<InputMsg>?): String {
      if (msgs == null) return ""
      val parts = mutableListOf<String>()
      msgs.forEach { m ->
        if (m.role.equals("user", ignoreCase = true)) {
          m.content.forEach { c ->
            if (c.type == "text" || c.type == "input_text") parts.add(c.text)
          }
        }
      }
      return parts.joinToString("\n")
    }

    private fun runLlm(prompt: String): String? {
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
      latch.await()
      return if (error != null) null else sb.toString()
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
    const val DEFAULT_PORT = 9000
  }
}
