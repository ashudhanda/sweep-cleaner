package com.sweep.cleaner

import android.net.Uri
import com.sweep.cleaner.model.SafDocumentItem
import com.sweep.cleaner.model.SafDuplicateGroup
import com.sweep.cleaner.model.SafScanResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SafScannerTest {

    @Test
    fun testSafDuplicateGroupReclaimableBytes() {
        val dummyUri1 = Uri.parse("content://dummy/1")
        val dummyUri2 = Uri.parse("content://dummy/2")
        val dummyUri3 = Uri.parse("content://dummy/3")

        val item1 = SafDocumentItem(
            uri = dummyUri1,
            documentId = "1",
            treeUri = Uri.parse("content://dummy/tree"),
            displayName = "file_copy1.mp4",
            sizeBytes = 100_000_000L,
            mimeType = "video/mp4",
            lastModifiedMs = 1000L,
            relativePath = "Downloads/file_copy1.mp4"
        )
        val item2 = SafDocumentItem(
            uri = dummyUri2,
            documentId = "2",
            treeUri = Uri.parse("content://dummy/tree"),
            displayName = "file_copy2.mp4",
            sizeBytes = 100_000_000L,
            mimeType = "video/mp4",
            lastModifiedMs = 2000L,
            relativePath = "Backup/file_copy2.mp4"
        )
        val item3 = SafDocumentItem(
            uri = dummyUri3,
            documentId = "3",
            treeUri = Uri.parse("content://dummy/tree"),
            displayName = "file_copy3.mp4",
            sizeBytes = 100_000_000L,
            mimeType = "video/mp4",
            lastModifiedMs = 3000L,
            relativePath = "Media/file_copy3.mp4"
        )

        val group = SafDuplicateGroup(
            groupId = "test_group",
            fileSize = 100_000_000L,
            items = listOf(item1, item2, item3),
            recommendedKeepItem = item1
        )

        assertEquals(3, group.totalDuplicateCount)
        // 3 items of 100MB each -> keeping 1 means 200MB reclaimable
        assertEquals(200_000_000L, group.reclaimableBytes)
    }

    @Test
    fun testSafScanResultCalculations() {
        val dummyTreeUri = Uri.parse("content://dummy/tree")
        val largeFile1 = SafDocumentItem(
            uri = Uri.parse("content://dummy/large1"),
            documentId = "large1",
            treeUri = dummyTreeUri,
            displayName = "large_iso.iso",
            sizeBytes = 500_000_000L,
            mimeType = "application/octet-stream",
            lastModifiedMs = 1000L,
            relativePath = "large_iso.iso"
        )

        val dupItem1 = SafDocumentItem(
            uri = Uri.parse("content://dummy/dup1"),
            documentId = "dup1",
            treeUri = dummyTreeUri,
            displayName = "song.mp3",
            sizeBytes = 10_000_000L,
            mimeType = "audio/mpeg",
            lastModifiedMs = 1000L,
            relativePath = "Music/song.mp3"
        )
        val dupItem2 = SafDocumentItem(
            uri = Uri.parse("content://dummy/dup2"),
            documentId = "dup2",
            treeUri = dummyTreeUri,
            displayName = "song_copy.mp3",
            sizeBytes = 10_000_000L,
            mimeType = "audio/mpeg",
            lastModifiedMs = 2000L,
            relativePath = "Downloads/song_copy.mp3"
        )

        val dupGroup = SafDuplicateGroup(
            groupId = "song_group",
            fileSize = 10_000_000L,
            items = listOf(dupItem1, dupItem2),
            recommendedKeepItem = dupItem1
        )

        val result = SafScanResult(
            treeUri = dummyTreeUri,
            rootDisplayName = "Downloads",
            totalFilesScanned = 100,
            totalDirectoriesScanned = 10,
            totalBytesScanned = 600_000_000L,
            scanDurationMs = 250L,
            largeFiles = listOf(largeFile1),
            duplicateGroups = listOf(dupGroup)
        )

        assertEquals(500_000_000L, result.totalLargeFilesBytes)
        assertEquals(10_000_000L, result.totalReclaimableDuplicateBytes)
        assertEquals(510_000_000L, result.totalPotentialCleanBytes)
    }
}
