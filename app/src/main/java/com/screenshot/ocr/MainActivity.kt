package com.screenshot.ocr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.screenshot.ocr.models.AiProvider
import com.screenshot.ocr.models.ProcessingState
import com.screenshot.ocr.models.Screenshot
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var imageStitcher: ImageStitcher
    private lateinit var ocrService: OcrService
    private lateinit var chatDetector: ChatDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize services
        settingsManager = SettingsManager(this)
        chatDetector = ChatDetector()
        imageStitcher = ImageStitcher(settingsManager.getSettings().overlapThreshold)
        ocrService = OcrService(chatDetector)

        // Request permissions
        requestPermissions()

        setContent {
            MaterialTheme {
                ScreenshotOCRApp()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ScreenshotOCRApp() {
        val context = LocalContext.current
        var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
        var processingState by remember { mutableStateOf<ProcessingState>(ProcessingState.Idle) }
        var stitchedBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var extractedText by remember { mutableStateOf("") }
        var showResults by remember { mutableStateOf(false) }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents()
        ) { uris ->
            selectedUris = uris
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Screenshot OCR") },
                    actions = {
                        IconButton(onClick = {
                            startActivity(Intent(context, SettingsActivity::class.java))
                        }) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    showResults -> {
                        ResultsScreen(
                            stitchedBitmap = stitchedBitmap,
                            extractedText = extractedText,
                            onBack = {
                                showResults = false
                                selectedUris = emptyList()
                                processingState = ProcessingState.Idle
                            },
                            onShare = { text ->
                                shareText(text)
                            },
                            onSave = { bitmap ->
                                saveBitmap(bitmap)
                            }
                        )
                    }

                    processingState is ProcessingState.Error -> {
                        ErrorScreen(
                            error = (processingState as ProcessingState.Error).message,
                            onRetry = {
                                processingState = ProcessingState.Idle
                            }
                        )
                    }

                    processingState !is ProcessingState.Idle -> {
                        ProcessingScreen(state = processingState)
                    }

                    else -> {
                        MainScreen(
                            selectedUris = selectedUris,
                            onSelectImages = {
                                imagePickerLauncher.launch("image/*")
                            },
                            onProcess = {
                                lifecycleScope.launch {
                                    processImages(
                                        selectedUris,
                                        onStateChange = { processingState = it },
                                        onSuccess = { bitmap, text ->
                                            stitchedBitmap = bitmap
                                            extractedText = text
                                            showResults = true
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun MainScreen(
        selectedUris: List<Uri>,
        onSelectImages: () -> Unit,
        onProcess: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (selectedUris.isEmpty()) {
                    "No screenshots selected"
                } else {
                    "${selectedUris.size} screenshots selected"
                },
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Select overlapping screenshots to stitch and extract text",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onSelectImages,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Screenshots")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onProcess,
                enabled = selectedUris.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Process Images")
            }
        }
    }

    @Composable
    fun ProcessingScreen(state: ProcessingState) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = when (state) {
                    is ProcessingState.Stitching ->
                        "Stitching image ${state.progress} of ${state.total}..."
                    is ProcessingState.ExtractingText ->
                        "Extracting text with AI..."
                    else -> "Processing..."
                },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    @Composable
    fun ResultsScreen(
        stitchedBitmap: Bitmap?,
        extractedText: String,
        onBack: () -> Unit,
        onShare: (String) -> Unit,
        onSave: (Bitmap) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back")
                }

                Row {
                    IconButton(onClick = { onShare(extractedText) }) {
                        Icon(Icons.Default.Share, "Share")
                    }
                    if (stitchedBitmap != null) {
                        IconButton(onClick = { onSave(stitchedBitmap) }) {
                            Icon(Icons.Default.Save, "Save")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stitched image preview
            if (stitchedBitmap != null) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Stitched Image",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = stitchedBitmap.asImageBitmap(),
                            contentDescription = "Stitched result",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Extracted text
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Extracted Text",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = extractedText.ifEmpty { "No text extracted" },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ErrorScreen(error: String, onRetry: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Error",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }

    private suspend fun processImages(
        uris: List<Uri>,
        onStateChange: (ProcessingState) -> Unit,
        onSuccess: (Bitmap, String) -> Unit
    ) {
        try {
            val settings = settingsManager.getSettings()

            if (settings.apiKey.isEmpty()) {
                onStateChange(ProcessingState.Error("Please set API key in settings"))
                return
            }

            // Load bitmaps
            onStateChange(ProcessingState.Stitching(0, uris.size))
            val bitmaps = uris.mapIndexed { index, uri ->
                onStateChange(ProcessingState.Stitching(index + 1, uris.size))
                contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: throw Exception("Failed to load image ${index + 1}")
            }

            // Stitch images
            val stitched = imageStitcher.stitchImages(bitmaps)

            // Extract text with OCR
            onStateChange(ProcessingState.ExtractingText)
            val result = ocrService.extractText(
                stitched.bitmap,
                settings.apiKey,
                settings.aiProvider,
                settings.enableChatDetection
            )

            // Format text
            val formattedText = if (result.isChat && result.chatMessages.isNotEmpty()) {
                chatDetector.formatConversation(result.chatMessages)
            } else {
                result.text
            }

            onStateChange(ProcessingState.Success(result, stitched.bitmap))
            onSuccess(stitched.bitmap, formattedText)

        } catch (e: Exception) {
            onStateChange(ProcessingState.Error(e.message ?: "Unknown error occurred"))
        }
    }

    private fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Share text"))
    }

    private fun saveBitmap(bitmap: Bitmap) {
        try {
            val file = File(getExternalFilesDir(null), "stitched_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            Toast.makeText(this, "Saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving image: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
        }
    }
}
