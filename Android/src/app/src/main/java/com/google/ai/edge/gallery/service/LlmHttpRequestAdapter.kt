package com.google.ai.edge.gallery.service

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object LlmHttpRequestAdapter {
  private val json = Json { ignoreUnknownKeys = true }

  fun extractLatestUserText(msgs: List<InputMsg>?): String {
    if (msgs == null) return ""
    var lastUserText: String? = null
    msgs.forEach { message ->
      if (message.role.equals("user", ignoreCase = true)) {
        val lastBlock = message.content.lastOrNull { it.type == "text" || it.type == "input_text" }
        if (lastBlock != null) {
          lastUserText = lastBlock.text
        }
      }
    }
    return lastUserText.orEmpty()
  }

  fun synthesizeToolCall(tool: ToolSpec, prompt: String, callId: String): ToolCall {
    val argsObj = JsonObject(mapOf("query" to JsonPrimitive(prompt)))
    return ToolCall(
      id = callId,
      function = ToolCallFunction(
        name = tool.function.name,
        arguments = json.encodeToString(argsObj),
      ),
    )
  }
}
