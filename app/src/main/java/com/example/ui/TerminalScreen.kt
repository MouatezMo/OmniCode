package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TerminalAmber
import com.example.viewmodel.LogType
import com.example.viewmodel.WorkspaceViewModel

val TerminalError = Color(0xFFFF3333)
val TerminalLogUser = Color(0xFF00AAFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(viewModel: WorkspaceViewModel) {
    var selectedSubTab by remember { mutableIntStateOf(1) } // 0: Brain, 1: Logs
    val logs by viewModel.logs.collectAsState()
    val reasoning by viewModel.reasoningContent.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }, text = { Text("BRAIN") })
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, text = { Text("LOGS") })
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            if (selectedSubTab == 0) {
                // Brain
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            text = if (reasoning.isEmpty()) "Waiting for neural linkage..." else reasoning,
                            color = TerminalAmber,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // Logs
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(logs) { log ->
                        val color = when (log.type) {
                            LogType.ERROR -> TerminalError
                            LogType.BRAIN -> TerminalAmber
                            LogType.USER -> TerminalLogUser
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Text(
                            text = log.text,
                            color = color,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter prompt >_") },
                singleLine = true,
                colors = defaultTerminalTextFieldColors(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank() && !isGenerating) {
                        viewModel.submitPrompt(inputText)
                        inputText = ""
                    }
                })
            )
            Button(
                onClick = {
                    if (inputText.isNotBlank() && !isGenerating) {
                        viewModel.submitPrompt(inputText)
                        inputText = ""
                    }
                },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(if (isGenerating) "..." else "RUN", color = MaterialTheme.colorScheme.background)
            }
        }
    }
}
