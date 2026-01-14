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

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val KEY_ALIAS = "chat_history_key"
private const val GCM_TAG_BITS = 128
private const val GCM_IV_BYTES = 12

class ChatCryptoManager {

  private fun getOrCreateKey(): SecretKey {
    val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
    keyStore.load(null)
    val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    if (existing != null) {
      return existing
    }

    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
    val spec =
      KeyGenParameterSpec.Builder(
          KEY_ALIAS,
          KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .build()
    keyGenerator.init(spec)
    return keyGenerator.generateKey()
  }

  fun encryptToStream(output: OutputStream): CipherOutputStream {
    val key = getOrCreateKey()
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val iv = cipher.iv
    val dataOutput = DataOutputStream(output)
    dataOutput.writeInt(iv.size)
    dataOutput.write(iv)
    dataOutput.flush()
    return CipherOutputStream(output, cipher)
  }

  fun decryptFromStream(input: InputStream): CipherInputStream {
    val key = getOrCreateKey()
    val dataInput = DataInputStream(input)
    val ivSize = dataInput.readInt()
    val iv = ByteArray(ivSize)
    dataInput.readFully(iv)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
    return CipherInputStream(input, cipher)
  }

  fun encryptBytes(bytes: ByteArray): ByteArray {
    val key = getOrCreateKey()
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val iv = cipher.iv
    val encrypted = cipher.doFinal(bytes)
    val output = java.io.ByteArrayOutputStream()
    val dataOutput = DataOutputStream(output)
    dataOutput.writeInt(iv.size)
    dataOutput.write(iv)
    dataOutput.write(encrypted)
    return output.toByteArray()
  }

  fun decryptBytes(bytes: ByteArray): ByteArray {
    val input = DataInputStream(bytes.inputStream())
    val ivSize = input.readInt()
    val iv = ByteArray(ivSize)
    input.readFully(iv)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val key = getOrCreateKey()
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
    val encrypted = input.readBytes()
    return cipher.doFinal(encrypted)
  }
}
