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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.ai.edge.gallery.GalleryTopAppBar
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.data.AppBarAction
import com.google.ai.edge.gallery.data.AppBarActionType
import com.google.ai.edge.gallery.data.chathistory.ChatSessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryScreen(
  navigateUp: () -> Unit,
  navigateToSession: (ChatSessionEntity) -> Unit,
  viewModel: ChatHistoryViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val taskFilters by viewModel.taskFilters.collectAsState(initial = listOf())
  val modelFilters by viewModel.modelFilters.collectAsState(initial = listOf())
  val pagingSessions = viewModel.sessionsPaging.collectAsLazyPagingItems()
  val snackbarHostState = remember { SnackbarHostState() }
  var showClearAllDialog by remember { mutableStateOf(false) }
  var selectedTaskFilter by remember { mutableStateOf<String?>(null) }
  var selectedModelFilter by remember { mutableStateOf<String?>(null) }

  val exportLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) {
      uri ->
      if (uri != null) {
        viewModel.exportHistory(uri)
      }
    }

  val importLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        viewModel.importHistory(uri)
      }
    }

  LaunchedEffect(uiState.lastMessage) {
    uiState.lastMessage?.let { message ->
      snackbarHostState.showSnackbar(message)
      viewModel.consumeMessage()
    }
  }

  Scaffold(
    topBar = {
      GalleryTopAppBar(
        title = stringResource(R.string.chat_history_title),
        leftAction =
          AppBarAction(
            actionType = AppBarActionType.NAVIGATE_UP,
            actionFn = navigateUp,
          ),
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { exportLauncher.launch("chat_history_export.zip") }) {
          Text(stringResource(R.string.chat_history_export))
        }
        Button(onClick = { importLauncher.launch(arrayOf("application/zip")) }) {
          Text(stringResource(R.string.chat_history_import))
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = { showClearAllDialog = true }) {
          Text(stringResource(R.string.chat_history_clear_all))
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterMenu(
          label = stringResource(R.string.chat_history_filter_task),
          options = taskFilters,
          selected = selectedTaskFilter,
          onSelected = { option ->
            selectedTaskFilter = option
            viewModel.setTaskFilter(option)
          },
        )
        FilterMenu(
          label = stringResource(R.string.chat_history_filter_model),
          options = modelFilters,
          selected = selectedModelFilter,
          onSelected = { option ->
            selectedModelFilter = option
            viewModel.setModelFilter(option)
          },
        )
      }

      if (uiState.isWorking) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
          Text(stringResource(R.string.chat_history_working))
        }
      }

      if (pagingSessions.itemCount == 0) {
        Text(stringResource(R.string.chat_history_empty), style = MaterialTheme.typography.bodyMedium)
      } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          items(pagingSessions.itemCount) { index ->
            val session = pagingSessions[index] ?: return@items
            ChatHistorySessionCard(
              session = session,
              onClick = { navigateToSession(session) },
              onDelete = { viewModel.deleteSession(session.id) },
            )
          }
        }
      }
    }
  }

  if (showClearAllDialog) {
    AlertDialog(
      onDismissRequest = { showClearAllDialog = false },
      title = { Text(stringResource(R.string.chat_history_clear_all_title)) },
      text = { Text(stringResource(R.string.chat_history_clear_all_body)) },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.deleteAllSessions()
            showClearAllDialog = false
          }
        ) {
          Text(stringResource(R.string.chat_history_clear_all_confirm))
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearAllDialog = false }) {
          Text(stringResource(R.string.chat_history_clear_all_cancel))
        }
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatHistorySessionCard(
  session: ChatSessionEntity,
  onClick: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    onClick = onClick,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(session.title, style = MaterialTheme.typography.titleMedium)
          Text(
            formatTimestamp(session.updatedAtMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = onDelete) {
          Icon(
            imageVector = Icons.Rounded.Delete,
            contentDescription = stringResource(R.string.cd_chat_history_delete),
          )
        }
      }
      if (session.lastMessagePreview.isNotBlank()) {
        Text(
          session.lastMessagePreview,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          session.modelName,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          session.taskId,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.secondary,
        )
      }
    }
  }
}

private fun formatTimestamp(timestampMs: Long): String {
  val formatter = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
  return formatter.format(Date(timestampMs))
}

@Composable
private fun FilterMenu(
  label: String,
  options: List<String>,
  selected: String?,
  onSelected: (String?) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  val display = selected ?: stringResource(R.string.chat_history_filter_all)
  Box {
    TextButton(onClick = { expanded = true }) { Text("$label: $display") }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      DropdownMenuItem(
        text = { Text(stringResource(R.string.chat_history_filter_all)) },
        onClick = {
          expanded = false
          onSelected(null)
        },
      )
      for (option in options) {
        DropdownMenuItem(
          text = { Text(option) },
          onClick = {
            expanded = false
            onSelected(option)
          },
        )
      }
    }
  }
}
