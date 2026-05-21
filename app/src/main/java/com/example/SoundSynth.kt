package com.example

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class SoundSynth {
    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    var isAppInForeground: Boolean = true
        private set

    fun setAppInForeground(value: Boolean) {
        isAppInForeground = value
    }

    /**
     * Plays a synthesized tone with a given frequency (Hz) and duration (ms).
     * Supports pitch sweep (from frequency to frequencyEnd) and dynamic volume control.
     * Integrates professional attack-decay pluck envelope for a soft, acoustic, physical feel.
     */
    suspend fun playTone(
        frequency: Double,
        durationMs: Int,
        type: WaveType = WaveType.SINE,
        frequencyEnd: Double = frequency,
        volume: Float = 0.4f
    ) = kotlinx.coroutines.withContext(Dispatchers.Default) {
        if (!isAppInForeground) {
            return@withContext
        }
        var audioTrack: AudioTrack? = null
        try {
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val samples = FloatArray(numSamples)
            var phase = 0.0
            
            for (i in 0 until numSamples) {
                val fraction = i.toDouble() / numSamples
                val currentFreq = frequency + (frequencyEnd - frequency) * fraction
                
                phase += currentFreq / sampleRate
                if (phase > 1.0) {
                    phase -= 1.0
                }
                
                val waveVal = when (type) {
                    WaveType.SINE -> sin(2.0 * Math.PI * phase).toFloat()
                    WaveType.TRIANGLE -> {
                        if (phase < 0.25) {
                            (phase * 4.0).toFloat()
                        } else if (phase < 0.75) {
                            (2.0 - phase * 4.0).toFloat()
                        } else {
                            (phase * 4.0 - 4.0).toFloat()
                        }
                    }
                }
                
                // Professional organic plucked string attack-decay envelope
                val attackTime = 0.05
                val envelope = if (fraction < attackTime) {
                    fraction / attackTime
                } else {
                    (1.0 - fraction) / (1.0 - attackTime)
                }
                
                samples[i] = (waveVal * envelope * volume).toFloat()
            }

            val bufferSize = numSamples * 4
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, numSamples, AudioTrack.WRITE_BLOCKING)
            audioTrack.play()
            
            kotlinx.coroutines.delay(durationMs.toLong() + 50)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                audioTrack?.stop()
            } catch (e: Exception) {}
            try {
                audioTrack?.release()
            } catch (e: Exception) {}
        }
    }

    enum class WaveType { SINE, TRIANGLE }

    fun playSlideSound() {
        scope.launch {
            playTone(frequency = 220.0, durationMs = 85, type = WaveType.TRIANGLE, frequencyEnd = 130.0, volume = 0.35f)
        }
    }

    fun playWinFanfare() {
        scope.launch {
            if (!isAppInForeground) return@launch
            // Play a gorgeous major 9th polyphonic chime chord (simultaneous overlapping tones)
            launch { playTone(frequency = 523.25, durationMs = 450, type = WaveType.SINE, volume = 0.30f) } // C5
            kotlinx.coroutines.delay(70)
            launch { playTone(frequency = 659.25, durationMs = 450, type = WaveType.SINE, volume = 0.30f) } // E5
            kotlinx.coroutines.delay(70)
            launch { playTone(frequency = 783.99, durationMs = 500, type = WaveType.SINE, volume = 0.30f) } // G5
            kotlinx.coroutines.delay(70)
            launch { playTone(frequency = 987.77, durationMs = 550, type = WaveType.SINE, volume = 0.25f) } // B5
            kotlinx.coroutines.delay(70)
            launch { playTone(frequency = 1046.50, durationMs = 700, type = WaveType.TRIANGLE, volume = 0.35f) } // C6
        }
    }
}
