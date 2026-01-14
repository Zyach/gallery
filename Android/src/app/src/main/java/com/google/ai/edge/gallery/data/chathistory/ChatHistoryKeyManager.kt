/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.data.chathistory

import android.content.Context
import android.util.Base64
import java.security.SecureRandom

class ChatHistoryKeyManager(private val context: Context, private val crypto: ChatCryptoManager) {
  private val prefsFile = "chat_history_keys"
  private val prefsKey = "db_passphrase"

  fun getOrCreateDbPassphrase(): ByteArray {
    val prefs = context.getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
    val existing = prefs.getString(prefsKey, null)
    if (existing != null) {
      val encrypted = Base64.decode(existing, Base64.NO_WRAP)
      return crypto.decryptBytes(encrypted)
    }
    val random = ByteArray(32)
    SecureRandom().nextBytes(random)
    val encrypted = crypto.encryptBytes(random)
    prefs.edit().putString(prefsKey, Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    return random
  }
}
