package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TerminalNeonGreen
import com.example.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: WorkspaceViewModel, onBack: () -> Unit) {
    var apiKey by remember { mutableStateOf(viewModel.settingsRepository.getApiKey()) }
    var modelName by remember { mutableStateOf(viewModel.settingsRepository.getModelName()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SETTINGS", color = TerminalNeonGreen) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("< BACK", color = TerminalNeonGreen)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Nvidia API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = defaultTerminalTextFieldColors()
            )

            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text("Model Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = defaultTerminalTextFieldColors()
            )

            Button(
                onClick = {
                    viewModel.settingsRepository.saveApiKey(apiKey)
                    viewModel.settingsRepository.saveModelName(modelName)
                    onBack()
                },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerminalNeonGreen,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text("SAVE Config")
            }
        }
    }
}

@Composable
fun defaultTerminalTextFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TerminalNeonGreen,
        unfocusedBorderColor = TerminalNeonGreen.copy(alpha = 0.5f),
        focusedLabelColor = TerminalNeonGreen,
        unfocusedLabelColor = TerminalNeonGreen.copy(alpha = 0.5f),
        cursorColor = TerminalNeonGreen,
    )
}
