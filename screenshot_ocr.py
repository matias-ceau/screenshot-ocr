#!/usr/bin/env python3
"""
Screenshot OCR Stitcher - Main CLI Application

Stitches overlapping screenshots and performs OCR with chat detection.
"""

import argparse
import sys
import os
from pathlib import Path

from image_stitcher import ImageStitcher
from ocr_extractor import OCRExtractor
from chat_detector import ChatDetector


def parse_arguments():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(
        description='Stitch overlapping screenshots and extract text with OCR',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s image1.png image2.png image3.png
  %(prog)s *.png -o output.png --chat
  %(prog)s screenshots/*.png --text-only
        """
    )

    parser.add_argument(
        'images',
        nargs='+',
        help='Screenshot image files to stitch (in order)'
    )

    parser.add_argument(
        '-o', '--output',
        default='stitched_output.png',
        help='Output image file path (default: stitched_output.png)'
    )

    parser.add_argument(
        '-t', '--text-output',
        default='extracted_text.txt',
        help='Text output file path (default: extracted_text.txt)'
    )

    parser.add_argument(
        '--chat',
        action='store_true',
        help='Enable chat conversation detection and formatting'
    )

    parser.add_argument(
        '--text-only',
        action='store_true',
        help='Only extract text, don\'t save stitched image'
    )

    parser.add_argument(
        '--overlap-threshold',
        type=float,
        default=0.80,
        help='Minimum overlap detection threshold 0-1 (default: 0.80)'
    )

    parser.add_argument(
        '--no-preprocess',
        action='store_true',
        help='Disable image preprocessing for OCR'
    )

    parser.add_argument(
        '--lang',
        default='eng',
        help='Tesseract language code (default: eng)'
    )

    return parser.parse_args()


def validate_images(image_paths):
    """
    Validate that all image files exist.

    Args:
        image_paths: List of image file paths

    Returns:
        List of validated paths

    Raises:
        FileNotFoundError: If any image file doesn't exist
    """
    validated = []
    for path in image_paths:
        if not os.path.exists(path):
            raise FileNotFoundError(f"Image file not found: {path}")
        if not os.path.isfile(path):
            raise ValueError(f"Not a file: {path}")
        validated.append(path)

    return validated


def main():
    """Main application entry point."""
    args = parse_arguments()

    print("Screenshot OCR Stitcher")
    print("=" * 60)

    try:
        # Validate input files
        image_paths = validate_images(args.images)
        print(f"Input images: {len(image_paths)}")

        # Step 1: Stitch images
        print("\n[1/3] Stitching images...")
        stitcher = ImageStitcher(overlap_threshold=args.overlap_threshold)
        stitched_image = stitcher.stitch_multiple_images(image_paths)

        # Save stitched image unless text-only mode
        if not args.text_only:
            stitcher.save_image(stitched_image, args.output)
            print(f"✓ Stitched image saved to: {args.output}")

        # Step 2: Extract text with OCR
        print("\n[2/3] Extracting text with OCR...")
        ocr = OCRExtractor(lang=args.lang)
        text = ocr.extract_text(
            stitched_image,
            preprocess=not args.no_preprocess
        )

        if not text.strip():
            print("Warning: No text was extracted from the image")
            return 1

        print(f"✓ Extracted {len(text)} characters")

        # Step 3: Process text (chat detection if enabled)
        print("\n[3/3] Processing extracted text...")

        if args.chat:
            detector = ChatDetector()
            is_chat, formatted_text = detector.process_text(text)

            if is_chat:
                print("✓ Chat conversation detected!")
                final_text = formatted_text
            else:
                print("No chat pattern detected, using raw text")
                final_text = text
        else:
            final_text = text

        # Save text output
        ocr.save_text(final_text, args.text_output)
        print(f"✓ Text saved to: {args.text_output}")

        # Print preview
        print("\n" + "=" * 60)
        print("TEXT PREVIEW (first 500 characters):")
        print("=" * 60)
        preview = final_text[:500]
        if len(final_text) > 500:
            preview += "\n... (truncated)"
        print(preview)
        print("=" * 60)

        print("\n✓ Processing complete!")

        # Summary
        print("\nSummary:")
        print(f"  Images stitched: {len(image_paths)}")
        print(f"  Output image: {args.output if not args.text_only else 'N/A (text-only mode)'}")
        print(f"  Output text: {args.text_output}")
        print(f"  Characters extracted: {len(text)}")
        if args.chat:
            print(f"  Chat detected: {'Yes' if is_chat else 'No'}")

        return 0

    except FileNotFoundError as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1
    except ValueError as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1
    except Exception as e:
        print(f"Unexpected error: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        return 1


if __name__ == '__main__':
    sys.exit(main())
