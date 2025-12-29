# Screenshot OCR Stitcher

A Python application that stitches overlapping screenshots into a single long image and performs OCR with intelligent chat conversation detection.

## Features

- **Smart Screenshot Stitching**: Automatically detects overlapping regions between consecutive screenshots
- **OCR Extraction**: Extracts text from the stitched image using Tesseract OCR
- **Chat Detection**: Intelligently detects chat conversations and formats them appropriately
- **High Accuracy**: Uses image matching algorithms to find exact overlap regions

## Installation

1. Install system dependencies:
```bash
# Ubuntu/Debian
sudo apt-get install tesseract-ocr

# macOS
brew install tesseract

# Windows
# Download installer from: https://github.com/UB-Mannheim/tesseract/wiki
```

2. Install Python dependencies:
```bash
pip install -r requirements.txt
```

## Usage

### Basic Usage

```bash
python screenshot_ocr.py screenshot1.png screenshot2.png screenshot3.png -o output.png
```

### With Chat Detection

```bash
python screenshot_ocr.py screenshot1.png screenshot2.png screenshot3.png -o output.png --chat
```

### Output Text Only

```bash
python screenshot_ocr.py screenshot1.png screenshot2.png screenshot3.png --text-only
```

### Options

- `-o, --output`: Output image file path (default: stitched_output.png)
- `-t, --text-output`: Text output file path (default: extracted_text.txt)
- `--chat`: Enable chat conversation detection and formatting
- `--text-only`: Only extract text, don't save stitched image
- `--overlap-threshold`: Minimum overlap percentage to detect (default: 80)

## How It Works

1. **Overlap Detection**: The app compares the bottom portion of each screenshot with the top portion of the next one using template matching
2. **Image Stitching**: Once overlap is detected, images are combined by removing duplicate regions
3. **OCR Processing**: Tesseract OCR extracts text from the final stitched image
4. **Chat Formatting**: Detects message patterns and formats them as a conversation with speakers and timestamps

## Examples

See the `examples/` directory for sample screenshots and outputs.

## License

MIT License
