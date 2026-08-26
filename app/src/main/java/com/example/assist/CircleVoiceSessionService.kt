package com.example.assist

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d("VoiceAssistSession", "onShow triggered, showFlags: $showFlags")

        // Wait up to 600ms for system to dispatch onHandleScreenshot.
        // If not received (e.g. secure screen or screenshot disabled by user), launch app fallback.
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            launchApp()
        }, 600)
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        super.onHandleScreenshot(screenshot)
        Log.d("VoiceAssistSession", "onHandleScreenshot received: ${screenshot?.width}x${screenshot?.height}")
        handler.removeCallbacksAndMessages(null)
        if (screenshot != null) {
            CircleLensScreenshotHolder.setScreenshot(screenshot)
        }
        launchApp()
    }

    override fun onHandleAssist(data: Bundle?, structure: AssistStructure?, content: AssistContent?) {
        super.onHandleAssist(data, structure, content)
        Log.d("VoiceAssistSession", "onHandleAssist triggered")
    }

    private fun launchApp() {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_ASSIST
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
            putExtra("from_voice_assist", true)
            putExtra("assist_launch_time", System.currentTimeMillis())
        }
        try {
            context.startActivity(intent)
        } catch (e: Throwable) {
            Log.e("VoiceAssistSession", "Failed to launch MainActivity", e)
        }
        handler.postDelayed({
            try {
                hide()
            } catch (e: Throwable) {
                Log.w("VoiceAssistSession", "Error hiding session", e)
            }
        }, 100)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
