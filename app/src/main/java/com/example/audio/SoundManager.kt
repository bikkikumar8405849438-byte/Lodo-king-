package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.PI

/**
 * Procedural audio synthesizer for zero-dependency, ultra-low-latency game sound effects.
 */
class SoundManager {
    var isMuted: Boolean = false

    private val audioScope = CoroutineScope(Dispatchers.Default)

    fun playDiceRoll() {
        if (isMuted) return
        audioScope.launch {
            // Rapid rattle / click sequence mimicking dice rolling on wooden board
            val sampleRate = 22050
            val durationMs = 320
            val numSamples = sampleRate * durationMs / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // Periodic clicks decaying over time
                val envelope = 1.0 - (i.toDouble() / numSamples)
                val clickFrequency = 35.0 + 15.0 * sin(t * 40.0)
                val clickPhase = (t * clickFrequency) % 1.0
                val clickVal = if (clickPhase < 0.15) 1.0 else -0.3
                val body = sin(2.0 * PI * 220.0 * t) * 0.4
                val sample = (clickVal * 0.6 + body) * envelope
                buffer[i] = (sample * 16000).toInt().coerceIn(-32767, 32767).toShort()
            }
            playPcm(buffer, sampleRate)
        }
    }

    fun playTokenMove() {
        if (isMuted) return
        audioScope.launch {
            // Cheerful wood-block 'pop'
            val sampleRate = 22050
            val durationMs = 80
            val numSamples = sampleRate * durationMs / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = (1.0 - (i.toDouble() / numSamples))
                // Pitch drop: 600Hz down to 350Hz
                val freq = 600.0 - 250.0 * (i.toDouble() / numSamples)
                val sample = sin(2.0 * PI * freq * t) * envelope
                buffer[i] = (sample * 20000).toInt().coerceIn(-32767, 32767).toShort()
            }
            playPcm(buffer, sampleRate)
        }
    }

    fun playCapture() {
        if (isMuted) return
        audioScope.launch {
            // Heavy impact and descending slide
            val sampleRate = 22050
            val durationMs = 350
            val numSamples = sampleRate * durationMs / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                val envelope = (1.0 - progress) * (1.0 - progress)
                val freq = 450.0 - 300.0 * progress
                val noise = (Math.random() * 2.0 - 1.0) * if (progress < 0.2) 0.5 else 0.05
                val sample = (sin(2.0 * PI * freq * t) * 0.7 + noise) * envelope
                buffer[i] = (sample * 24000).toInt().coerceIn(-32767, 32767).toShort()
            }
            playPcm(buffer, sampleRate)
        }
    }

    fun playSixRolled() {
        if (isMuted) return
        audioScope.launch {
            // Bright triumphant two-tone chime
            val sampleRate = 22050
            val durationMs = 280
            val numSamples = sampleRate * durationMs / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                val freq = if (progress < 0.45) 587.33 else 880.0 // D5 -> A5
                val env = 1.0 - ((progress * 2.0) % 1.0) * 0.7
                val sample = sin(2.0 * PI * freq * t) * env
                buffer[i] = (sample * 18000).toInt().coerceIn(-32767, 32767).toShort()
            }
            playPcm(buffer, sampleRate)
        }
    }

    fun playTokenHome() {
        if (isMuted) return
        audioScope.launch {
            // Major triad fanfare (C5, E5, G5, C6)
            val sampleRate = 22050
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
            val noteDurationMs = 90
            val totalDurationMs = notes.size * noteDurationMs
            val numSamples = sampleRate * totalDurationMs / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val noteIdx = (i * notes.size / numSamples).coerceIn(0, notes.size - 1)
                val noteProgress = (i % (numSamples / notes.size)).toDouble() / (numSamples / notes.size)
                val env = (1.0 - noteProgress)
                val freq = notes[noteIdx]
                val sample = (sin(2.0 * PI * freq * t) + 0.3 * sin(4.0 * PI * freq * t)) * env
                buffer[i] = (sample * 16000).toInt().coerceIn(-32767, 32767).toShort()
            }
            playPcm(buffer, sampleRate)
        }
    }

    fun playVictory() {
        if (isMuted) return
        audioScope.launch {
            // Grand victory fanfare
            val sampleRate = 22050
            val notes = doubleArrayOf(523.25, 523.25, 523.25, 659.25, 783.99, 1046.50)
            val noteDurations = intArrayOf(120, 120, 120, 200, 200, 500)
            val totalSamples = noteDurations.sum() * sampleRate / 1000
            val buffer = ShortArray(totalSamples)

            var sampleCursor = 0
            for (n in notes.indices) {
                val freq = notes[n]
                val noteSamples = noteDurations[n] * sampleRate / 1000
                for (s in 0 until noteSamples) {
                    if (sampleCursor >= totalSamples) break
                    val t = s.toDouble() / sampleRate
                    val progress = s.toDouble() / noteSamples
                    val env = (1.0 - progress * 0.8)
                    val sample = (sin(2.0 * PI * freq * t) + 0.25 * sin(4.0 * PI * freq * t)) * env
                    buffer[sampleCursor++] = (sample * 18000).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            playPcm(buffer, sampleRate)
        }
    }

    fun playButtonClick() {
        if (isMuted) return
        audioScope.launch {
            val sampleRate = 22050
            val durationMs = 40
            val numSamples = sampleRate * durationMs / 1000
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val env = 1.0 - (i.toDouble() / numSamples)
                val sample = sin(2.0 * PI * 800.0 * t) * env
                buffer[i] = (sample * 14000).toInt().coerceIn(-32767, 32767).toShort()
            }
            playPcm(buffer, sampleRate)
        }
    }

    private fun playPcm(buffer: ShortArray, sampleRate: Int) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            // Track will release naturally after completion
            Thread.sleep((buffer.size.toLong() * 1000 / sampleRate) + 50)
            audioTrack.release()
        } catch (_: Exception) {
            // Graceful fallback if device audio is occupied
        }
    }
}
