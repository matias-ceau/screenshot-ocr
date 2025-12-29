# Quick Start Guide

Get started with Screenshot OCR in 5 minutes!

## For Users (Install APK)

### 1. Install the App

**Option A: From Releases**
1. Download `screenshot-ocr.apk` from the releases page
2. Open the APK file on your Android device
3. Allow "Install from Unknown Sources" if prompted
4. Install the app

**Option B: Build Yourself**
See [BUILDING.md](BUILDING.md) for detailed build instructions.

### 2. Get an API Key

Choose one of these AI providers:

**OpenAI (Recommended)** - Most accurate
- Go to: https://platform.openai.com/api-keys
- Create account or sign in
- Click "Create new secret key"
- Copy the key (starts with `sk-...`)
- Cost: ~$0.01-0.05 per image

**Google Gemini** - Good balance
- Go to: https://makersuite.google.com/app/apikey
- Sign in with Google account
- Click "Create API Key"
- Copy the key
- Has free tier with quota

**Mistral** - Fast and efficient
- Go to: https://console.mistral.ai/
- Create account
- Navigate to API Keys
- Create new key
- Copy the key

### 3. Configure the App

1. Open Screenshot OCR app
2. Tap the settings icon (⚙️) in top right
3. Select your AI provider
4. Paste your API key
5. Tap "Save Settings"

### 4. Use the App

#### Basic Usage
1. Tap "Select Screenshots"
2. Select multiple screenshots **in order** (top to bottom)
3. Tap "Process Images"
4. Wait for stitching and OCR
5. View results!

#### For Chat Conversations
The app automatically detects chat conversations from:
- WhatsApp
- Telegram
- Discord
- iMessage/SMS
- Most chat apps

Just process the screenshots and it will format them nicely!

## Example Workflow

### Scenario: Export Long WhatsApp Chat

1. **Take Screenshots**
   - Open WhatsApp conversation
   - Take screenshot of top
   - Scroll down ~70% (leave overlap!)
   - Take another screenshot
   - Repeat until you reach the end

2. **Process in App**
   - Open Screenshot OCR
   - Select all screenshots in order
   - Tap "Process Images"

3. **Get Results**
   - View stitched long image
   - Read formatted chat with speakers and timestamps
   - Share or save the text

## Tips for Best Results

### Screenshot Overlap
- **Good**: 20-40% overlap between screenshots
- **Too little**: App might not detect overlap
- **Too much**: Works but wastes processing

### Screenshot Quality
- Use high resolution
- Ensure text is readable
- Good lighting/contrast
- Avoid blur or motion

### Ordering
- **Critical**: Select screenshots in correct order (top to bottom)
- Wrong order = garbage output

### Chat Detection
- Works automatically for most chat apps
- Recognizes common patterns
- Preserves timestamps and speakers

## Troubleshooting

### "Please set API key in settings"
- Go to Settings (⚙️ icon)
- Enter your API key
- Make sure you selected the correct provider

### "Error processing images"
Possible causes:
1. **No internet** - App needs internet for AI API
2. **Invalid API key** - Check your key is correct
3. **Insufficient API credits** - Add credits to your AI provider account
4. **Images too large** - Try with fewer/smaller screenshots

### Poor Overlap Detection
- Ensure screenshots actually overlap
- Adjust "Overlap Threshold" in settings (try 70-85%)
- Make sure screenshots are in correct order

### Chat Not Detected
- Most chats are detected automatically
- Check "Enable chat detection" in settings
- Chat patterns must follow common formats (Name: message)

### App Crashes
1. Clear app data: Settings → Apps → Screenshot OCR → Clear Data
2. Reinstall the app
3. Check Android version (minimum: Android 7.0)

## Advanced Features

### Adjust Overlap Sensitivity
Settings → Overlap Detection Threshold
- Higher (90%): Requires very exact matches
- Lower (70%): More forgiving, might accept false matches
- Default (80%): Good for most cases

### Switch AI Providers
Try different providers to compare:
- **OpenAI**: Most accurate, best for complex text
- **Gemini**: Fast, good free tier
- **Mistral**: Efficient, good for simple text

## Privacy & Security

- API keys stored securely using Android Keystore
- Images processed locally before sending
- Only final stitched image sent to AI provider
- No data stored on third-party servers
- Can delete images after processing

## Cost Estimates

Per 100 images processed:

- **OpenAI**: ~$1-5 (depends on image size)
- **Gemini**: Free tier available, then ~$0.50-2
- **Mistral**: ~$0.50-2

Tips to reduce costs:
- Use smaller images when possible
- Compress screenshots
- Use Gemini's free tier for testing

## Next Steps

- Try the app with different types of content
- Experiment with overlap threshold settings
- Share extracted text to notes apps
- Export conversations to PDF (coming soon!)

## Need Help?

- Check [README.md](README.md) for features
- See [BUILDING.md](BUILDING.md) for building from source
- Open an issue on GitHub for bugs
- Join discussions for questions

## Pro Tips

1. **Batch Processing**: Select all screenshots at once, don't process one by one
2. **Consistent Screenshots**: Use same device/orientation for all screenshots
3. **Clean Up**: Delete screenshots after processing to save space
4. **Backup**: Save important extracted text immediately
5. **Test First**: Try with 2-3 screenshots before doing a huge batch

Enjoy using Screenshot OCR! 🎉
