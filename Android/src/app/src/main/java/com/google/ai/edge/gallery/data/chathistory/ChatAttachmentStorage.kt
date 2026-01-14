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
import android.graphics.Bitmap
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class ChatAttachmentStorage(private val context: Context, private val crypto: ChatCryptoManager) {
  private val attachmentDir = File(context.filesDir, "chat_attachments")

  init {
    if (!attachmentDir.exists()) {
      attachmentDir.mkdirs()
    }
  }

  fun savePng(bitmap: Bitmap): File {
    val file = File(attachmentDir, "img_${UUID.randomUUID()}.png")
    FileOutputStream(file).use { output ->
      crypto.encryptToStream(output).use { encrypted ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, encrypted)
      }
    }
    return file
  }

  fun saveAudioBytes(bytes: ByteArray): File {
    val file = File(attachmentDir, "aud_${UUID.randomUUID()}.pcm")
    FileOutputStream(file).use { output ->
      crypto.encryptToStream(output).use { encrypted ->
        encrypted.write(bytes)
      }
    }
    return file
  }

  fun readBytes(file: File): ByteArray {
    FileInputStream(file).use { input ->
      crypto.decryptFromStream(input).use { decrypted -> return decrypted.readBytes() }
    }
  }

  fun saveBytes(destName: String, bytes: ByteArray): File {
    val file = File(attachmentDir, destName)
    FileOutputStream(file).use { output ->
      crypto.encryptToStream(output).use { encrypted -> encrypted.write(bytes) }
    }
    return file
  }

  fun deleteFile(path: String) {
    val file = File(path)
    if (file.exists()) {
      file.delete()
    }
  }

  fun listAllFiles(): List<File> {
    if (!attachmentDir.exists()) {
      return listOf()
    }
    return attachmentDir.listFiles()?.toList() ?: listOf()
  }

  fun getAttachmentDir(): File {
    return attachmentDir
  }

  fun copyTo(file: File, dest: File) {
    FileInputStream(file).use { input ->
      crypto.decryptFromStream(input).use { decrypted ->
        FileOutputStream(dest).use { output -> decrypted.copyTo(output) }
      }
    }
  }

  fun importFrom(src: File, destName: String): File {
    val dest = File(attachmentDir, destName)
    FileInputStream(src).use { input ->
      FileOutputStream(dest).use { output ->
        crypto.encryptToStream(output).use { encrypted -> input.copyTo(encrypted) }
      }
    }
    return dest
  }
}
