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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatHistoryDao {
  @Query(
    "SELECT * FROM chat_sessions ORDER BY isPinned DESC, updatedAtMs DESC, createdAtMs DESC",
  )
  fun getSessions(): Flow<List<ChatSessionEntity>>

  @Query(
    """
    SELECT * FROM chat_sessions
    WHERE (:taskId IS NULL OR taskId = :taskId)
      AND (:modelName IS NULL OR modelName = :modelName)
    ORDER BY isPinned DESC, updatedAtMs DESC, createdAtMs DESC
    """,
  )
  fun getSessionsPaged(taskId: String?, modelName: String?): PagingSource<Int, ChatSessionEntity>

  @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
  suspend fun getSessionById(sessionId: String): ChatSessionEntity?

  @Query("SELECT * FROM chat_sessions")
  suspend fun getSessionsOnce(): List<ChatSessionEntity>

  @Query("SELECT DISTINCT taskId FROM chat_sessions ORDER BY taskId ASC")
  fun getDistinctTaskIds(): Flow<List<String>>

  @Query("SELECT DISTINCT modelName FROM chat_sessions ORDER BY modelName ASC")
  fun getDistinctModelNames(): Flow<List<String>>

  @Query("SELECT COUNT(*) FROM chat_sessions")
  suspend fun getSessionCount(): Int

  @Query("SELECT * FROM chat_sessions ORDER BY updatedAtMs ASC LIMIT :limit")
  suspend fun getOldestSessions(limit: Int): List<ChatSessionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSession(session: ChatSessionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSessions(sessions: List<ChatSessionEntity>)

  @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
  suspend fun deleteSession(sessionId: String)

  @Query("DELETE FROM chat_sessions")
  suspend fun deleteAllSessions()

  @Transaction
  @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY indexInSession ASC")
  fun getMessagesWithAttachments(sessionId: String): Flow<List<ChatMessageWithAttachments>>

  @Transaction
  @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY indexInSession ASC")
  suspend fun getMessagesWithAttachmentsOnce(sessionId: String): List<ChatMessageWithAttachments>

  @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY indexInSession ASC")
  suspend fun getMessages(sessionId: String): List<ChatMessageEntity>

  @Query("SELECT * FROM chat_messages")
  suspend fun getAllMessages(): List<ChatMessageEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMessage(message: ChatMessageEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMessages(messages: List<ChatMessageEntity>)

  @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
  suspend fun deleteMessagesForSession(sessionId: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAttachments(attachments: List<ChatAttachmentEntity>)

  @Query("SELECT * FROM chat_attachments WHERE messageId IN (:messageIds)")
  suspend fun getAttachmentsForMessages(messageIds: List<String>): List<ChatAttachmentEntity>

  @Query("SELECT * FROM chat_attachments")
  suspend fun getAllAttachments(): List<ChatAttachmentEntity>

  @Query(
    "SELECT id FROM chat_messages WHERE sessionId = :sessionId ORDER BY indexInSession ASC LIMIT :limit",
  )
  suspend fun getOldestMessageIds(sessionId: String, limit: Int): List<String>

  @Query("DELETE FROM chat_messages WHERE id IN (:messageIds)")
  suspend fun deleteMessagesByIds(messageIds: List<String>)

  @Query("DELETE FROM chat_attachments WHERE messageId = :messageId")
  suspend fun deleteAttachmentsForMessage(messageId: String)

  @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId")
  suspend fun getMessageCount(sessionId: String): Int

  @Query(
    "SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY indexInSession DESC LIMIT 1",
  )
  suspend fun getLastMessage(sessionId: String): ChatMessageEntity?
}
