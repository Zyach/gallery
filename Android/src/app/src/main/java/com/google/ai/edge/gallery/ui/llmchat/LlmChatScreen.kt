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

package com.google.ai.edge.gallery.ui.llmchat

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.bundleOf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.firebaseAnalytics
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageAudioClip
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageImage
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageText
import com.google.ai.edge.gallery.ui.common.chat.ChatView
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageWarning
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel

@Composable
fun LlmChatScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  sessionId: String? = null,
  viewModel: LlmChatViewModel = hiltViewModel(),
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    taskId = BuiltInTaskId.LLM_CHAT,
    navigateUp = navigateUp,
    modifier = modifier,
    sessionId = sessionId,
  )
}

@Composable
fun LlmAskImageScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  sessionId: String? = null,
  viewModel: LlmAskImageViewModel = hiltViewModel(),
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    taskId = BuiltInTaskId.LLM_ASK_IMAGE,
    navigateUp = navigateUp,
    modifier = modifier,
    sessionId = sessionId,
  )
}

@Composable
fun LlmAskAudioScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  sessionId: String? = null,
  viewModel: LlmAskAudioViewModel = hiltViewModel(),
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    taskId = BuiltInTaskId.LLM_ASK_AUDIO,
    navigateUp = navigateUp,
    modifier = modifier,
    sessionId = sessionId,
  )
}

@Composable
fun ChatViewWrapper(
  viewModel: LlmChatViewModelBase,
  modelManagerViewModel: ModelManagerViewModel,
  taskId: String,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  sessionId: String? = null,
) {
  val context = LocalContext.current
  val task = modelManagerViewModel.getTaskById(id = taskId)!!
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  var pendingMissingModelNotice by remember { mutableStateOf<String?>(null) }
  var missingModelNoticeShown by remember { mutableStateOf(false) }
  var showMissingModelDialog by remember { mutableStateOf(false) }

  LaunchedEffect(sessionId) {
    if (sessionId.isNullOrBlank()) {
      pendingMissingModelNotice = null
      missingModelNoticeShown = false
      showMissingModelDialog = false
      return@LaunchedEffect
    }
    val session = viewModel.getSessionInfo(sessionId)
    if (session != null) {
      val targetModel = modelManagerViewModel.getModelByName(session.modelName)
      if (targetModel != null) {
        pendingMissingModelNotice = null
        missingModelNoticeShown = false
        showMissingModelDialog = false
        if (targetModel.name != modelManagerUiState.selectedModel.name) {
          modelManagerViewModel.selectModel(targetModel)
        }
      } else {
        pendingMissingModelNotice =
          "Model '${session.modelName}' is not available. Showing history only."
        showMissingModelDialog = true
      }
    }
  }

  LaunchedEffect(
    task.id,
    modelManagerUiState.selectedModel.name,
    sessionId,
  ) {
    viewModel.initializeSession(
      model = modelManagerUiState.selectedModel,
      taskId = task.id,
      sessionId = sessionId,
      defaultTitle = task.label,
    )
    if (!missingModelNoticeShown && !pendingMissingModelNotice.isNullOrBlank()) {
      viewModel.addMessage(
        model = modelManagerUiState.selectedModel,
        message = ChatMessageWarning(content = pendingMissingModelNotice ?: ""),
      )
      missingModelNoticeShown = true
    }
  }

  ChatView(
    task = task,
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    onSendMessage = { model, messages ->
      for (message in messages) {
        viewModel.addMessage(model = model, message = message)
      }
      viewModel.persistMessages(messages)

      var text = ""
      val images: MutableList<Bitmap> = mutableListOf()
      val audioMessages: MutableList<ChatMessageAudioClip> = mutableListOf()
      var chatMessageText: ChatMessageText? = null
      for (message in messages) {
        if (message is ChatMessageText) {
          chatMessageText = message
          text = message.content
        } else if (message is ChatMessageImage) {
          images.addAll(message.bitmaps)
        } else if (message is ChatMessageAudioClip) {
          audioMessages.add(message)
        }
      }
      if ((text.isNotEmpty() && chatMessageText != null) || audioMessages.isNotEmpty()) {
        modelManagerViewModel.addTextInputHistory(text)
        viewModel.generateResponse(
          model = model,
          input = text,
          images = images,
          audioMessages = audioMessages,
          onError = { errorMessage ->
            viewModel.handleError(
              context = context,
              task = task,
              model = model,
              errorMessage = errorMessage,
              modelManagerViewModel = modelManagerViewModel,
            )
          },
        )

        firebaseAnalytics?.logEvent(
          "generate_action",
          bundleOf("capability_name" to task.id, "model_id" to model.name),
        )
      }
    },
    onRunAgainClicked = { model, message ->
      if (message is ChatMessageText) {
        viewModel.runAgain(
          model = model,
          message = message,
          onError = { errorMessage ->
            viewModel.handleError(
              context = context,
              task = task,
              model = model,
              errorMessage = errorMessage,
              modelManagerViewModel = modelManagerViewModel,
            )
          },
        )
      }
    },
    onBenchmarkClicked = { _, _, _, _ -> },
    onResetSessionClicked = { model -> viewModel.resetSession(task = task, model = model) },
    showStopButtonInInputWhenInProgress = true,
    onStopButtonClicked = { model -> viewModel.stopResponse(model = model) },
    navigateUp = navigateUp,
    modifier = modifier,
  )

  if (showMissingModelDialog && !pendingMissingModelNotice.isNullOrBlank()) {
    AlertDialog(
      onDismissRequest = { showMissingModelDialog = false },
      title = { Text("Model not available") },
      text = {
        Text(
          pendingMissingModelNotice ?: "",
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        TextButton(onClick = { showMissingModelDialog = false }) { Text("OK") }
      },
    )
  }
}
