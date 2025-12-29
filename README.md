# Screenshot OCR Stitcher - Android App

An Android application that stitches overlapping screenshots into a single long image and performs OCR using AI (OpenAI Vision API, Google Gemini, or Mistral).

## Features

- 📱 **Native Android App** - Built with Kotlin
- 🤖 **AI-Powered OCR** - Uses OpenAI Vision API (or Gemini/Mistral)
- 🧩 **Smart Stitching** - Automatically detects and removes overlapping regions
- 💬 **Chat Detection** - Recognizes and formats chat conversations
- 📸 **Easy to Use** - Select multiple screenshots from gallery or capture new ones
- 🎨 **Material Design** - Modern, intuitive UI

## Installation

### Option 1: Install APK (Recommended)
1. Download `screenshot-ocr.apk` from releases
2. Enable "Install from Unknown Sources" in Android settings
3. Install the APK
4. Add your OpenAI API key in settings

### Option 2: Build from Source

#### Prerequisites
- Android Studio Hedgehog or later
- JDK 17 or later
- Android SDK (API 24+)

#### Build Steps
```bash
# Clone the repository
git clone <repo-url>
cd screenshot-ocr

# Open in Android Studio
# Or build from command line:
./gradlew assembleDebug

# APK will be in app/build/outputs/apk/debug/
```

## Configuration

### API Key Setup

The app supports multiple AI providers:

1. **OpenAI** (default)
   - Add API key in app settings or
   - Set `OPENAI_API_KEY` environment variable

2. **Google Gemini**
   - Get API key from https://makersuite.google.com/
   - Select "Gemini" in app settings

3. **Mistral**
   - Get API key from https://console.mistral.ai/
   - Select "Mistral" in app settings

### First Time Setup
1. Open the app
2. Tap the settings icon (⚙️)
3. Enter your API key
4. Select your preferred AI provider
5. Save settings

## Usage

### Basic Workflow

1. **Select Screenshots**
   - Tap "Select Screenshots" button
   - Choose multiple screenshots in order (top to bottom)
   - Or use "Camera" to capture new screenshots

2. **Process**
   - App automatically detects overlaps
   - Stitches images together
   - Sends to AI for OCR

3. **View Results**
   - View stitched image
   - Read extracted text
   - If chat detected, see formatted conversation
   - Share or save results

### Tips for Best Results

- **Screenshot Order**: Select screenshots from top to bottom
- **Overlap**: Ensure 20-40% overlap between screenshots
- **Quality**: Use high-resolution screenshots
- **Lighting**: Ensure good contrast and readability

## Features in Detail

### Image Stitching
- Uses OpenCV for Android to detect overlapping regions
- Configurable similarity threshold
- Handles different screen sizes and resolutions

### AI OCR
- Supports multiple AI providers
- High accuracy text extraction
- Preserves formatting and structure
- Handles multiple languages

### Chat Detection
Automatically detects common chat patterns:
- WhatsApp format
- Telegram format
- Discord format
- SMS/iMessage format
- Generic chat formats

Formatted output includes:
- Speaker names
- Timestamps
- Message grouping
- Conversation statistics

## Permissions

The app requires:
- **READ_EXTERNAL_STORAGE** - To access screenshots from gallery
- **WRITE_EXTERNAL_STORAGE** - To save stitched images
- **INTERNET** - To connect to AI APIs
- **CAMERA** (optional) - To capture new screenshots

## Architecture

```
app/
├── src/main/
│   ├── java/com/screenshot/ocr/
│   │   ├── MainActivity.kt          # Main UI
│   │   ├── ImageStitcher.kt        # Image stitching logic
│   │   ├── OcrService.kt           # AI OCR integration
│   │   ├── ChatDetector.kt         # Chat detection
│   │   ├── SettingsActivity.kt     # Settings UI
│   │   └── models/                 # Data models
│   ├── res/                        # Resources (layouts, strings, etc.)
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## Technologies Used

- **Kotlin** - Primary language
- **Jetpack Compose** - Modern UI toolkit
- **OpenCV for Android** - Image processing
- **Retrofit** - API networking
- **Coroutines** - Asynchronous operations
- **Material 3** - Design system

## Privacy & Security

- API keys stored securely using Android Keystore
- No data sent to third parties (except chosen AI provider)
- Images processed locally before OCR
- Option to delete images after processing

## Roadmap

- [ ] Support for more AI providers (Claude, Llama Vision)
- [ ] Offline mode with on-device OCR
- [ ] Batch processing
- [ ] Export to PDF
- [ ] Cloud sync (optional)
- [ ] Widget support

## License

MIT License - see LICENSE file

## Support

For issues and feature requests, please open an issue on GitHub.
