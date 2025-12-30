package com.screenshot.ocr

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

class SettingsActivity : ComponentActivity() {

    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(this)

        setContent {
            MaterialTheme {
                SettingsScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SettingsScreen() {
        val context = LocalContext.current
        val settings = settingsManager.getSettings()

        var apiKey by remember { mutableStateOf(settings.apiKey) }
        var modelName by remember { mutableStateOf(settings.modelName) }
        var overlapThreshold by remember { mutableStateOf(settings.overlapThreshold) }
        var chatDetection by remember { mutableStateOf(settings.enableChatDetection) }
        var showApiKey by remember { mutableStateOf(false) }
        var showModelDropdown by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // API Configuration Section
                Text(
                    "OpenRouter Configuration",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // API Key Input
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("OpenRouter API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showApiKey) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(onClick = { showApiKey = !showApiKey }) {
                            Text(if (showApiKey) "Hide" else "Show")
                        }
                    },
                    supportingText = {
                        Text("Get your key at openrouter.ai/keys")
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Model Selection
                Text(
                    "Vision Model",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Model name input with dropdown
                ExposedDropdownMenuBox(
                    expanded = showModelDropdown,
                    onExpandedChange = { showModelDropdown = it }
                ) {
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("Model name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                "Select model",
                                Modifier.clickable { showModelDropdown = true }
                            )
                        },
                        supportingText = {
                            Text("Enter any OpenRouter model ID or select from suggestions")
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = showModelDropdown,
                        onDismissRequest = { showModelDropdown = false }
                    ) {
                        SettingsManager.SUGGESTED_MODELS.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(name, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            id,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    modelName = id
                                    showModelDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Help Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Recommended free vision models:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "- qwen/qwen2.5-vl-72b-instruct:free (best OCR)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "- google/gemma-3-27b-it:free",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "- meta-llama/llama-3.2-11b-vision-instruct:free",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Browse all models: openrouter.ai/models",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Divider()

                Spacer(modifier = Modifier.height(24.dp))

                // Processing Options Section
                Text(
                    "Processing Options",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Chat Detection Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Chat Detection",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Automatically detect and format chat conversations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = chatDetection,
                        onCheckedChange = { chatDetection = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Overlap Threshold Slider
                Text(
                    "Overlap Detection Threshold: ${(overlapThreshold * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Higher values require more exact matches",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = overlapThreshold,
                    onValueChange = { overlapThreshold = it },
                    valueRange = 0.6f..0.95f,
                    steps = 6
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Save Button
                Button(
                    onClick = {
                        if (apiKey.isBlank()) {
                            Toast.makeText(context, "Please enter an API key", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (modelName.isBlank()) {
                            Toast.makeText(context, "Please enter a model name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        settingsManager.saveSettings(
                            com.screenshot.ocr.models.AppSettings(
                                apiKey = apiKey.trim(),
                                modelName = modelName.trim(),
                                overlapThreshold = overlapThreshold,
                                enableChatDetection = chatDetection
                            )
                        )
                        Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Save Settings")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Info
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Screenshot OCR v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "AI-powered screenshot stitching and OCR via OpenRouter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
