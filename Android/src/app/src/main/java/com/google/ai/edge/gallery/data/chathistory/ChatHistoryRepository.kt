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
import android.net.Uri
import android.util.Log
import com.google.ai.edge.gallery.ui.common.chat.ChatMessage
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageAudioClip
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageError
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageImage
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageImageWithHistory
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageInfo
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageLoading
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageText
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageWarning
import com.google.ai.edge.gallery.ui.common.chat.ChatSide
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageType
import com.google.gson.Gson
import com.google.ai.edge.gallery.data.LlmHttpPrefs
import java.io.File
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

private const val TAG = "AGChatHistoryRepo"
private const val MAX_SESSIONS = 50
private const val MAX_MESSAGES_PER_SESSION = 200
private const val MAX_TOTAL_ATTACHMENT_BYTES = 300L * 1024L * 1024L
private const val EXPORT_SCHEMA_VERSION = 1

class ChatHistoryRepository
@Inject
constructor(
  private val dao: ChatHistoryDao,
  private val attachmentStorage: ChatAttachmentStorage,
  private val context: Context,
  private val prefs: LlmHttpPrefs,
) {
  fun getSessions(): Flow<List<ChatSessionEntity>> = dao.getSessions()

  fun getSessionsPaged(taskId: String?, modelName: String?): PagingSource<Int, ChatSessionEntity> {
    return dao.getSessionsPaged(taskId, modelName)
  }

  fun getDistinctTaskIds(): Flow<List<String>> = dao.getDistinctTaskIds()

  fun getDistinctModelNames(): Flow<List<String>> = dao.getDistinctModelNames()

  fun getMessagesWithAttachments(sessionId: String): Flow<List<ChatMessageWithAttachments>> {
    return dao.getMessagesWithAttachments(sessionId)
  }

  suspend fun getMessagesWithAttachmentsOnce(sessionId: String): List<ChatMessageWithAttachments> {
    return dao.getMessagesWithAttachmentsOnce(sessionId)
  }

  suspend fun getSession(sessionId: String): ChatSessionEntity? {
    return dao.getSessionById(sessionId)
  }

  data class ExportResult(val sessions: Int, val messages: Int, val attachments: Int)

  data class ImportResult(val sessions: Int, val messages: Int, val attachments: Int)

  suspend fun createSession(taskId: String, modelName: String, title: String): ChatSessionEntity {
    if (!prefs.isHistoryEnabled(context)) {
      // Historial desactivado: devolver sesión efímera en memoria (no se persiste).
      return ChatSessionEntity(
        id = "ephemeral",
        taskId = taskId,
        modelName = modelName,
        title = title.ifBlank { "New chat" },
        createdAtMs = 0,
        updatedAtMs = 0,
        lastMessagePreview = "",
        messageCount = 0,
        isPinned = false,
      )
    }
    val now = System.currentTimeMillis()
    val session =
      ChatSessionEntity(
        id = UUID.randomUUID().toString(),
        taskId = taskId,
        modelName = modelName,
        title = title.ifBlank { "New chat" },
        createdAtMs = now,
        updatedAtMs = now,
        lastMessagePreview = "",
        messageCount = 0,
        isPinned = false,
      )
    dao.upsertSession(session)
    enforceSessionLimit()
    return session
  }

  suspend fun deleteSession(sessionId: String) {
    val messages = dao.getMessages(sessionId)
    deleteAttachmentFilesForMessages(messages.map { it.id })
    dao.deleteSession(sessionId)
  }

  suspend fun deleteAllSessions() {
    val allFiles = attachmentStorage.listAllFiles()
    for (file in allFiles) {
      file.delete()
    }
    dao.deleteAllSessions()
  }

  suspend fun appendMessages(sessionId: String, messages: List<ChatMessage>) {
    if (!prefs.isHistoryEnabled(context)) return
    if (messages.isEmpty()) {
      return
    }
    val baseIndex = dao.getMessageCount(sessionId)
    val now = System.currentTimeMillis()
    val messageEntities = mutableListOf<ChatMessageEntity>()
    val attachmentEntities = mutableListOf<ChatAttachmentEntity>()
    var lastPreview = ""

    var addedCount = 0
    messages.forEach { message ->
      if (message is ChatMessageLoading) {
        return@forEach
      }
      val messageId = UUID.randomUUID().toString()
      val content = getMessageContent(message)
      val type = message.type.name
      val side = message.side.name
      val entity =
        ChatMessageEntity(
          id = messageId,
          sessionId = sessionId,
          indexInSession = baseIndex + addedCount,
          timestampMs = now,
          side = side,
          type = type,
          content = content,
          latencyMs = message.latencyMs,
          accelerator = message.accelerator,
          metaJson = "",
          isComplete = true,
        )
      messageEntities.add(entity)
      addedCount += 1
      lastPreview = content.take(120)

      when (message) {
        is ChatMessageImage -> {
          val attachments = createImageAttachments(messageId, message.bitmaps)
          attachmentEntities.addAll(attachments)
        }
        is ChatMessageImageWithHistory -> {
          val attachments = createImageAttachments(messageId, message.bitmaps)
          attachmentEntities.addAll(attachments)
        }
        is ChatMessageAudioClip -> {
          val attachment = createAudioAttachment(messageId, message)
          attachmentEntities.add(attachment)
        }
        else -> {}
      }
    }

    dao.upsertMessages(messageEntities)
    if (attachmentEntities.isNotEmpty()) {
      dao.upsertAttachments(attachmentEntities)
    }
    maybeUpdateSessionTitle(sessionId, messages)
    updateSessionStats(sessionId, lastPreview)
    trimSessionMessages(sessionId)
    enforceAttachmentLimit()
  }

  private suspend fun updateSessionStats(sessionId: String, lastPreview: String) {
    val session = dao.getSessionById(sessionId) ?: return
    val messageCount = dao.getMessageCount(sessionId)
    val updatedSession =
      session.copy(
        updatedAtMs = System.currentTimeMillis(),
        lastMessagePreview = lastPreview,
        messageCount = messageCount,
      )
    dao.upsertSession(updatedSession)
  }

  private suspend fun maybeUpdateSessionTitle(sessionId: String, messages: List<ChatMessage>) {
    val session = dao.getSessionById(sessionId) ?: return
    if (session.title != "New chat") {
      return
    }
    val candidate = messages.firstOrNull { it.side == ChatSide.USER }
    val title =
      when (candidate) {
        is ChatMessageText -> candidate.content.trim().take(60)
        is ChatMessageInfo -> candidate.content.trim().take(60)
        is ChatMessageWarning -> candidate.content.trim().take(60)
        is ChatMessageError -> candidate.content.trim().take(60)
        else -> ""
      }
    if (title.isBlank()) {
      return
    }
    dao.upsertSession(session.copy(title = title))
  }

  private suspend fun trimSessionMessages(sessionId: String) {
    val count = dao.getMessageCount(sessionId)
    if (count <= MAX_MESSAGES_PER_SESSION) {
      return
    }
    val toRemove = count - MAX_MESSAGES_PER_SESSION
    val oldestIds = dao.getOldestMessageIds(sessionId, toRemove)
    if (oldestIds.isNotEmpty()) {
      deleteAttachmentFilesForMessages(oldestIds)
      dao.deleteMessagesByIds(oldestIds)
    }
  }

  private suspend fun enforceSessionLimit() {
    val count = dao.getSessionCount()
    if (count <= MAX_SESSIONS) {
      return
    }
    val toRemove = count - MAX_SESSIONS
    val oldest = dao.getOldestSessions(toRemove)
    for (session in oldest) {
      deleteSession(session.id)
    }
  }

  private suspend fun enforceAttachmentLimit() {
    val files = attachmentStorage.listAllFiles()
    val totalSize = files.sumOf { it.length() }
    if (totalSize <= MAX_TOTAL_ATTACHMENT_BYTES) {
      return
    }
    Log.d(TAG, "Attachment limit exceeded. Cleaning up oldest sessions.")
    val sessions = dao.getOldestSessions(5)
    for (session in sessions) {
      deleteSession(session.id)
      val remaining = attachmentStorage.listAllFiles().sumOf { it.length() }
      if (remaining <= MAX_TOTAL_ATTACHMENT_BYTES) {
        break
      }
    }
  }

  private suspend fun deleteAttachmentFilesForMessages(messageIds: List<String>) {
    if (messageIds.isEmpty()) {
      return
    }
    val attachments = dao.getAttachmentsForMessages(messageIds)
    for (attachment in attachments) {
      attachmentStorage.deleteFile(attachment.uri)
    }
  }

  private fun getMessageContent(message: ChatMessage): String {
    return when (message) {
      is ChatMessageText -> message.content
      is ChatMessageInfo -> message.content
      is ChatMessageWarning -> message.content
      is ChatMessageError -> message.content
      is ChatMessageImage -> "[Image]"
      is ChatMessageImageWithHistory -> "[Image]"
      is ChatMessageAudioClip -> "[Audio]"
      else -> ""
    }
  }

  private fun createImageAttachments(messageId: String, bitmaps: List<Bitmap>): List<ChatAttachmentEntity> {
    val attachments = mutableListOf<ChatAttachmentEntity>()
    for (bitmap in bitmaps) {
      val file = attachmentStorage.savePng(bitmap)
      attachments.add(
        ChatAttachmentEntity(
          id = UUID.randomUUID().toString(),
          messageId = messageId,
          kind = ChatMessageType.IMAGE.name,
          uri = file.absolutePath,
          mimeType = "image/png",
          width = bitmap.width,
          height = bitmap.height,
          sampleRate = 0,
          durationMs = 0L,
        )
      )
    }
    return attachments
  }

  private fun createAudioAttachment(messageId: String, message: ChatMessageAudioClip): ChatAttachmentEntity {
    val file = attachmentStorage.saveAudioBytes(message.audioData)
    val durationMs = (message.getDurationInSeconds() * 1000f).toLong()
    return ChatAttachmentEntity(
      id = UUID.randomUUID().toString(),
      messageId = messageId,
      kind = ChatMessageType.AUDIO_CLIP.name,
      uri = file.absolutePath,
      mimeType = "audio/pcm",
      width = 0,
      height = 0,
      sampleRate = message.sampleRate,
      durationMs = durationMs,
    )
  }

  fun readAttachmentBytes(path: String): ByteArray {
    return attachmentStorage.readBytes(File(path))
  }

  fun getAttachmentDir(): File {
    return attachmentStorage.getAttachmentDir()
  }

  fun getContext(): Context {
    return context
  }

  suspend fun exportHistory(outputUri: Uri): ExportResult {
    val sessions = dao.getSessionsOnce()
    val messages = dao.getAllMessages()
    val attachments = dao.getAllAttachments()
    val export =
      ChatHistoryExport(
        schemaVersion = EXPORT_SCHEMA_VERSION,
        exportedAtMs = System.currentTimeMillis(),
        sessions =
          sessions.map {
            ChatSessionExport(
              id = it.id,
              taskId = it.taskId,
              modelName = it.modelName,
              title = it.title,
              createdAtMs = it.createdAtMs,
              updatedAtMs = it.updatedAtMs,
              lastMessagePreview = it.lastMessagePreview,
              messageCount = it.messageCount,
              isPinned = it.isPinned,
            )
          },
        messages =
          messages.map {
            ChatMessageExport(
              id = it.id,
              sessionId = it.sessionId,
              indexInSession = it.indexInSession,
              timestampMs = it.timestampMs,
              side = it.side,
              type = it.type,
              content = it.content,
              latencyMs = it.latencyMs,
              accelerator = it.accelerator,
              metaJson = it.metaJson,
              isComplete = it.isComplete,
            )
          },
        attachments =
          attachments.map {
            val fileName = File(it.uri).name
            ChatAttachmentExport(
              id = it.id,
              messageId = it.messageId,
              kind = it.kind,
              fileName = fileName,
              mimeType = it.mimeType,
              width = it.width,
              height = it.height,
              sampleRate = it.sampleRate,
              durationMs = it.durationMs,
            )
          },
      )
    val resolver = context.contentResolver
    resolver.openOutputStream(outputUri)?.use { output ->
      ZipOutputStream(BufferedOutputStream(output)).use { zip ->
        zip.putNextEntry(ZipEntry("manifest.json"))
        zip.write(Gson().toJson(export).toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        for (attachment in attachments) {
          val file = File(attachment.uri)
          if (!file.exists()) {
            continue
          }
          zip.putNextEntry(ZipEntry("attachments/${file.name}"))
          val bytes = attachmentStorage.readBytes(file)
          zip.write(bytes)
          zip.closeEntry()
        }
      }
    }
    return ExportResult(sessions.size, messages.size, attachments.size)
  }

  suspend fun importHistory(inputUri: Uri): ImportResult {
    val resolver = context.contentResolver
    val manifestJson = StringBuilder()
    val attachmentBytes = mutableMapOf<String, ByteArray>()

    resolver.openInputStream(inputUri)?.use { input ->
      ZipInputStream(BufferedInputStream(input)).use { zip ->
        while (true) {
          val entry = zip.nextEntry ?: break
          when {
            entry.name == "manifest.json" -> {
              val bytes = readZipEntry(zip)
              manifestJson.append(String(bytes, Charsets.UTF_8))
            }
            entry.name.startsWith("attachments/") -> {
              val name = entry.name.removePrefix("attachments/")
              attachmentBytes[name] = readZipEntry(zip)
            }
          }
          zip.closeEntry()
        }
      }
    }

    if (manifestJson.isBlank()) {
      return ImportResult(0, 0, 0)
    }

    val export = Gson().fromJson(manifestJson.toString(), ChatHistoryExport::class.java)
    val existingSessionIds = dao.getSessionsOnce().map { it.id }.toSet()
    val sessionIdMap = mutableMapOf<String, String>()
    val messageIdMap = mutableMapOf<String, String>()
    val newSessions = mutableListOf<ChatSessionEntity>()
    val newMessages = mutableListOf<ChatMessageEntity>()
    val newAttachments = mutableListOf<ChatAttachmentEntity>()

    for (session in export.sessions) {
      val newId = if (existingSessionIds.contains(session.id)) UUID.randomUUID().toString() else session.id
      sessionIdMap[session.id] = newId
      newSessions.add(
        ChatSessionEntity(
          id = newId,
          taskId = session.taskId,
          modelName = session.modelName,
          title = session.title,
          createdAtMs = session.createdAtMs,
          updatedAtMs = session.updatedAtMs,
          lastMessagePreview = session.lastMessagePreview,
          messageCount = session.messageCount,
          isPinned = session.isPinned,
        )
      )
    }

    for (message in export.messages) {
      val newId = UUID.randomUUID().toString()
      messageIdMap[message.id] = newId
      newMessages.add(
        ChatMessageEntity(
          id = newId,
          sessionId = sessionIdMap[message.sessionId] ?: message.sessionId,
          indexInSession = message.indexInSession,
          timestampMs = message.timestampMs,
          side = message.side,
          type = message.type,
          content = message.content,
          latencyMs = message.latencyMs,
          accelerator = message.accelerator,
          metaJson = message.metaJson,
          isComplete = message.isComplete,
        )
      )
    }

    for (attachment in export.attachments) {
      val bytes = attachmentBytes[attachment.fileName] ?: continue
      val destName = uniqueAttachmentName(attachment.fileName)
      val savedFile = attachmentStorage.saveBytes(destName, bytes)
      val newMessageId = messageIdMap[attachment.messageId] ?: attachment.messageId
      newAttachments.add(
        ChatAttachmentEntity(
          id = UUID.randomUUID().toString(),
          messageId = newMessageId,
          kind = attachment.kind,
          uri = savedFile.absolutePath,
          mimeType = attachment.mimeType,
          width = attachment.width,
          height = attachment.height,
          sampleRate = attachment.sampleRate,
          durationMs = attachment.durationMs,
        )
      )
    }

    dao.upsertSessions(newSessions)
    dao.upsertMessages(newMessages)
    if (newAttachments.isNotEmpty()) {
      dao.upsertAttachments(newAttachments)
    }
    enforceSessionLimit()
    enforceAttachmentLimit()
    return ImportResult(newSessions.size, newMessages.size, newAttachments.size)
  }

  private fun uniqueAttachmentName(originalName: String): String {
    val base = originalName.substringBeforeLast('.')
    val ext = originalName.substringAfterLast('.', "")
    val suffix = if (ext.isEmpty()) "" else ".$ext"
    return "${base}_${UUID.randomUUID()}$suffix"
  }

  private fun readZipEntry(zip: ZipInputStream): ByteArray {
    val buffer = ByteArray(8 * 1024)
    val output = java.io.ByteArrayOutputStream()
    while (true) {
      val read = zip.read(buffer)
      if (read <= 0) {
        break
      }
      output.write(buffer, 0, read)
    }
    return output.toByteArray()
  }
}
