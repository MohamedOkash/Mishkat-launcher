package com.mishkat.launcher.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SystemStatsAndCleanerTest {

    @Test
    fun `parseMemInfo extracts MemTotal and MemAvailable accurately`() {
        val sample = """
            MemTotal:        1982464 kB
            MemFree:          182400 kB
            MemAvailable:     842112 kB
            Buffers:           41200 kB
            Cached:           520000 kB
        """.trimIndent()

        val (totalBytes, availBytes) = SystemStatsReader.parseMemInfo(sample)
        assertEquals(1982464L * 1024L, totalBytes)
        assertEquals(842112L * 1024L, availBytes)
    }

    @Test
    fun `parseThermalRaw handles millidegrees and degrees and rejects invalid values`() {
        assertEquals(52.4f, SystemStatsReader.parseThermalRaw("52400\n") ?: 0f, 0.01f)
        assertEquals(48.0f, SystemStatsReader.parseThermalRaw("48") ?: 0f, 0.01f)
        assertNull(SystemStatsReader.parseThermalRaw("-273000"))
        assertNull(SystemStatsReader.parseThermalRaw("invalid"))
    }

    @Test
    fun `HonestCleaner measures real before and after bytes and frees actual files (R24)`() {
        val tempRoot = Files.createTempDirectory("mishkat_cleaner_test").toFile()
        try {
            val subDir = File(tempRoot, "sub").apply { mkdirs() }
            val f1 = File(tempRoot, "a.bin").apply { writeBytes(ByteArray(4096) { 1 }) }
            val f2 = File(subDir, "b.bin").apply { writeBytes(ByteArray(8192) { 2 }) }

            assertTrue(f1.exists() && f2.exists())

            val result = HonestCleaner.cleanDirectories(listOf(tempRoot))

            assertEquals(4096L + 8192L, result.beforeBytes)
            assertEquals(0L, result.afterBytes)
            assertEquals(4096L + 8192L, result.freedBytes)
            assertTrue(result.deletedFilesCount >= 2)
        } finally {
            tempRoot.deleteRecursively()
        }
    }
}
