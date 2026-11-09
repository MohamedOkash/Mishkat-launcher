package com.mishkat.launcher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import com.mishkat.launcher.focus.LocalFocusRestorer
import com.mishkat.launcher.home.HomeScreen
import com.mishkat.launcher.home.SafeModeScreen
import com.mishkat.launcher.i18n.LocalAppSettings
import com.mishkat.launcher.i18n.LocaleFormatter
import com.mishkat.launcher.input.RemoteUnificationLayer
import com.mishkat.launcher.theme.LocalThemeTokens

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = (application as MishkatApp).graph

        // R25: pressing Back on the home launcher should not exit/finish the activity.
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            },
        )

        val initialSafeMode = graph.crashGuard.isCrashLooping()

        setContent {
            val baseTokens by graph.themeRepository.tokens.collectAsState()
            val appSettings by graph.localeRepository.settings.collectAsState()
            val tokens = remember(baseTokens, appSettings.isLightMode) {
                baseTokens.withLightMode(appSettings.isLightMode)
            }
            var inSafeMode by remember { mutableStateOf(initialSafeMode) }

            val baseContext = LocalContext.current
            val baseDensity = LocalDensity.current
            val customDensity = remember(baseDensity, appSettings.fontScale) {
                Density(
                    density = baseDensity.density,
                    fontScale = (baseDensity.fontScale * appSettings.fontScale).coerceIn(0.85f, 1.30f),
                )
            }
            val localizedContext = remember(baseContext, appSettings.language) {
                LocaleFormatter.localizedContext(baseContext, appSettings.locale)
            }
            val localizedConfig = remember(localizedContext) {
                localizedContext.resources.configuration
            }

            // R21: instant in-app locale + RTL/LTR + Light/Dark Mode + Font Scale switch without restarting Activity/Device.
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfig,
                LocalLayoutDirection provides appSettings.layoutDirection,
                LocalDensity provides customDensity,
                LocalAppSettings provides appSettings,
                LocalThemeTokens provides tokens,
                LocalFocusRestorer provides graph.focusRestorer,
            ) {
                if (inSafeMode) {
                    SafeModeScreen(
                        tokens = tokens,
                        onRetry = {
                            graph.crashGuard.reset()
                            inSafeMode = false
                        },
                        onOpenSystemLauncherChooser = { openHomeSettings() },
                    )
                } else {
                    HomeScreen(
                        graph = graph,
                        onOpenHomeChooser = { openHomeSettings() },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        RemoteUnificationLayer.emitAction(com.mishkat.launcher.input.MishkatAction.Home)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return when (val r = RemoteUnificationLayer.dispatch(event)) {
            RemoteUnificationLayer.CONSUMED -> true
            RemoteUnificationLayer.PASS -> super.dispatchKeyEvent(event)
            else -> super.dispatchKeyEvent(KeyEvent(event.action, r))
        }
    }

    private fun openHomeSettings() {
        val opened = runCatching {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (!opened) {
            runCatching {
                startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
