package com.sweep.cleaner.util

import android.graphics.Bitmap

object ImageHasher {

    /**
     * Computes a 64-bit difference hash (dHash) from a bitmap resized to 9x8.
     * Grayscale luminance formula: 0.299 R + 0.587 G + 0.114 B.
     * Each of the 8 rows produces 8 bits comparing pixel[x] > pixel[x+1],
     * yielding exactly 64 bits represented as a [Long].
     */
    fun computeDHash(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        val pixels = IntArray(72)
        scaled.getPixels(pixels, 0, 9, 0, 0, 9, 8)
        if (scaled != bitmap) {
            scaled.recycle()
        }

        val gray = IntArray(72)
        for (i in 0 until 72) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            gray[i] = (r * 299 + g * 587 + b * 114) / 1000
        }

        return computeDHashFromGrayscale(gray)
    }

    /**
     * Pure array-based dHash computation for JVM testing and verification.
     * Takes an array of 72 grayscale values (9 width x 8 height).
     */
    fun computeDHashFromGrayscale(gray72: IntArray): Long {
        require(gray72.size == 72) { "Grayscale array must contain exactly 72 elements (9x8)" }
        var hash = 0L
        var bitIndex = 0
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val left = gray72[y * 9 + x]
                val right = gray72[y * 9 + (x + 1)]
                if (left > right) {
                    hash = hash or (1L shl bitIndex)
                }
                bitIndex++
            }
        }
        return hash
    }

    /**
     * Calculates the Hamming distance between two 64-bit difference hashes.
     * Return value ranges from 0 (identical) to 64 (complete opposite).
     * Typically, photos with distance <= 10 are considered visually similar.
     */
    fun hammingDistance(hash1: Long, hash2: Long): Int {
        return java.lang.Long.bitCount(hash1 xor hash2)
    }
}
