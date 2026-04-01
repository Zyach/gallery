package com.google.ai.edge.gallery.service

object LlmHttpBridgeUtils {
  fun normalizeModelKey(value: String): String =
    value.lowercase().replace(Regex("[^a-z0-9]"), "")

  fun isBearerAuthorized(expectedToken: String, authorizationHeader: String?): Boolean {
    if (expectedToken.isBlank()) return true
    return authorizationHeader == "Bearer $expectedToken"
  }
}
