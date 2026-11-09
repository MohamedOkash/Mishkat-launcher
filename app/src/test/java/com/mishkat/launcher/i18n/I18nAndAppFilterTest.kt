package com.mishkat.launcher.i18n

import android.content.ComponentName
import androidx.compose.ui.unit.LayoutDirection
import com.mishkat.launcher.home.AppEntry
import com.mishkat.launcher.home.AppListPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class I18nAndAppFilterTest {

    @Test
    fun localeDigitsAndDirectionFollowLanguage() {
        val ar = AppSettings(language = "ar", use24Hour = false)
        val en = AppSettings(language = "en", use24Hour = true)

        assertEquals(LayoutDirection.Rtl, ar.layoutDirection)
        assertEquals(LayoutDirection.Ltr, en.layoutDirection)
        assertEquals("٠٤:٣١", LocaleFormatter.localizeDigits("04:31", isArabic = true))
        assertEquals("04:31", LocaleFormatter.localizeDigits("04:31", isArabic = false))
    }

    @Test
    fun appListPolicyPinsFirstAndHidesBloatware() {
        val a = AppEntry("com.ads/.Main", "com.ads", "AdApp", ComponentName("com.ads", ".Main"), null, null)
        val b = AppEntry("com.yt/.Main", "com.yt", "YouTube", ComponentName("com.yt", ".Main"), null, null)
        val c = AppEntry("com.quran/.Main", "com.quran", "Balancer", ComponentName("com.quran", ".Main"), null, null)

        val (visible, all) = AppListPolicy.organize(
            raw = listOf(a, b, c),
            pinnedPackages = setOf("com.yt"),
            hiddenPackages = setOf("com.ads"),
        )

        // All contains 3; visible contains 2 with pinned "com.yt" first.
        assertEquals(3, all.size)
        assertEquals(listOf("com.yt", "com.quran"), visible.map { it.packageName })
    }
}
