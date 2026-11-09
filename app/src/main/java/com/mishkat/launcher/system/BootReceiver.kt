package com.mishkat.launcher.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mishkat.launcher.MainActivity
import com.mishkat.launcher.core.Logger
import com.mishkat.launcher.core.mishkatPrefs
import com.mishkat.launcher.i18n.LocaleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * §4 + R11 + §10 S4: BootReceiver.
 * Warms up the launcher process on device boot and, if enabled in settings, brings Mishkat to the front
 * so the user bypasses slow/ad-heavy stock TV screens.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val launchOnBoot = runCatching {
                    context.mishkatPrefs.data.first()[LocaleRepository.KEY_LAUNCH_ON_BOOT] ?: true
                }.getOrDefault(true)

                Logger.system { "BootReceiver triggered (action=$action, launchOnBoot=$launchOnBoot)" }
                if (launchOnBoot) {
                    runCatching {
                        val homeIntent = Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        }
                        context.startActivity(homeIntent)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
