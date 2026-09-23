package com.sweep.cleaner.util

import com.sweep.cleaner.model.MediaItem
import com.sweep.cleaner.model.SimilarPhotoGroup
import java.util.UUID

object SimilarPhotoDetector {

    const val DEFAULT_HAMMING_THRESHOLD = 10

    /**
     * Clusters hashed photos into groups with Hamming distance <= [maxHammingDistance].
     * Discards groups with fewer than 2 items.
     * Determines best shot based on highest resolution (width * height), then newest capture date.
     */
    fun clusterSimilarPhotos(
        hashedPhotos: List<Pair<MediaItem, Long>>,
        maxHammingDistance: Int = DEFAULT_HAMMING_THRESHOLD
    ): List<SimilarPhotoGroup> {
        val visited = BooleanArray(hashedPhotos.size)
        val groups = mutableListOf<SimilarPhotoGroup>()

        for (i in hashedPhotos.indices) {
            if (visited[i]) continue

            val cluster = mutableListOf<MediaItem>()
            cluster.add(hashedPhotos[i].first)
            visited[i] = true

            val baseHash = hashedPhotos[i].second

            for (j in i + 1 until hashedPhotos.size) {
                if (visited[j]) continue
                val targetHash = hashedPhotos[j].second
                val dist = ImageHasher.hammingDistance(baseHash, targetHash)
                if (dist <= maxHammingDistance) {
                    visited[j] = true
                    cluster.add(hashedPhotos[j].first)
                }
            }

            if (cluster.size >= 2) {
                val bestShot = determineBestShot(cluster)
                groups.add(
                    SimilarPhotoGroup(
                        groupId = UUID.randomUUID().toString(),
                        bestShotId = bestShot.id,
                        items = cluster
                    )
                )
            }
        }

        return groups
    }

    /**
     * Pure selection of best shot:
     * 1. Highest resolution (width * height)
     * 2. If equal, newest capture date (dateModifiedMs)
     */
    fun determineBestShot(items: List<MediaItem>): MediaItem {
        require(items.isNotEmpty()) { "Cannot determine best shot from empty list" }
        return items.maxWithOrNull(
            compareBy<MediaItem> { it.pixelCount }
                .thenBy { it.dateModifiedMs }
                .thenBy { it.id }
        ) ?: items.first()
    }
}
