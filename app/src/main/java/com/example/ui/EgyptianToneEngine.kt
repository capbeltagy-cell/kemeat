package com.example.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.sin

object EgyptianToneEngine {
    private const val SAMPLE_RATE = 22050
    
    @Volatile
    var soundEnabledFlag = true
    
    @Volatile
    var musicEnabledFlag = true

    @Volatile
    private var isPlayingAmbient = false
    private var bgmThread: Thread? = null

    /**
     * Synthesizes and plays a dynamic synth tone with custom frequency and wave type.
     */
    fun playTone(frequency: Double, durationMs: Int, type: String = "sine", volume: Float = 0.5f) {
        if (!soundEnabledFlag) return
        
        Thread {
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            if (numSamples <= 0) return@Thread
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val angle = 2.0 * Math.PI * frequency * t
                val value = when (type) {
                    "saw" -> ((t * frequency) % 1.0 * 2.0 - 1.0)
                    "triangle" -> {
                        val x = (t * frequency) % 1.0
                        if (x < 0.5) 4.0 * x - 1.0 else 3.0 - 4.0 * x
                    }
                    else -> sin(angle) // sine wave
                }
                // Apply subtle envelope fade-out to prevent popping noises at the tail end
                val fade = if (i > numSamples - 1200) (numSamples - i) / 1200.0 else 1.0
                samples[i] = (value * Short.MAX_VALUE * volume * fade).toInt().toShort()
            }

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
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()

                // Wait until done, then release resources
                Thread.sleep(durationMs.toLong() + 50L)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                Log.e("EgyptianToneEngine", "PCM tone generation failed: ${e.message}")
            }
        }.start()
    }

    fun playCoinSound() {
        playTone(987.77, 70, "sine", 0.4f) // B5
        try { Thread.sleep(60) } catch (e: Exception) {}
        playTone(1318.51, 140, "sine", 0.4f) // E6
    }

    fun playGemSound() {
        playTone(1046.50, 60, "triangle", 0.35f) // C6
        try { Thread.sleep(50) } catch (e: Exception) {}
        playTone(1567.98, 160, "triangle", 0.35f) // G6
    }

    fun playComboSound(combo: Int) {
        val mult = (combo % 12).toDouble()
        val freq = 440.0 * Math.pow(1.059463, mult + 3.0) // phrygian scale step
        playTone(freq, 110, "sine", 0.4f)
        playTone(freq * 1.5, 130, "triangle", 0.15f)
    }

    fun playChestSound() {
        // Multi-pitch rolling chest opening creak
        for (i in 0 until 4) {
            playTone(140.0 + (i * 70.0), 60, "saw", 0.25f)
            try { Thread.sleep(50) } catch (e: Exception) {}
        }
        playTone(880.0, 220, "triangle", 0.4f)
    }

    fun playCursedStoneSound() {
        playTone(220.0, 150, "saw", 0.45f)
        try { Thread.sleep(100) } catch (e: Exception) {}
        playTone(146.83, 300, "saw", 0.45f)
    }

    fun playVictorySound() {
        val freqs = listOf(523.25, 659.25, 783.99, 1046.50) // C Major chord scale
        for (f in freqs) {
            playTone(f, 160, "triangle", 0.4f)
            try { Thread.sleep(120) } catch (e: Exception) {}
        }
    }

    fun playBossIntroSound() {
        playTone(98.0, 400, "saw", 0.6f)
        try { Thread.sleep(250) } catch (e: Exception) {}
        playTone(92.5, 400, "saw", 0.6f)
    }

    fun playBossHitSound() {
        playTone(110.0, 110, "saw", 0.55f)
    }

    /**
     * Spawns an async loop thread recreating pharaonic Egyptian ambient tunes.
     */
    fun startBgm() {
        if (isPlayingAmbient) return
        isPlayingAmbient = true

        bgmThread = Thread {
            // Harmonic Phrygian sequence of mystical desert tones keys (A - Bb - C# - D - E)
            val melody = listOf(220.0, 233.08, 277.18, 293.66, 329.63, 293.66, 277.18, 233.08)
            var index = 0
            while (isPlayingAmbient) {
                if (musicEnabledFlag) {
                    val targetFreq = melody[index]
                    playTone(targetFreq, 420, "sine", 0.18f)
                    index = (index + 1) % melody.size
                }
                try {
                    Thread.sleep(600)
                } catch (e: Exception) {
                    break
                }
            }
        }
        bgmThread?.start()
    }

    /**
     * Stops the looping background ambient playback thread.
     */
    fun stopBgm() {
        isPlayingAmbient = false
        bgmThread?.interrupt()
        bgmThread = null
    }
}
