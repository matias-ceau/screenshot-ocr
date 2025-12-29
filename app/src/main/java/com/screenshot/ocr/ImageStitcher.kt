package com.screenshot.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import com.screenshot.ocr.models.OverlapRegion
import com.screenshot.ocr.models.StitchedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max

/**
 * Stitches overlapping screenshots into a single long image
 */
class ImageStitcher(
    private val overlapThreshold: Float = 0.8f
) {

    /**
     * Stitch multiple bitmaps together by detecting overlapping regions
     */
    suspend fun stitchImages(bitmaps: List<Bitmap>): StitchedResult = withContext(Dispatchers.Default) {
        if (bitmaps.isEmpty()) {
            throw IllegalArgumentException("No images provided")
        }

        if (bitmaps.size == 1) {
            return@withContext StitchedResult(
                bitmap = bitmaps[0],
                width = bitmaps[0].width,
                height = bitmaps[0].height
            )
        }

        val overlapRegions = mutableListOf<OverlapRegion>()
        var currentBitmap = bitmaps[0]

        for (i in 1 until bitmaps.size) {
            val nextBitmap = bitmaps[i]
            val (stitched, overlap) = stitchTwoBitmaps(currentBitmap, nextBitmap, i)
            currentBitmap = stitched
            overlap?.let { overlapRegions.add(it) }
        }

        StitchedResult(
            bitmap = currentBitmap,
            width = currentBitmap.width,
            height = currentBitmap.height,
            overlapRegions = overlapRegions
        )
    }

    /**
     * Stitch two bitmaps together
     */
    private fun stitchTwoBitmaps(
        top: Bitmap,
        bottom: Bitmap,
        index: Int
    ): Pair<Bitmap, OverlapRegion?> {
        val overlapInfo = findOverlap(top, bottom)

        return if (overlapInfo != null) {
            // Found overlap, stitch intelligently
            val (yOffset, confidence) = overlapInfo
            val stitched = mergeBitmapsWithOverlap(top, bottom, yOffset)
            val overlap = OverlapRegion(
                imageIndex = index,
                yOffset = yOffset,
                height = top.height - yOffset,
                confidence = confidence
            )
            Pair(stitched, overlap)
        } else {
            // No overlap found, just concatenate
            val stitched = concatenateBitmaps(top, bottom)
            Pair(stitched, null)
        }
    }

    /**
     * Find overlapping region between bottom of top image and top of bottom image
     * Returns (yOffset in top image, confidence) or null
     */
    private fun findOverlap(top: Bitmap, bottom: Bitmap): Pair<Int, Float>? {
        val width = min(top.width, bottom.width)
        val searchHeight = min(top.height, bottom.height) / 2 // Search in middle 50%

        val minOverlap = min(top.height, bottom.height) / 10 // At least 10%
        val maxOverlap = min(top.height, bottom.height) * 9 / 10 // At most 90%

        var bestMatch = 0f
        var bestOffset = -1

        // Search for best overlap
        for (overlapHeight in maxOverlap downTo minOverlap step 10) {
            val similarity = compareRegions(
                top,
                bottom,
                top.height - overlapHeight,
                0,
                overlapHeight,
                width
            )

            if (similarity > bestMatch) {
                bestMatch = similarity
                bestOffset = top.height - overlapHeight
            }

            // Early exit if we found a very good match
            if (similarity > 0.95f) {
                break
            }
        }

        return if (bestMatch >= overlapThreshold) {
            Pair(bestOffset, bestMatch)
        } else {
            null
        }
    }

    /**
     * Compare two regions of bitmaps for similarity
     */
    private fun compareRegions(
        bitmap1: Bitmap,
        bitmap2: Bitmap,
        y1: Int,
        y2: Int,
        height: Int,
        width: Int
    ): Float {
        var totalDiff = 0L
        var pixelCount = 0

        val step = max(1, height / 50) // Sample every N rows for performance

        for (y in 0 until height step step) {
            if (y1 + y >= bitmap1.height || y2 + y >= bitmap2.height) break

            for (x in 0 until width step 4) { // Sample pixels
                if (x >= bitmap1.width || x >= bitmap2.width) continue

                val pixel1 = bitmap1.getPixel(x, y1 + y)
                val pixel2 = bitmap2.getPixel(x, y2 + y)

                val r1 = (pixel1 shr 16) and 0xFF
                val g1 = (pixel1 shr 8) and 0xFF
                val b1 = pixel1 and 0xFF

                val r2 = (pixel2 shr 16) and 0xFF
                val g2 = (pixel2 shr 8) and 0xFF
                val b2 = pixel2 and 0xFF

                totalDiff += abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)
                pixelCount++
            }
        }

        if (pixelCount == 0) return 0f

        // Average difference per channel (0-255)
        val avgDiff = totalDiff.toFloat() / (pixelCount * 3)

        // Convert to similarity (0-1, where 1 is identical)
        return 1f - (avgDiff / 255f)
    }

    /**
     * Merge two bitmaps with overlap
     */
    private fun mergeBitmapsWithOverlap(
        top: Bitmap,
        bottom: Bitmap,
        yOffset: Int
    ): Bitmap {
        val width = max(top.width, bottom.width)
        val height = yOffset + bottom.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw top image
        canvas.drawBitmap(top, 0f, 0f, null)

        // Draw bottom image starting at offset
        canvas.drawBitmap(bottom, 0f, yOffset.toFloat(), null)

        return result
    }

    /**
     * Simply concatenate two bitmaps vertically
     */
    private fun concatenateBitmaps(top: Bitmap, bottom: Bitmap): Bitmap {
        val width = max(top.width, bottom.width)
        val height = top.height + bottom.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        canvas.drawBitmap(top, 0f, 0f, null)
        canvas.drawBitmap(bottom, 0f, top.height.toFloat(), null)

        return result
    }
}
