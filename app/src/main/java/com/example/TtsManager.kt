package com.example

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingSpeech = mutableListOf<String>()

    init {
        val attributionContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            context.applicationContext.createAttributionContext("azan_puzzles_attribution")
        } else {
            context.applicationContext
        }
        tts = TextToSpeech(attributionContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TtsManager", "US English Language is not supported or missing data")
                } else {
                    isInitialized = true
                    tts?.setSpeechRate(0.85f)
                    for (text in pendingSpeech) {
                        speakImmediately(text)
                    }
                    pendingSpeech.clear()
                }
            } else {
                Log.e("TtsManager", "TTS Initialization failed")
            }
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            speakImmediately(text)
        } else {
            pendingSpeech.add(text)
        }
    }

    private fun speakImmediately(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AzanSpeechId")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
