package com.example.assist

import android.content.Intent
import android.speech.RecognitionService

/**
 * Recognition service stub to satisfy Android OS Digital Assistant / VoiceInteractionService
 * requirements on OEM devices (Samsung, Xiaomi, Pixel, etc.) so CircleLens is recognized
 * in Android Settings -> Default apps -> Digital assistant app.
 */
class CircleRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        // Assistant OCR operates visually via screen capture
    }

    override fun onCancel(listener: Callback?) {
    }

    override fun onStopListening(listener: Callback?) {
    }
}
