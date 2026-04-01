package com.google.ai.edge.gallery.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object ModelAllowlistJson {
  private val parser = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
  }

  fun decode(content: String): ModelAllowlist = parser.decodeFromString(content)
}
