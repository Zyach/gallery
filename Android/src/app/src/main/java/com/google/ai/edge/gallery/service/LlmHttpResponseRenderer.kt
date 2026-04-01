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
}
