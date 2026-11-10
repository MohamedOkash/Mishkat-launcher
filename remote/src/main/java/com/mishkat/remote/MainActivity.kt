package com.mishkat.remote

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mishkat.remote.ui.RemoteScreen
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var recognizedVoiceText by mutableStateOf<String?>(null)

    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                recognizedVoiceText = spokenText
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val discoveryClient = RemoteApp.instance.discoveryClient
            val currentTv by discoveryClient.targetTv.collectAsState()
            val isSearching by discoveryClient.isSearching.collectAsState()

            RemoteScreen(
                currentTv = currentTv,
                isSearching = isSearching,
                onSaveManualIp = { ip -> discoveryClient.setManualIp(ip) },
                onVibrate = { triggerHapticFeedback() },
                onLaunchVoiceInput = { launchVoiceRecognizer() },
                recognizedVoiceText = recognizedVoiceText,
                onClearVoiceText = { recognizedVoiceText = null }
            )
        }
    }

    private fun triggerHapticFeedback() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(25L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(25L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(25L)
                }
            }
        }
    }

    private fun launchVoiceRecognizer() {
        runCatching {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث للبحث أو الكتابة على التلفزيون...")
            }
            voiceLauncher.launch(intent)
        }
    }
}
