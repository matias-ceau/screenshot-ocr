# Building the Android App

## Prerequisites

1. **Android Studio**
   - Download from: https://developer.android.com/studio
   - Version: Hedgehog (2023.1.1) or later

2. **JDK 17**
   - Required for Android Gradle Plugin 8.x
   - Can be installed through Android Studio

3. **Android SDK**
   - Minimum SDK: API 24 (Android 7.0)
   - Target SDK: API 34 (Android 14)
   - Install through Android Studio SDK Manager

## Setup

1. **Clone the repository**
   ```bash
   git clone <repo-url>
   cd screenshot-ocr
   ```

2. **Configure local.properties**
   ```bash
   # Copy the example
   cp local.properties.example local.properties

   # Edit local.properties and set your SDK path
   # Example on Linux:
   sdk.dir=/home/username/Android/Sdk
   ```

3. **Sync Gradle**
   - Open Android Studio
   - File → Open → Select screenshot-ocr directory
   - Wait for Gradle sync to complete
   - Gradle will download all dependencies

## Build from Android Studio

1. **Open Project**
   - File → Open → screenshot-ocr

2. **Build APK**
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - APK location: `app/build/outputs/apk/debug/app-debug.apk`

3. **Run on Device/Emulator**
   - Connect Android device or start emulator
   - Run → Run 'app'

## Build from Command Line

### Debug Build

```bash
# Linux/Mac
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build

```bash
# Create a signing keystore (first time only)
keytool -genkey -v -keystore release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias screenshot-ocr

# Build release APK
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

### Install to Device

```bash
# Install debug APK
./gradlew installDebug

# Or manually with adb
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Troubleshooting

### Gradle Sync Failed

1. Check internet connection (Gradle needs to download dependencies)
2. Verify JDK 17 is installed
3. Try: File → Invalidate Caches → Invalidate and Restart

### SDK Not Found

1. Open Android Studio → Preferences → Appearance & Behavior → System Settings → Android SDK
2. Note the SDK path
3. Update `local.properties` with correct path

### Build Failed

1. Clean project:
   ```bash
   ./gradlew clean
   ```

2. Rebuild:
   ```bash
   ./gradlew assembleDebug
   ```

3. Check `app/build.gradle.kts` for correct dependency versions

### OpenCV Issues

If you encounter OpenCV-related errors:
1. The app uses a pure Kotlin implementation without OpenCV dependency
2. No additional setup needed

## Testing

### Run Unit Tests

```bash
./gradlew test
```

### Run on Device

```bash
./gradlew connectedAndroidTest
```

## Release Checklist

Before creating a release:

- [ ] Update version in `app/build.gradle.kts`
- [ ] Test on multiple devices/Android versions
- [ ] Test with different AI providers (OpenAI, Gemini, Mistral)
- [ ] Test image stitching with various screenshot sizes
- [ ] Test chat detection with different chat apps
- [ ] Update CHANGELOG.md
- [ ] Create signed release APK
- [ ] Test signed APK on clean device

## Distribution

### Google Play Store

1. Create signed release bundle:
   ```bash
   ./gradlew bundleRelease
   ```
   Output: `app/build/outputs/bundle/release/app-release.aab`

2. Upload to Google Play Console

### Direct APK Distribution

1. Build signed APK:
   ```bash
   ./gradlew assembleRelease
   ```

2. Share `app-release.apk`

## Development Tips

### Fast Iteration

```bash
# Build and install in one command
./gradlew installDebug && adb shell am start -n com.screenshot.ocr/.MainActivity
```

### View Logs

```bash
adb logcat | grep screenshot.ocr
```

### Clear App Data

```bash
adb shell pm clear com.screenshot.ocr
```

## Dependencies

Key dependencies used:
- Jetpack Compose: Modern UI
- Retrofit: API networking
- Kotlin Coroutines: Async operations
- Encrypted SharedPreferences: Secure API key storage
- Material 3: Design system

All dependencies are managed in `app/build.gradle.kts`.
