package com.example.assist

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
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

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        super.onHandleScreenshot(screenshot)
        Log.d("VoiceAssistSession", "onHandleScreenshot received: ${screenshot?.width}x${screenshot?.height}")
        if (screenshot != null) {
            CircleLensScreenshotHolder.setScreenshot(screenshot)
        }
        launchApp()
    }

    override fun onHandleAssist(data: Bundle?, structure: AssistStructure?, content: AssistContent?) {
        super.onHandleAssist(data, structure, content)
        Log.d("VoiceAssistSession", "onHandleAssist triggered")
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        // If screenshot is delayed or disabled in system settings, still open the overlay
        launchApp()
    }

    private fun launchApp() {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_ASSIST
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("from_voice_assist", true)
        }
        context.startActivity(intent)
        hide()
    }
}

