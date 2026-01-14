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

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
  tableName = "chat_sessions",
)
data class ChatSessionEntity(
  @PrimaryKey val id: String,
  val taskId: String,
  val modelName: String,
  val title: String,
  val createdAtMs: Long,
  val updatedAtMs: Long,
  val lastMessagePreview: String,
  val messageCount: Int,
  val isPinned: Boolean = false,
)

@Entity(
  tableName = "chat_messages",
  foreignKeys =
    [
      ForeignKey(
        entity = ChatSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
      ),
    ],
  indices = [Index("sessionId")],
)
data class ChatMessageEntity(
  @PrimaryKey val id: String,
  val sessionId: String,
  val indexInSession: Int,
  val timestampMs: Long,
  val side: String,
  val type: String,
  val content: String,
  val latencyMs: Float,
  val accelerator: String,
  val metaJson: String,
  val isComplete: Boolean,
)

@Entity(
  tableName = "chat_attachments",
  foreignKeys =
    [
      ForeignKey(
        entity = ChatMessageEntity::class,
        parentColumns = ["id"],
        childColumns = ["messageId"],
        onDelete = ForeignKey.CASCADE,
      ),
    ],
  indices = [Index("messageId")],
)
data class ChatAttachmentEntity(
  @PrimaryKey val id: String,
  val messageId: String,
  val kind: String,
  val uri: String,
  val mimeType: String,
  val width: Int,
  val height: Int,
  val sampleRate: Int,
  val durationMs: Long,
)

data class ChatMessageWithAttachments(
  @Embedded val message: ChatMessageEntity,
  @Relation(parentColumn = "id", entityColumn = "messageId")
  val attachments: List<ChatAttachmentEntity>,
)
