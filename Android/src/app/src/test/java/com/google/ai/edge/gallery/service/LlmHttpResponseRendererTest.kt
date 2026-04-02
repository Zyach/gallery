package com.google.ai.edge.gallery.service

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmHttpResponseRendererTest {
  private val json = Json { encodeDefaults = true }

  @Test
  fun rendersJsonErrorPayload() {
    assertEquals("{\"error\":\"not_found\"}", LlmHttpResponseRenderer.renderJsonError("not_found"))
  }

  @Test
  fun rendersFallbackModelListWhenIdsEmpty() {
    val payload = LlmHttpResponseRenderer.renderModelListPayload(json, emptyList(), "local")
    assertTrue(payload.contains("\"id\":\"local\""))
  }

  @Test
  fun emitsSseEventFrame() {
    assertEquals("event: ping\ndata: {\"ok\":true}\n\n", LlmHttpResponseRenderer.emitSseEvent("ping", "{\"ok\":true}"))
  }

  @Test
  fun textSsePayloadContainsRequiredEvents() {
    val payload = LlmHttpResponseRenderer.buildTextSsePayload("test-model", "hello world")
    assertTrue(payload.contains("event: response.created"))
    assertTrue(payload.contains("event: response.in_progress"))
    assertTrue(payload.contains("event: response.output_text.delta"))
    assertTrue(payload.contains("event: response.completed"))
    assertTrue(payload.contains("data: [DONE]"))
    assertTrue(payload.contains("\"model\":\"test-model\""))
    assertTrue(payload.contains("hello world"))
  }

  @Test
  fun textSsePayloadEscapesSpecialChars() {
    val payload = LlmHttpResponseRenderer.buildTextSsePayload("m", "line1\nline2 \"quoted\"")
    assertTrue(payload.contains("line1\\nline2"))
    assertTrue(payload.contains("\\\"quoted\\\""))
  }

  @Test
  fun toolCallSsePayloadContainsToolJson() {
    val toolJson = """{"id":"call-1","type":"function","function":{"name":"test","arguments":"{}"}}"""
    val payload = LlmHttpResponseRenderer.buildToolCallSsePayload("m", toolJson)
    assertTrue(payload.contains("output_tool_call"))
    assertTrue(payload.contains(toolJson))
    assertTrue(payload.contains("event: response.completed"))
    assertTrue(payload.contains("data: [DONE]"))
  }

  @Test
  fun emptySsePayloadUsesEmptyText() {
    val payload = LlmHttpResponseRenderer.buildTextSsePayload("m", "")
    assertTrue(payload.contains("\"delta\":\"\""))
    assertTrue(payload.contains("event: response.completed"))
  }
}
