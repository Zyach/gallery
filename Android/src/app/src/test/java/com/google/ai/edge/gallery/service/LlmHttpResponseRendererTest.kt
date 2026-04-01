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
}
