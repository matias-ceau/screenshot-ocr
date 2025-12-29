# Usage Guide

## Installation

### 1. Install System Dependencies

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install -y tesseract-ocr tesseract-ocr-eng
```

**macOS:**
```bash
brew install tesseract
```

**Windows:**
Download and install from: https://github.com/UB-Mannheim/tesseract/wiki

### 2. Install Python Dependencies

```bash
pip install -r requirements.txt
```

## Basic Usage

### Stitch and OCR Screenshots

```bash
python screenshot_ocr.py screenshot1.png screenshot2.png screenshot3.png
```

This will:
- Stitch the screenshots into `stitched_output.png`
- Extract text to `extracted_text.txt`

### Custom Output Files

```bash
python screenshot_ocr.py *.png -o my_output.png -t my_text.txt
```

### Chat Conversation Detection

```bash
python screenshot_ocr.py chat1.png chat2.png chat3.png --chat
```

This will detect if the content is a chat conversation and format it nicely with speakers and timestamps.

### Text-Only Mode (Skip Image Saving)

```bash
python screenshot_ocr.py *.png --text-only
```

This is faster if you only need the text and don't need to save the stitched image.

## Advanced Options

### Adjust Overlap Detection Sensitivity

```bash
python screenshot_ocr.py *.png --overlap-threshold 0.85
```

Higher values (closer to 1.0) require more exact matches. Lower values (0.7-0.8) are more forgiving.

### Disable Image Preprocessing

```bash
python screenshot_ocr.py *.png --no-preprocess
```

By default, images are preprocessed (grayscale, blur, threshold) to improve OCR. Use this flag to use raw images.

### Different Language

```bash
python screenshot_ocr.py *.png --lang spa
```

Supported languages depend on your Tesseract installation. Common codes:
- `eng` - English
- `spa` - Spanish
- `fra` - French
- `deu` - German
- `chi_sim` - Simplified Chinese
- `jpn` - Japanese

## Tips for Best Results

### 1. Screenshot Order Matters

Ensure screenshots are in the correct order (top to bottom). The app expects:
- Bottom of screenshot 1 overlaps with top of screenshot 2
- Bottom of screenshot 2 overlaps with top of screenshot 3
- And so on...

### 2. Consistent Overlap

Try to maintain consistent overlap between screenshots (20-40% is ideal).

### 3. Clear Screenshots

- Use high resolution screenshots
- Ensure text is crisp and readable
- Avoid heavily compressed images

### 4. Chat Detection

The chat detector looks for patterns like:
- `Name: message`
- `[12:30 PM] Name: message`
- `Name [12:30]: message`

If your chat uses a different format, the raw text will still be extracted.

## Troubleshooting

### No Text Extracted

- Check that Tesseract is properly installed: `tesseract --version`
- Try with `--no-preprocess` flag
- Verify your screenshots contain readable text
- Check image quality and resolution

### Poor Overlap Detection

- Adjust `--overlap-threshold` (try values between 0.7 and 0.9)
- Ensure screenshots actually overlap
- Check that screenshots are in correct order

### Chat Not Detected

- Use the `--chat` flag
- Check that your chat follows common patterns
- The detector needs at least 30% of lines to match chat patterns

## Examples

### Example 1: Simple Screenshot Stitching

```bash
# Capture overlapping screenshots of a long article
python screenshot_ocr.py article_part1.png article_part2.png article_part3.png
```

### Example 2: WhatsApp Chat Extraction

```bash
# Screenshots of a WhatsApp conversation
python screenshot_ocr.py chat*.png --chat -o whatsapp_chat.png -t whatsapp_chat.txt
```

### Example 3: Quick Text Extraction

```bash
# Just get the text, don't save stitched image
python screenshot_ocr.py page*.png --text-only
cat extracted_text.txt
```

### Example 4: Multiple Languages

```bash
# Spanish document
python screenshot_ocr.py documento*.png --lang spa -t texto.txt
```

## Output Format

### Regular Text

Plain text extracted from the image, preserving line breaks and spacing.

### Chat Format (with --chat flag)

```
Detected chat conversation with 15 messages
Participants: Alice, Bob, Charlie

============================================================
CHAT CONVERSATION
============================================================

Alice:
  Hey everyone!

[2:30 PM] Bob:
  Hi Alice! How are you?

Alice:
  I'm good, thanks for asking!
  How about you?

============================================================
Total messages: 15
============================================================
```
