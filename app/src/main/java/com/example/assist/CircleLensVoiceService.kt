package com.example.assist

import android.service.voice.VoiceInteractionService

class CircleLensVoiceService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        // Ensure both assist data and screenshot context are requested from the Android system
        setDisabledShowContext(0)
    }
}
