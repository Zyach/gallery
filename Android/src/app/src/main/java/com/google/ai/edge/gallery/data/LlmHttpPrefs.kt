package com.google.ai.edge.gallery.data

import android.content.Context

private const val PREFS_NAME = "llm_http_prefs"
private const val KEY_ENABLED = "enabled"
private const val KEY_PORT = "port"
private const val DEFAULT_PORT = 9006

object LlmHttpPrefs {
  fun isEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

  fun getPort(context: Context): Int =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_PORT, DEFAULT_PORT)

  fun save(context: Context, enabled: Boolean, port: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_ENABLED, enabled)
      .putInt(KEY_PORT, port)
      .apply()
  }
}
