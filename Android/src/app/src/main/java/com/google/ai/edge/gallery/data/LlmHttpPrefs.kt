package com.google.ai.edge.gallery.data

import android.content.Context

private const val PREFS_NAME = "llm_http_prefs"
private const val KEY_ENABLED = "enabled"
private const val KEY_PORT = "port"
private const val KEY_HISTORY_ENABLED = "history_enabled"
private const val KEY_PAYLOAD_LOGGING_ENABLED = "payload_logging_enabled"
private const val DEFAULT_PORT = 9006
private const val DEFAULT_HISTORY_ENABLED = false
private const val DEFAULT_PAYLOAD_LOGGING_ENABLED = false

object LlmHttpPrefs {
  fun isEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

  fun getPort(context: Context): Int =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_PORT, DEFAULT_PORT)

  fun isHistoryEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(KEY_HISTORY_ENABLED, DEFAULT_HISTORY_ENABLED)

  fun isPayloadLoggingEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(KEY_PAYLOAD_LOGGING_ENABLED, DEFAULT_PAYLOAD_LOGGING_ENABLED)

  fun setHistoryEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_HISTORY_ENABLED, enabled)
      .apply()
  }

  fun setPayloadLoggingEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_PAYLOAD_LOGGING_ENABLED, enabled)
      .apply()
  }

  fun save(context: Context, enabled: Boolean, port: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_ENABLED, enabled)
      .putInt(KEY_PORT, port)
      .apply()
  }
}
