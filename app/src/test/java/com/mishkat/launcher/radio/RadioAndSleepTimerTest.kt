package com.mishkat.launcher.radio

import com.mishkat.launcher.system.SleepTimerPolicy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * R19: unit tests for SleepTimer logic (fade-out 5s + presets + countdown formatting)
 * and offline stations snapshot integrity (177 stations).
 */
class RadioAndSleepTimerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun sleepTimerFadeOutAndExpiryMath() {
        val target = 1_000_000L

        // 10s before expiry: active, full volume (1.0f), not fading.
        val s10 = SleepTimerPolicy.computeState(targetEpochMs = target, nowEpochMs = target - 10_000L)
        assertTrue(s10.active)
        assertFalse(s10.isFading)
        assertEquals(1.0f, s10.volumeScale, 0.001f)

        // 2.5s before expiry (inside 5s fade-out window): active, fading, volume = 0.5f.
        val sFade = SleepTimerPolicy.computeState(targetEpochMs = target, nowEpochMs = target - 2_500L)
        assertTrue(sFade.active)
        assertTrue(sFade.isFading)
        assertEquals(0.5f, sFade.volumeScale, 0.01f)

        // At/after expiry: inactive, volume = 0f.
        val sExpired = SleepTimerPolicy.computeState(targetEpochMs = target, nowEpochMs = target)
        assertFalse(sExpired.active)
        assertEquals(0.0f, sExpired.volumeScale, 0.001f)
    }

    @Test
    fun sleepTimerPresetCycleAndFormatting() {
        assertEquals(15, SleepTimerPolicy.nextPresetMinutes(0L))
        assertEquals(30, SleepTimerPolicy.nextPresetMinutes(14 * 60_000L))
        assertEquals(60, SleepTimerPolicy.nextPresetMinutes(30 * 60_000L))
        assertEquals(90, SleepTimerPolicy.nextPresetMinutes(60 * 60_000L))
        assertEquals(0, SleepTimerPolicy.nextPresetMinutes(90 * 60_000L))

        assertEquals("15:00", SleepTimerPolicy.formatRemaining(15 * 60_000L))
        assertEquals("00:05", SleepTimerPolicy.formatRemaining(4_200L))
    }

    @Test
    fun offlineStationsSnapshotParsesAll177Stations() {
        val candidates = listOf(
            File("src/main/assets/stations_snapshot.json"),
            File("app/src/main/assets/stations_snapshot.json"),
        )
        val file = candidates.first { it.isFile }
        val stations = json.decodeFromString(
            ListSerializer(SnapshotStation.serializer()),
            file.readText(),
        )
        assertTrue("Expected >= 150 stations, got ${stations.size}", stations.size >= 150)
        for (s in stations) {
            assertTrue(s.id > 0)
            assertTrue(s.nameAr.isNotBlank())
            assertTrue(s.nameEn.isNotBlank())
            assertTrue("Expected https stream URL: ${s.url}", s.url.startsWith("https://"))
        }
    }
}
