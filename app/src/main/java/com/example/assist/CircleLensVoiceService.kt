package com.example.assist

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.util.Log

class CircleLensVoiceService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        // Ensure both assist data and screenshot context are requested from the Android system
        try {
            setDisabledShowContext(0)
        } catch (e: Throwable) {
            Log.w("CircleVoiceService", "setDisabledShowContext error", e)
        }
    }

    override fun onLaunchVoiceAssistFromKeyguard() {
        super.onLaunchVoiceAssistFromKeyguard()
        try {
            val args = Bundle()
            showSession(
                args,
                VoiceInteractionSession.SHOW_WITH_ASSIST or VoiceInteractionSession.SHOW_WITH_SCREENSHOT
            )
        } catch (e: Throwable) {
            Log.e("CircleVoiceService", "Error showing session from keyguard", e)
        }
    }
}
