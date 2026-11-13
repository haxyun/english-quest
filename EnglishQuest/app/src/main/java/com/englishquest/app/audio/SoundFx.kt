package com.englishquest.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Petits effets sonores synthétisés à la volée (aucun fichier audio nécessaire).
 */
object SoundFx {
    @Volatile
    var enabled = true

    private const val SAMPLE_RATE = 44100
    private val executor = Executors.newSingleThreadExecutor()

    private data class Note(val freq: Double, val ms: Int, val volume: Double = 0.5)

    /** Bonne réponse : deux notes qui montent, de plus en plus aigu avec le combo. */
    fun correct(combo: Int) {
        val shift = Math.pow(2.0, min(combo, 10) / 12.0)
        play(listOf(Note(784.0 * shift, 70), Note(1175.0 * shift, 140)))
    }

    fun wrong() = play(listOf(Note(220.0, 110, 0.45), Note(185.0, 190, 0.45)))

    fun softWrong() = play(listOf(Note(260.0, 90, 0.35)))

    fun tap() = play(listOf(Note(1046.0, 45, 0.3)))

    fun combo() = play(listOf(Note(784.0, 70), Note(988.0, 70), Note(1175.0, 70), Note(1568.0, 200)))

    fun hit() = play(listOf(Note(140.0, 60, 0.6), Note(98.0, 110, 0.5)))

    fun victory() = play(
        listOf(Note(523.0, 110), Note(659.0, 110), Note(784.0, 110), Note(1046.0, 160), Note(784.0, 90), Note(1046.0, 320))
    )

    fun defeat() = play(listOf(Note(392.0, 180, 0.4), Note(330.0, 180, 0.4), Note(262.0, 380, 0.4)))

    fun chest() = play(listOf(Note(659.0, 60), Note(880.0, 60), Note(1175.0, 60), Note(1760.0, 260, 0.4)))

    fun levelUp() = play(listOf(Note(523.0, 90), Note(784.0, 90), Note(1046.0, 90), Note(1318.0, 90), Note(1568.0, 340)))

    private fun play(notes: List<Note>) {
        if (!enabled) return
        executor.execute {
            try {
                val pcm = synth(notes)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(pcm.size * 1000L / SAMPLE_RATE + 60L)
                track.release()
            } catch (e: Exception) {
                // Pas de son disponible : on ignore.
            }
        }
    }

    private fun synth(notes: List<Note>): ShortArray {
        val total = notes.sumOf { it.ms } * SAMPLE_RATE / 1000
        val out = ShortArray(total)
        var idx = 0
        for (n in notes) {
            val len = n.ms * SAMPLE_RATE / 1000
            val attack = SAMPLE_RATE * 5 / 1000
            for (i in 0 until len) {
                if (idx >= out.size) break
                val t = i.toDouble() / SAMPLE_RATE
                val env = (if (i < attack) i.toDouble() / attack else 1.0) * exp(-3.5 * i / len)
                val wave = sin(2 * PI * n.freq * t) * 0.75 + sin(2 * PI * n.freq * 2 * t) * 0.18 + sin(2 * PI * n.freq * 3 * t) * 0.07
                out[idx++] = (wave * env * n.volume * Short.MAX_VALUE * 0.6).toInt().toShort()
            }
        }
        return out
    }
}
