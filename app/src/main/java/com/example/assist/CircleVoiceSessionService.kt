package com.example.assist

import android.app.ActivityOptions
import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.util.Log
import com.example.MainActivity
import com.example.util.CircleLensScreenshotHolder

class CircleVoiceSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return CircleVoiceInteractionSession(this)
    }
}

class CircleVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    private val handler = Handler(Looper.getMainLooper())
    private var hasHandledCurrentSession = false

    override fun onCreate() {
        super.onCreate()
        try {
            // CircleLens delegates overlay UI to MainActivity
            setUiEnabled(false)
        } catch (e: Throwable) {
            Log.w("VoiceAssistSession", "Could not setUiEnabled(false)", e)
        }
    }

    override fun onPrepareShow(args: Bundle?, showFlags: Int) {
        super.onPrepareShow(args, showFlags)
        hasHandledCurrentSession = false
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d("VoiceAssistSession", "onShow triggered, showFlags: $showFlags")

        // Safety fallback: If OS doesn't deliver onHandleScreenshot within 350ms
        // (e.g. secure screen, DRM, or slow OS hook), launch MainActivity anyway
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (!hasHandledCurrentSession) {
                Log.d("VoiceAssistSession", "Fallback timer triggered launch")
                launchAssistantActivity()
            }
        }, 350)
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        super.onHandleScreenshot(screenshot)
        Log.d("VoiceAssistSession", "onHandleScreenshot received: ${screenshot?.width}x${screenshot?.height}")
        handler.removeCallbacksAndMessages(null)
        if (screenshot != null) {
            CircleLensScreenshotHolder.setScreenshot(screenshot)
        }
        launchAssistantActivity()
    }

    override fun onHandleAssist(data: Bundle?, structure: AssistStructure?, content: AssistContent?) {
        super.onHandleAssist(data, structure, content)
        Log.d("VoiceAssistSession", "onHandleAssist triggered")
        if (!hasHandledCurrentSession) {
            handler.removeCallbacksAndMessages(null)
            launchAssistantActivity()
        }
    }

    private fun launchAssistantActivity() {
        hasHandledCurrentSession = true
        handler.removeCallbacksAndMessages(null)

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_ASSIST
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            putExtra("from_voice_assist", true)
            putExtra("assist_launch_time", System.currentTimeMillis())
        }

        var launchedSuccessfully = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                startAssistantActivity(intent)
                launchedSuccessfully = true
            } catch (e: Throwable) {
                Log.w("VoiceAssistSession", "startAssistantActivity failed, falling back to context.startActivity", e)
            }
        }

        if (!launchedSuccessfully) {
            try {
                val options = ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle()
                context.startActivity(intent, options)
                launchedSuccessfully = true
            } catch (e: Throwable) {
                Log.e("VoiceAssistSession", "Failed to launch MainActivity", e)
            }
        }

        // Gracefully finish session after activity launch without abruptly aborting window transition
        try {
            finish()
        } catch (e: Throwable) {
            try {
                hide()
            } catch (_: Throwable) {}
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
