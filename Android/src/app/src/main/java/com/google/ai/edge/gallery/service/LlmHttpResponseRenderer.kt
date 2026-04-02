package com.google.ai.edge.gallery.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LlmHttpModelItem(val id: String, val `object`: String = "model")

@Serializable
data class LlmHttpModelList(val `object`: String = "list", val data: List<LlmHttpModelItem>)

object LlmHttpResponseRenderer {
  fun renderJsonError(error: String): String = """{"error":"$error"}"""

  fun renderModelListPayload(json: Json, modelIds: List<String>, fallbackId: String): String {
    val ids = if (modelIds.isEmpty()) listOf(fallbackId) else modelIds
    return json.encodeToString(LlmHttpModelList(data = ids.map { id -> LlmHttpModelItem(id = id) }))
  }

  fun emitSseEvent(event: String, payload: String): String = "event: $event\n" + "data: $payload\n\n"

  fun buildTextSsePayload(modelId: String, text: String): String {
    val now = System.currentTimeMillis() / 1000
    val respId = "resp-$now"
    val msgId = "msg-$now"
    val esc = LlmHttpBridgeUtils.escapeSseText(text)

    return buildString {
      append(emitSseEvent("response.created", """{"type":"response.created","response":{"id":"$respId","object":"response","created_at":$now,"status":"in_progress","model":"$modelId","output":[]}}"""))
      append(emitSseEvent("response.in_progress", """{"type":"response.in_progress","response":{"id":"$respId","object":"response","created_at":$now,"status":"in_progress","model":"$modelId","output":[]}}"""))
      append(emitSseEvent("response.output_item.added", """{"type":"response.output_item.added","item":{"id":"$msgId","type":"message","status":"in_progress","content":[],"role":"assistant"},"output_index":0,"sequence_number":0}"""))
      append(emitSseEvent("response.content_part.added", """{"type":"response.content_part.added","content_index":0,"item_id":"$msgId","output_index":0,"part":{"type":"output_text","annotations":[],"logprobs":[],"text":""}}"""))
      append(emitSseEvent("response.output_text.delta", """{"type":"response.output_text.delta","content_index":0,"delta":"$esc","item_id":"$msgId","output_index":0}"""))
      append(emitSseEvent("response.output_text.done", """{"type":"response.output_text.done","content_index":0,"item_id":"$msgId","output_index":0,"text":"$esc"}"""))
      append(emitSseEvent("response.content_part.done", """{"type":"response.content_part.done","content_index":0,"item_id":"$msgId","output_index":0,"part":{"type":"output_text","annotations":[],"logprobs":[],"text":"$esc"}}"""))
      append(emitSseEvent("response.output_item.done", """{"type":"response.output_item.done","item":{"id":"$msgId","type":"message","status":"completed","content":[{"type":"output_text","annotations":[],"logprobs":[],"text":"$esc"}],"role":"assistant"},"output_index":0}"""))
      append(emitSseEvent("response.completed", """{"type":"response.completed","response":{"id":"$respId","object":"response","created_at":$now,"status":"completed","model":"$modelId","output":[{"id":"$msgId","type":"message","status":"completed","content":[{"type":"output_text","annotations":[],"logprobs":[],"text":"$esc"}],"role":"assistant"}],"usage":{"input_tokens":0,"output_tokens":0,"total_tokens":0}}}"""))
      append("data: [DONE]\n\n")
    }
  }

  fun buildToolCallSsePayload(modelId: String, toolCallJson: String): String {
    val now = System.currentTimeMillis() / 1000
    val respId = "resp-$now"
    val msgId = "msg-$now"

    return buildString {
      append(emitSseEvent("response.created", """{"type":"response.created","response":{"id":"$respId","object":"response","created_at":$now,"status":"in_progress","model":"$modelId","output":[]}}"""))
      append(emitSseEvent("response.in_progress", """{"type":"response.in_progress","response":{"id":"$respId","object":"response","created_at":$now,"status":"in_progress","model":"$modelId","output":[]}}"""))
      append(emitSseEvent("response.output_item.added", """{"type":"response.output_item.added","item":{"id":"$msgId","type":"message","status":"in_progress","content":[],"role":"assistant"},"output_index":0,"sequence_number":0}"""))
      append(emitSseEvent("response.content_part.added", """{"type":"response.content_part.added","content_index":0,"item_id":"$msgId","output_index":0,"part":{"type":"output_tool_call","tool_call":$toolCallJson}}"""))
      append(emitSseEvent("response.content_part.done", """{"type":"response.content_part.done","content_index":0,"item_id":"$msgId","output_index":0,"part":{"type":"output_tool_call","tool_call":$toolCallJson}}"""))
      append(emitSseEvent("response.output_item.done", """{"type":"response.output_item.done","item":{"id":"$msgId","type":"message","status":"completed","content":[{"type":"output_tool_call","tool_call":$toolCallJson}],"role":"assistant"},"output_index":0}"""))
      append(emitSseEvent("response.completed", """{"type":"response.completed","response":{"id":"$respId","object":"response","created_at":$now,"status":"completed","model":"$modelId","output":[{"id":"$msgId","type":"message","status":"completed","content":[{"type":"output_tool_call","tool_call":$toolCallJson}],"role":"assistant"}],"usage":{"input_tokens":0,"output_tokens":0,"total_tokens":0}}}"""))
      append("data: [DONE]\n\n")
    }
  }
}
