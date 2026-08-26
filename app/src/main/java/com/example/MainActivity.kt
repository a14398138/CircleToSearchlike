package com.example

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ui.CircleLensScreen
import com.example.ui.CircleLensViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.util.CircleLensScreenshotHolder
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: CircleLensViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.closeAppEvent.collect {
                    finish()
                }
            }
        }

        setContent {
            MyApplicationTheme(darkTheme = true) {
                CircleLensScreen(
                    viewModel = viewModel,
                    onCloseApp = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val captured = CircleLensScreenshotHolder.consumeScreenshot()
        if (captured != null) {
            viewModel.loadBitmapDirect(captured, "画面をキャプチャしました")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        val isAssist = intent.getBooleanExtra("from_voice_assist", false) ||
                intent.action == Intent.ACTION_ASSIST

        // 1. Check Voice Assistant / ScreenshotHolder
        val captured = CircleLensScreenshotHolder.consumeScreenshot()
        if (captured != null) {
            viewModel.loadBitmapDirect(captured, "画面をキャプチャしました")
            return
        }

        // 2. Check direct screenshot parcelable in Intent extras
        val screenshotBitmap = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("android.intent.extra.ASSIST_SCREENSHOT", Bitmap::class.java)
                    ?: intent.getParcelableExtra("screenshot", Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                (intent.getParcelableExtra<Parcelable>("android.intent.extra.ASSIST_SCREENSHOT") as? Bitmap)
                    ?: (intent.getParcelableExtra<Parcelable>("screenshot") as? Bitmap)
            }
        } catch (e: Throwable) {
            null
        }

        if (screenshotBitmap != null) {
            viewModel.loadBitmapDirect(screenshotBitmap, "画面キャプチャを読み込みました")
            return
        }

        // 3. Check shared image URI
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                    }
                    if (imageUri != null) {
                        viewModel.loadBitmapFromUri(imageUri)
                    }
                }
            }
            Intent.ACTION_ASSIST -> {
                viewModel.onAssistLaunched()
            }
            else -> {
                if (isAssist) {
                    viewModel.onAssistLaunched()
                } else if (intent.action == Intent.ACTION_MAIN && intent.categories?.contains(Intent.CATEGORY_LAUNCHER) == true) {
                    // Opened directly by tapping the app launcher icon -> show setup wizard
                    viewModel.showAssistHelp(true)
                }
            }
        }
    }
}
