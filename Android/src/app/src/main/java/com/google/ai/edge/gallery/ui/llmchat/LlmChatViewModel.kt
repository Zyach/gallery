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

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.ConfigKeys
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.data.allowThinking
import com.google.ai.edge.gallery.runtime.runtimeHelper
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageAudioClip
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageBenchmarkLlmResult
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageError
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageLoading
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageText
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageThinking
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageType
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageWarning
import com.google.ai.edge.gallery.ui.common.chat.ChatSide
import com.google.ai.edge.gallery.ui.common.chat.ChatViewModel
import com.google.ai.edge.gallery.ui.common.chat.Stat
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.litertlm.ExperimentalApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "AGLlmChatViewModel"
private val STATS =
  listOf(
    Stat(id = "time_to_first_token", label = "1st token", unit = "sec"),
    Stat(id = "prefill_speed", label = "Prefill speed", unit = "tokens/s"),
    Stat(id = "decode_speed", label = "Decode speed", unit = "tokens/s"),
    Stat(id = "latency", label = "Latency", unit = "sec"),
  )

open class LlmChatViewModelBase : ChatViewModel() {
  fun generateResponse(
    model: Model,
    input: String,
    images: List<android.graphics.Bitmap> = listOf(),
    audioMessages: List<ChatMessageAudioClip> = listOf(),
    onFirstToken: (Model) -> Unit = {},
    onDone: () -> Unit = {},
    onError: (String) -> Unit,
    allowThinking: Boolean = false,
  ) {
    val accelerator = model.getStringConfigValue(key = ConfigKeys.ACCELERATOR, defaultValue = "")
    viewModelScope.launch(Dispatchers.Default) {
      setInProgress(true)
      setPreparing(true)

      addMessage(model = model, message = ChatMessageLoading(accelerator = accelerator))

      while (model.instance == null) {
        delay(100)
      }
      delay(500)

      val instance = model.instance as LlmModelInstance
      var prefillTokens = images.size * 257
      val audioClips: MutableList<ByteArray> = mutableListOf()
      for (audioMessage in audioMessages) {
        audioClips.add(audioMessage.genByteArrayForWav())
        val duration = audioMessage.getDurationInSeconds()
        prefillTokens += (duration * 1000f / 150f).toInt()
      }

      var firstRun = true
      var timeToFirstToken = 0f
      var firstTokenTs = 0L
      var decodeTokens = 0
      var prefillSpeed = 0f
      val start = System.currentTimeMillis()

      try {
        val enableThinking =
          allowThinking &&
            model.getBooleanConfigValue(key = ConfigKeys.ENABLE_THINKING, defaultValue = false)
        val extraContext = if (enableThinking) mapOf("enable_thinking" to "true") else null

        model.runtimeHelper.runInference(
          model = model,
          input = input,
          images = images,
          audioClips = audioClips,
          resultListener = { partialResult, done, partialThinkingResult ->
            val curTs = System.currentTimeMillis()
            val currentLastMessage = getLastMessage(model = model)
            val wasLoading = currentLastMessage?.type == ChatMessageType.LOADING
            if (wasLoading) {
              removeLastMessage(model = model)
            }

            if (!partialThinkingResult.isNullOrEmpty() && allowThinking) {
              val lastMessageAfterLoading = getLastMessage(model = model)
              if (lastMessageAfterLoading?.type != ChatMessageType.THINKING) {
                addMessage(
                  model = model,
                  message =
                    ChatMessageThinking(
                      content = "",
                      inProgress = true,
                      side = ChatSide.AGENT,
                      accelerator = accelerator,
                    ),
                )
              }
              updateLastThinkingMessageContentIncrementally(
                model = model,
                partialContent = partialThinkingResult,
              )
            } else {
              val lastMessage = getLastMessage(model = model)
              if (lastMessage is ChatMessageThinking && lastMessage.inProgress) {
                replaceLastMessage(
                  model = model,
                  message =
                    ChatMessageThinking(
                      content = lastMessage.content,
                      inProgress = false,
                      side = lastMessage.side,
                      accelerator = lastMessage.accelerator,
                    ),
                  type = ChatMessageType.THINKING,
                )
              }
              val lastMessageAfterThinking = getLastMessage(model = model)
              if (
                lastMessageAfterThinking?.type != ChatMessageType.TEXT ||
                  lastMessageAfterThinking.side != ChatSide.AGENT
              ) {
                addMessage(
                  model = model,
                  message =
                    ChatMessageText(content = "", side = ChatSide.AGENT, accelerator = accelerator),
                )
              }
            }

            if (firstRun) {
              firstTokenTs = curTs
              timeToFirstToken = (firstTokenTs - start) / 1000f
              @OptIn(ExperimentalApi::class)
              prefillTokens += instance.conversation.getBenchmarkInfo().lastPrefillTokenCount
              prefillSpeed = if (timeToFirstToken > 0f) prefillTokens / timeToFirstToken else 0f
              firstRun = false
              setPreparing(false)
              onFirstToken(model)
            } else {
              decodeTokens++
            }

            val latencyMs: Long = if (done) curTs - start else -1
            if (partialResult.isNotEmpty() || done) {
              updateLastTextMessageContentIncrementally(
                model = model,
                partialContent = partialResult,
                latencyMs = latencyMs.toFloat(),
              )
            }

            if (done) {
              setInProgress(false)
              val finalLastMessage = getLastMessage(model = model)
              if (finalLastMessage is ChatMessageThinking && finalLastMessage.inProgress) {
                replaceLastMessage(
                  model = model,
                  message =
                    ChatMessageThinking(
                      content = finalLastMessage.content,
                      inProgress = false,
                      side = finalLastMessage.side,
                      accelerator = finalLastMessage.accelerator,
                    ),
                  type = ChatMessageType.THINKING,
                )
              }

              var decodeSpeed = 0f
              if (curTs > firstTokenTs) {
                decodeSpeed = decodeTokens / ((curTs - firstTokenTs) / 1000f)
                if (decodeSpeed.isNaN()) decodeSpeed = 0f
              }

              val finalRenderableMessage = getLastMessage(model = model)
              if (finalRenderableMessage is ChatMessageText) {
                updateLastTextMessageLlmBenchmarkResult(
                  model = model,
                  llmBenchmarkResult =
                    ChatMessageBenchmarkLlmResult(
                      orderedStats = STATS,
                      statValues =
                        mutableMapOf(
                          "prefill_speed" to prefillSpeed,
                          "decode_speed" to decodeSpeed,
                          "time_to_first_token" to timeToFirstToken,
                          "latency" to (curTs - start).toFloat() / 1000f,
                        ),
                      running = false,
                      latencyMs = -1f,
                      accelerator = accelerator,
                    ),
                )
              }
              onDone()
            }
          },
          cleanUpListener = {
            setInProgress(false)
            setPreparing(false)
          },
          onError = { message ->
            Log.e(TAG, "Error occurred while running inference")
            setInProgress(false)
            setPreparing(false)
            onError(message)
          },
          coroutineScope = viewModelScope,
          extraContext = extraContext,
        )
      } catch (e: Exception) {
        Log.e(TAG, "Error occurred while running inference", e)
        setInProgress(false)
        setPreparing(false)
        onError(e.message ?: "")
      }
    }
  }

  fun stopResponse(model: Model) {
    Log.d(TAG, "Stopping response for model ${model.name}...")
    if (getLastMessage(model = model) is ChatMessageLoading) {
      removeLastMessage(model = model)
    }
    setInProgress(false)
    model.runtimeHelper.stopResponse(model)
    Log.d(TAG, "Done stopping response")
  }

  fun resetSession(task: Task, model: Model) {
    viewModelScope.launch(Dispatchers.Default) {
      setIsResettingSession(true)
      clearAllMessages(model = model)
      stopResponse(model = model)

      val maxRetries = 5
      var retries = 0
      while (retries < maxRetries) {
        try {
          val supportImage =
            model.llmSupportImage &&
              task.id == com.google.ai.edge.gallery.data.BuiltInTaskId.LLM_ASK_IMAGE
          val supportAudio =
            model.llmSupportAudio &&
              task.id == com.google.ai.edge.gallery.data.BuiltInTaskId.LLM_ASK_AUDIO
          model.runtimeHelper.resetConversation(
            model = model,
            supportImage = supportImage,
            supportAudio = supportAudio,
            systemInstruction = null,
          )
          break
        } catch (e: Exception) {
          retries++
          Log.e(TAG, "Failed to reset session (attempt $retries/$maxRetries)", e)
          if (retries >= maxRetries) {
            Log.e(TAG, "Giving up session reset after $maxRetries attempts")
            break
          }
        }
        delay(200)
      }
      setIsResettingSession(false)
    }
  }

  fun runAgain(model: Model, message: ChatMessageText, onError: (String) -> Unit) {
    viewModelScope.launch(Dispatchers.Default) {
      val ready =
        withTimeoutOrNull(30_000) {
          while (model.instance == null) {
            delay(100)
          }
          true
        }
      if (ready == null) {
        onError("Model not ready. Please try again.")
        return@launch
      }

      addMessage(model = model, message = message.clone())
      generateResponse(model = model, input = message.content, onError = onError)
    }
  }

  fun runAgain(
    task: Task,
    model: Model,
    message: ChatMessageText,
    onError: (String) -> Unit,
  ) {
    viewModelScope.launch(Dispatchers.Default) {
      val ready =
        withTimeoutOrNull(30_000) {
          while (model.instance == null) {
            delay(100)
          }
          true
        }
      if (ready == null) {
        onError("Model not ready. Please try again.")
        return@launch
      }

      addMessage(model = model, message = message.clone())
      generateResponse(
        model = model,
        input = message.content,
        onError = onError,
        allowThinking = task.allowThinking() && model.llmSupportThinking,
      )
    }
  }

  fun handleError(
    context: Context,
    task: Task,
    model: Model,
    modelManagerViewModel: ModelManagerViewModel,
    errorMessage: String,
  ) {
    if (getLastMessage(model = model) is ChatMessageLoading) {
      removeLastMessage(model = model)
    }

    addMessage(model = model, message = ChatMessageError(content = errorMessage))

    viewModelScope.launch(Dispatchers.Default) {
      modelManagerViewModel.cleanupModel(
        context = context,
        task = task,
        model = model,
        onDone = {
          modelManagerViewModel.initializeModel(context = context, task = task, model = model)
          addMessage(
            model = model,
            message = ChatMessageWarning(content = "Session re-initialized"),
          )
        },
      )
    }
  }
}

@HiltViewModel
class LlmChatViewModel @Inject constructor() : LlmChatViewModelBase()

@HiltViewModel
class LlmAskImageViewModel @Inject constructor() : LlmChatViewModelBase()

@HiltViewModel
class LlmAskAudioViewModel @Inject constructor() : LlmChatViewModelBase()
