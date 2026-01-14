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

data class ChatHistoryExport(
  val schemaVersion: Int,
  val exportedAtMs: Long,
  val sessions: List<ChatSessionExport>,
  val messages: List<ChatMessageExport>,
  val attachments: List<ChatAttachmentExport>,
)

data class ChatSessionExport(
  val id: String,
  val taskId: String,
  val modelName: String,
  val title: String,
  val createdAtMs: Long,
  val updatedAtMs: Long,
  val lastMessagePreview: String,
  val messageCount: Int,
  val isPinned: Boolean,
)

data class ChatMessageExport(
  val id: String,
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

data class ChatAttachmentExport(
  val id: String,
  val messageId: String,
  val kind: String,
  val fileName: String,
  val mimeType: String,
  val width: Int,
  val height: Int,
  val sampleRate: Int,
  val durationMs: Long,
)
