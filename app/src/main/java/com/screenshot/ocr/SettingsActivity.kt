package com.screenshot.ocr

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.screenshot.ocr.models.AiProvider

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
        var selectedProvider by remember { mutableStateOf(settings.aiProvider) }
        var overlapThreshold by remember { mutableStateOf(settings.overlapThreshold) }
        var chatDetection by remember { mutableStateOf(settings.enableChatDetection) }
        var showApiKey by remember { mutableStateOf(false) }

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
                    "API Configuration",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // AI Provider Selection
                Text(
                    "AI Provider",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                AiProvider.values().forEach { provider ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(provider.name)
                        RadioButton(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // API Key Input
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
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
                        Text("Your API key for ${selectedProvider.name}")
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // API Key Help Text
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Get your API key:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        when (selectedProvider) {
                            AiProvider.OPENAI -> Text(
                                "OpenAI: platform.openai.com",
                                style = MaterialTheme.typography.bodySmall
                            )
                            AiProvider.GEMINI -> Text(
                                "Gemini: makersuite.google.com",
                                style = MaterialTheme.typography.bodySmall
                            )
                            AiProvider.MISTRAL -> Text(
                                "Mistral: console.mistral.ai",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
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
                        settingsManager.saveSettings(
                            com.screenshot.ocr.models.AppSettings(
                                apiKey = apiKey,
                                aiProvider = selectedProvider,
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
                    "AI-powered screenshot stitching and OCR",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
