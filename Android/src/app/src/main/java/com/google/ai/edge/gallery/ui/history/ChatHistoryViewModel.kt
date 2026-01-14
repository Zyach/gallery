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

package com.google.ai.edge.gallery.ui.history

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.ai.edge.gallery.data.chathistory.ChatHistoryRepository
import com.google.ai.edge.gallery.data.chathistory.ChatSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatHistoryViewModel @Inject constructor(private val repository: ChatHistoryRepository) :
  ViewModel() {
  private val _uiState = MutableStateFlow(ChatHistoryUiState())
  val uiState = _uiState.asStateFlow()
  private val taskFilter = MutableStateFlow<String?>(null)
  private val modelFilter = MutableStateFlow<String?>(null)

  val taskFilters: Flow<List<String>> = repository.getDistinctTaskIds()
  val modelFilters: Flow<List<String>> = repository.getDistinctModelNames()

  val sessionsPaging: Flow<PagingData<ChatSessionEntity>> =
    combine(taskFilter, modelFilter) { task, model -> Pair(task, model) }
      .flatMapLatest { (task, model) ->
        Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
            repository.getSessionsPaged(task, model)
          }
          .flow
      }
      .cachedIn(viewModelScope)

  init {
    viewModelScope.launch {
      repository.getSessions().collect { sessions ->
        _uiState.update { it.copy(sessions = sessions) }
      }
    }
  }

  fun deleteSession(sessionId: String) {
    viewModelScope.launch(Dispatchers.Default) { repository.deleteSession(sessionId) }
  }

  fun deleteAllSessions() {
    viewModelScope.launch(Dispatchers.Default) { repository.deleteAllSessions() }
  }

  fun exportHistory(uri: Uri) {
    viewModelScope.launch(Dispatchers.Default) {
      _uiState.update { it.copy(isWorking = true) }
      val result = repository.exportHistory(uri)
      _uiState.update {
        it.copy(
          isWorking = false,
          lastMessage =
            "Exported ${result.sessions} sessions, ${result.messages} messages, ${result.attachments} attachments",
        )
      }
    }
  }

  fun importHistory(uri: Uri) {
    viewModelScope.launch(Dispatchers.Default) {
      _uiState.update { it.copy(isWorking = true) }
      val result = repository.importHistory(uri)
      _uiState.update {
        it.copy(
          isWorking = false,
          lastMessage =
            "Imported ${result.sessions} sessions, ${result.messages} messages, ${result.attachments} attachments",
        )
      }
    }
  }

  fun consumeMessage() {
    _uiState.update { it.copy(lastMessage = null) }
  }

  fun setTaskFilter(taskId: String?) {
    taskFilter.value = taskId
  }

  fun setModelFilter(modelName: String?) {
    modelFilter.value = modelName
  }
}

data class ChatHistoryUiState(
  val sessions: List<ChatSessionEntity> = listOf(),
  val isWorking: Boolean = false,
  val lastMessage: String? = null,
)
