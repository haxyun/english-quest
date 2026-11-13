package com.englishquest.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** Synthèse vocale anglaise via le moteur TextToSpeech du téléphone (fonctionne hors ligne). */
class Speaker(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    /** true quand une voix anglaise est disponible. */
    var ready by mutableStateOf(false)
        private set

    @Volatile
    var rate: Float = 0.9f

    private var pendingStatus: Int? = null

    init {
        val t = TextToSpeech(context.applicationContext, this)
        tts = t
        // Certains moteurs appellent onInit avant la fin du constructeur
        pendingStatus?.let { configure(t, it) }
    }

    override fun onInit(status: Int) {
        val t = tts
        if (t == null) {
            pendingStatus = status
            return
        }
        configure(t, status)
    }

    private fun configure(t: TextToSpeech, status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        var result = try { t.setLanguage(Locale.UK) } catch (e: Exception) { TextToSpeech.LANG_NOT_SUPPORTED }
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            result = try { t.setLanguage(Locale.US) } catch (e: Exception) { TextToSpeech.LANG_NOT_SUPPORTED }
        }
        ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun speak(text: String, slow: Boolean = false) {
        val t = tts ?: return
        if (!ready || text.isBlank()) return
        try {
            t.setSpeechRate(if (slow) rate * 0.6f else rate)
            t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "eq-" + System.nanoTime())
        } catch (e: Exception) {
            // On ignore : la synthèse vocale n'est jamais bloquante.
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
        }
        tts = null
    }
}
