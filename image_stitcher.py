"""
Image stitching module for combining overlapping screenshots.
"""

import cv2
import numpy as np
from PIL import Image
from typing import List, Tuple, Optional


class ImageStitcher:
    """Stitches overlapping screenshots into a single long image."""

    def __init__(self, overlap_threshold: float = 0.80):
        """
        Initialize the image stitcher.

        Args:
            overlap_threshold: Minimum correlation threshold for overlap detection (0-1)
        """
        self.overlap_threshold = overlap_threshold

    def find_overlap(self, img1: np.ndarray, img2: np.ndarray,
                     search_region: float = 0.5) -> Optional[int]:
        """
        Find the overlapping region between two images.

        The bottom of img1 should overlap with the top of img2.

        Args:
            img1: First image (top image)
            img2: Second image (bottom image)
            search_region: Portion of image to search (0-1)

        Returns:
            The y-offset in img1 where the overlap starts, or None if no overlap found
        """
        h1, w1 = img1.shape[:2]
        h2, w2 = img2.shape[:2]

        # Use the bottom portion of img1 as search space
        search_height = int(h1 * search_region)
        search_start = h1 - search_height

        # Try different overlap sizes
        min_overlap = int(min(h1, h2) * 0.1)  # At least 10% overlap
        max_overlap = int(min(h1, h2) * 0.9)  # At most 90% overlap

        best_match_score = 0
        best_offset = None

        # Search for the best match
        for overlap_size in range(max_overlap, min_overlap, -10):
            # Template from bottom of img1
            template = img1[h1 - overlap_size:h1, :]

            # Search area from top of img2
            search_area = img2[:min(overlap_size + 100, h2), :]

            # Resize to same width if needed
            if w1 != w2:
                if w1 < w2:
                    template = cv2.resize(template, (w2, template.shape[0]))
                else:
                    search_area = cv2.resize(search_area, (w1, search_area.shape[0]))

            # Convert to grayscale for matching
            template_gray = cv2.cvtColor(template, cv2.COLOR_BGR2GRAY) if len(template.shape) == 3 else template
            search_gray = cv2.cvtColor(search_area, cv2.COLOR_BGR2GRAY) if len(search_area.shape) == 3 else search_area

            if template_gray.shape[0] > search_gray.shape[0]:
                continue

            # Template matching
            result = cv2.matchTemplate(search_gray, template_gray, cv2.TM_CCOEFF_NORMED)
            _, max_val, _, max_loc = cv2.minMaxLoc(result)

            # Check if this is the best match
            if max_val > best_match_score and max_val >= self.overlap_threshold:
                best_match_score = max_val
                best_offset = h1 - overlap_size

        if best_offset is not None:
            print(f"Found overlap with confidence: {best_match_score:.2%}")
            return best_offset

        print("No overlap detected, images will be concatenated")
        return None

    def stitch_two_images(self, img1: np.ndarray, img2: np.ndarray) -> np.ndarray:
        """
        Stitch two overlapping images together.

        Args:
            img1: First image (top)
            img2: Second image (bottom)

        Returns:
            Stitched image
        """
        overlap_offset = self.find_overlap(img1, img2)

        if overlap_offset is None:
            # No overlap found, just concatenate
            # Match widths
            h1, w1 = img1.shape[:2]
            h2, w2 = img2.shape[:2]

            if w1 != w2:
                max_width = max(w1, w2)
                if w1 < max_width:
                    img1 = cv2.resize(img1, (max_width, h1))
                if w2 < max_width:
                    img2 = cv2.resize(img2, (max_width, h2))

            return np.vstack([img1, img2])

        # Stitch with overlap
        h1, w1 = img1.shape[:2]
        h2, w2 = img2.shape[:2]

        # Match widths if needed
        if w1 != w2:
            max_width = max(w1, w2)
            if w1 < max_width:
                img1 = cv2.resize(img1, (max_width, h1))
                w1 = max_width
            if w2 < max_width:
                img2 = cv2.resize(img2, (max_width, h2))
                w2 = max_width

        # Take the non-overlapping part of img1 and all of img2
        overlap_height = h1 - overlap_offset

        # Use img1 up to the overlap point, then img2 from the overlap point
        result = np.vstack([img1[:overlap_offset, :], img2])

        return result

    def stitch_multiple_images(self, image_paths: List[str]) -> np.ndarray:
        """
        Stitch multiple overlapping screenshots.

        Args:
            image_paths: List of image file paths in order

        Returns:
            Stitched image as numpy array
        """
        if not image_paths:
            raise ValueError("No images provided")

        if len(image_paths) == 1:
            img = cv2.imread(image_paths[0])
            if img is None:
                raise ValueError(f"Could not read image: {image_paths[0]}")
            return img

        print(f"Stitching {len(image_paths)} images...")

        # Load first image
        result = cv2.imread(image_paths[0])
        if result is None:
            raise ValueError(f"Could not read image: {image_paths[0]}")

        print(f"Loaded image 1: {image_paths[0]}")

        # Stitch each subsequent image
        for i, img_path in enumerate(image_paths[1:], start=2):
            img = cv2.imread(img_path)
            if img is None:
                print(f"Warning: Could not read image: {img_path}, skipping")
                continue

            print(f"Stitching image {i}: {img_path}")
            result = self.stitch_two_images(result, img)

        print(f"Final stitched image size: {result.shape[1]}x{result.shape[0]}")
        return result

    def save_image(self, image: np.ndarray, output_path: str):
        """
        Save the stitched image.

        Args:
            image: Image array
            output_path: Output file path
        """
        cv2.imwrite(output_path, image)
        print(f"Saved stitched image to: {output_path}")
