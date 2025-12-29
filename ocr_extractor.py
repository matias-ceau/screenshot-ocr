"""
OCR extraction module using Tesseract.
"""

import cv2
import numpy as np
import pytesseract
from typing import Dict, List, Tuple
from dataclasses import dataclass


@dataclass
class TextBlock:
    """Represents a block of detected text with position."""
    text: str
    x: int
    y: int
    width: int
    height: int
    confidence: float


class OCRExtractor:
    """Extracts text from images using Tesseract OCR."""

    def __init__(self, lang: str = 'eng'):
        """
        Initialize the OCR extractor.

        Args:
            lang: Language code for Tesseract (e.g., 'eng', 'spa', 'fra')
        """
        self.lang = lang

    def preprocess_image(self, image: np.ndarray) -> np.ndarray:
        """
        Preprocess image to improve OCR accuracy.

        Args:
            image: Input image

        Returns:
            Preprocessed image
        """
        # Convert to grayscale
        if len(image.shape) == 3:
            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        else:
            gray = image

        # Apply slight Gaussian blur to reduce noise
        blurred = cv2.GaussianBlur(gray, (3, 3), 0)

        # Apply adaptive thresholding to handle varying lighting
        processed = cv2.adaptiveThreshold(
            blurred,
            255,
            cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
            cv2.THRESH_BINARY,
            11,
            2
        )

        return processed

    def extract_text(self, image: np.ndarray, preprocess: bool = True) -> str:
        """
        Extract text from image.

        Args:
            image: Input image
            preprocess: Whether to preprocess the image

        Returns:
            Extracted text
        """
        if preprocess:
            processed = self.preprocess_image(image)
        else:
            processed = image

        # Configure Tesseract
        custom_config = r'--oem 3 --psm 6'

        # Extract text
        text = pytesseract.image_to_string(
            processed,
            lang=self.lang,
            config=custom_config
        )

        return text.strip()

    def extract_text_with_boxes(self, image: np.ndarray,
                                 preprocess: bool = True) -> List[TextBlock]:
        """
        Extract text with bounding box information.

        Args:
            image: Input image
            preprocess: Whether to preprocess the image

        Returns:
            List of TextBlock objects
        """
        if preprocess:
            processed = self.preprocess_image(image)
        else:
            processed = image

        # Get detailed information
        data = pytesseract.image_to_data(
            processed,
            lang=self.lang,
            output_type=pytesseract.Output.DICT
        )

        text_blocks = []
        n_boxes = len(data['text'])

        for i in range(n_boxes):
            text = data['text'][i].strip()
            if text:  # Only include non-empty text
                confidence = float(data['conf'][i])
                if confidence > 0:  # Filter out very low confidence results
                    text_block = TextBlock(
                        text=text,
                        x=data['left'][i],
                        y=data['top'][i],
                        width=data['width'][i],
                        height=data['height'][i],
                        confidence=confidence
                    )
                    text_blocks.append(text_block)

        return text_blocks

    def extract_structured_text(self, image: np.ndarray) -> Dict[str, any]:
        """
        Extract text with structure information (lines, paragraphs).

        Args:
            image: Input image

        Returns:
            Dictionary with structured text data
        """
        processed = self.preprocess_image(image)

        # Get detailed data including block, paragraph, and line info
        data = pytesseract.image_to_data(
            processed,
            lang=self.lang,
            output_type=pytesseract.Output.DICT
        )

        # Organize by blocks and lines
        structure = {
            'blocks': [],
            'full_text': ''
        }

        current_block = None
        current_par = None
        current_line = None

        for i in range(len(data['text'])):
            text = data['text'][i].strip()
            level = data['level'][i]
            block_num = data['block_num'][i]
            par_num = data['par_num'][i]
            line_num = data['line_num'][i]

            # New block
            if level == 2 or (current_block is None):
                current_block = {
                    'block_num': block_num,
                    'paragraphs': []
                }
                structure['blocks'].append(current_block)
                current_par = None
                current_line = None

            # New paragraph
            if level == 3 or (current_par is None or current_par['par_num'] != par_num):
                current_par = {
                    'par_num': par_num,
                    'lines': []
                }
                if current_block:
                    current_block['paragraphs'].append(current_par)
                current_line = None

            # New line
            if level == 4 or (current_line is None or current_line['line_num'] != line_num):
                current_line = {
                    'line_num': line_num,
                    'words': []
                }
                if current_par:
                    current_par['lines'].append(current_line)

            # Add word
            if text and level == 5:
                if current_line:
                    current_line['words'].append({
                        'text': text,
                        'confidence': data['conf'][i],
                        'x': data['left'][i],
                        'y': data['top'][i],
                        'width': data['width'][i],
                        'height': data['height'][i]
                    })

        # Build full text
        full_text = pytesseract.image_to_string(processed, lang=self.lang)
        structure['full_text'] = full_text.strip()

        return structure

    def save_text(self, text: str, output_path: str):
        """
        Save extracted text to file.

        Args:
            text: Text to save
            output_path: Output file path
        """
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(text)
        print(f"Saved extracted text to: {output_path}")
