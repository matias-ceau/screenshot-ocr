#!/usr/bin/env python3
"""
Test script to demonstrate the screenshot stitcher functionality.
Creates sample overlapping images for testing.
"""

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import os


def create_test_screenshots():
    """Create sample overlapping screenshots for testing."""

    print("Creating test screenshots...")

    # Create output directory
    os.makedirs('test_images', exist_ok=True)

    # Image dimensions
    width, height = 800, 600
    overlap_height = 100  # 100 pixels of overlap

    # Create three overlapping screenshots with text
    texts = [
        [
            "This is the first screenshot.",
            "It contains some text at the top.",
            "This is a test of the screenshot stitcher.",
            "The overlap region starts here:",
            "OVERLAP REGION - PART 1",
            "This text appears in both screenshots."
        ],
        [
            "OVERLAP REGION - PART 1",
            "This text appears in both screenshots.",
            "This is the second screenshot.",
            "It continues from the first one.",
            "More text in the middle section.",
            "Another overlap region starts here:",
            "OVERLAP REGION - PART 2",
            "This is shared with screenshot 3."
        ],
        [
            "OVERLAP REGION - PART 2",
            "This is shared with screenshot 3.",
            "This is the third and final screenshot.",
            "It contains the conclusion.",
            "The stitcher should combine all three",
            "into one long image.",
            "End of document."
        ]
    ]

    # Create each screenshot
    for i, text_lines in enumerate(texts, 1):
        # Create white background
        img = Image.new('RGB', (width, height), color='white')
        draw = ImageDraw.Draw(img)

        # Try to use a truetype font, fall back to default if not available
        try:
            font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 24)
        except:
            font = ImageFont.load_default()

        # Draw text
        y_position = 50
        for line in text_lines:
            draw.text((50, y_position), line, fill='black', font=font)
            y_position += 80

        # Save image
        filename = f'test_images/screenshot_{i}.png'
        img.save(filename)
        print(f"Created: {filename}")

    print("\nTest images created successfully!")
    print("\nNow run:")
    print("  python screenshot_ocr.py test_images/screenshot_*.png")


def create_test_chat_screenshots():
    """Create sample chat conversation screenshots."""

    print("\nCreating test chat screenshots...")

    os.makedirs('test_images', exist_ok=True)

    width, height = 600, 800

    # Chat messages
    chats = [
        [
            "Alice: Hey everyone!",
            "Bob: Hi Alice! How are you?",
            "Alice: I'm doing great, thanks!",
            "Alice: How about you?",
            "Charlie: Hey folks!",
            "[2:30 PM] Bob: I'm good too!",
            "Alice: What are you working on?",
            "OVERLAP - CHAT 1"
        ],
        [
            "OVERLAP - CHAT 1",
            "[2:31 PM] Bob: Working on a new project",
            "Charlie: That sounds interesting!",
            "Charlie: Tell us more",
            "Alice: Yeah, I'd love to hear about it",
            "[2:32 PM] Bob: It's a screenshot stitcher",
            "Bob: With OCR capabilities",
            "OVERLAP - CHAT 2"
        ],
        [
            "OVERLAP - CHAT 2",
            "Alice: That's so cool!",
            "[2:33 PM] Charlie: Nice!",
            "Bob: Thanks! It can even detect chats",
            "Alice: Amazing work Bob!",
            "Charlie: When can we try it?",
            "[2:34 PM] Bob: It's ready now!",
            "Alice: Awesome!"
        ]
    ]

    for i, chat_lines in enumerate(chats, 1):
        img = Image.new('RGB', (width, height), color='white')
        draw = ImageDraw.Draw(img)

        try:
            font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 20)
        except:
            font = ImageFont.load_default()

        y_position = 50
        for line in chat_lines:
            draw.text((30, y_position), line, fill='black', font=font)
            y_position += 80

        filename = f'test_images/chat_{i}.png'
        img.save(filename)
        print(f"Created: {filename}")

    print("\nTest chat images created successfully!")
    print("\nNow run:")
    print("  python screenshot_ocr.py test_images/chat_*.png --chat")


if __name__ == '__main__':
    create_test_screenshots()
    print("\n" + "=" * 60)
    create_test_chat_screenshots()
    print("\n" + "=" * 60)
    print("\nAll test images created!")
    print("\nTo test the application:")
    print("  1. Regular stitching: python screenshot_ocr.py test_images/screenshot_*.png")
    print("  2. Chat detection: python screenshot_ocr.py test_images/chat_*.png --chat")
