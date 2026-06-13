package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plants by viewModel.plants.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    
    var showAddDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    
    var showSettingsDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(viewModel.apiKey) }
    
    val imagePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val base64 = uri.toBase64(context)
            if (base64 != null) {
                viewModel.analyzePlantImage(base64, inputName.ifBlank { "Unknown Plant" }) {
                    showAddDialog = false
                    inputName = ""
                }
            } else {
                android.widget.Toast.makeText(context, "Failed to process image. Try a different photo.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PlantDoc History") },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { viewModel.exportCsvData(context) }) {
                        Icon(Icons.Default.Download, contentDescription = "Export CSV")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Scan New Plant")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state is MainState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Analyzing leaf logic using Gemini... Please wait.", modifier = Modifier.padding(16.dp))
            }
            if (state is MainState.Error) {
                Text(
                    text = "Error: ${(state as MainState.Error).message}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (plants.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No records yet. Tap + to scan a leaf.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    items(plants, key = { it.id }) { plant ->
                        var expanded by remember { mutableStateOf(false) }
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(plant.name, style = MaterialTheme.typography.titleMedium)
                                    IconButton(onClick = { viewModel.deletePlant(plant.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Diagnosis:", style = MaterialTheme.typography.labelMedium)
                                Text(plant.diagnosis, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { expanded = !expanded }) {
                                        Text(if (expanded) "Hide details" else "View detailed report", style = MaterialTheme.typography.bodySmall)
                                        Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Expand details")
                                    }
                                    IconButton(onClick = { 
                                        viewModel.generateSpeechTask(plant.recommendation) { audio ->
                                            playBase64Audio(context, audio)
                                        }
                                    }) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Play Diagnosis (TTS)")
                                    }
                                }
                                if (expanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(plant.recommendation, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Scan New Plant") },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Plant Name") }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select a clear photo of the leaf for highest accuracy diagnosis.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    imagePicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Text("Pick Image")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Settings") },
            text = {
                Column {
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Gemini API Key") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Enter your own API key to bypass rate limits. It will be saved securely on your device.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateApiKey(apiKeyInput)
                    showSettingsDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("Cancel") }
            }
        )
    }
}
