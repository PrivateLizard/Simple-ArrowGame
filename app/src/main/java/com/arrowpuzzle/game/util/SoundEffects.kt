package com.arrowpuzzle.game.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.arrowpuzzle.game.game.GameFeedback
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Tiny offline sound layer. The cues are synthesised into short WAV files in the
 * app's cache directory and played through [SoundPool], which is built for
 * short, low-latency effects.
 *
 * An earlier version wrote raw PCM into a single streaming AudioTrack. That
 * produced `AudioFlinger: BUFFER TIMEOUT / underrun` and the cue was usually
 * inaudible, because a one-shot write into a stream that small gets dropped
 * before it reaches the mixer. SoundPool loads the sample once and mixes it
 * reliably.
 *
 * No audio assets are bundled and no permission is needed.
 */
class SoundEffects(private val context: Context) {

    private val lock = Any()

    /** Lazily created so the cost is only paid when a cue is played. */
    private var soundPool: SoundPool? = null
    private val sampleIds = mutableMapOf<GameFeedback.Kind, Int>()
    private var loadedIds = emptySet<Int>()

    fun play(feedback: GameFeedback) {
        ensureLoaded()
        val id = sampleIds[feedback.kind] ?: return
        if (id !in loadedIds) return
        runCatching {
            soundPool?.play(
                id,
                VOLUME,
                VOLUME,
                PRIORITY,
                /* loop = */ 0,
                /* rate = */ 1.0f,
            )
        }
    }

    fun release() {
        synchronized(lock) {
            runCatching { soundPool?.release() }
            soundPool = null
            sampleIds.clear()
            loadedIds = emptySet()
        }
    }

    private fun ensureLoaded() {
        synchronized(lock) {
            if (soundPool != null) return

            val pool = runCatching {
                SoundPool.Builder()
                    .setMaxStreams(MAX_STREAMS)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .build()
            }.getOrNull() ?: return

            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    synchronized(lock) { loadedIds = loadedIds + sampleId }
                }
            }

            soundPool = pool
            // Keep at least the music stream audible at a sensible level.
            runCatching {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.let {
                    if (it.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                        it.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            (it.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2),
                            0,
                        )
                    }
                }
            }

            GameFeedback.Kind.entries.forEach { kind ->
                val file = writeWav(kind)
                if (file != null) {
                    val id = runCatching { pool.load(file.absolutePath, 1) }.getOrDefault(0)
                    if (id != 0) sampleIds[kind] = id
                }
            }
        }
    }

    /** Writes the synthesised cue once into the cache dir and returns the file. */
    private fun writeWav(kind: GameFeedback.Kind): File? = runCatching {
        val samples = when (kind) {
            GameFeedback.Kind.CORRECT -> synthesizePlop(640.0, 300.0, 0.085)
            GameFeedback.Kind.WRONG -> synthesizeThud()
            GameFeedback.Kind.VICTORY -> concatenate(
                synthesizePlop(520.0, 620.0, 0.09),
                silence(0.03),
                synthesizePlop(700.0, 860.0, 0.09),
            )
            GameFeedback.Kind.DEFEAT -> synthesizePlop(420.0, 240.0, 0.16)
        }
        val file = File(context.cacheDir, "cue_${kind.name.lowercase()}.wav")
        file.outputStream().use { out -> out.write(toWavBytes(samples)) }
        file
    }.getOrNull()

    /** A soft "plop": falling sine with a fast attack and a mellow decay. */
    private fun synthesizePlop(start: Double, end: Double, duration: Double): ShortArray {
        val count = (SAMPLE_RATE * duration).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val frequency = start + (end - start) * progress
            phase += 2.0 * PI * frequency / SAMPLE_RATE
            val attack = (progress / 0.06).coerceAtMost(1.0)
            val decay = exp(-6.0 * progress)
            val sample = sin(phase) * attack * decay * LEVEL
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    /** Duller, lower thud for a wrong tap. */
    private fun synthesizeThud(): ShortArray {
        val duration = 0.13
        val count = (SAMPLE_RATE * duration).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val frequency = 210.0 - 90.0 * progress
            phase += 2.0 * PI * frequency / SAMPLE_RATE
            val attack = (progress / 0.05).coerceAtMost(1.0)
            val decay = exp(-8.0 * progress)
            val sample = (sin(phase) * 0.8 + sin(phase * 2.0) * 0.2) * attack * decay * LEVEL
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    private fun silence(duration: Double): ShortArray = ShortArray((SAMPLE_RATE * duration).toInt())

    private fun concatenate(vararg parts: ShortArray): ShortArray {
        val out = ShortArray(parts.sumOf { it.size })
        var offset = 0
        for (part in parts) {
            part.copyInto(out, offset)
            offset += part.size
        }
        return out
    }

    /** Minimal 16-bit mono PCM WAV container. */
    private fun toWavBytes(samples: ShortArray): ByteArray {
        val dataSize = samples.size * 2
        val out = ByteArray(44 + dataSize)
        fun putAscii(offset: Int, value: String) {
            value.forEachIndexed { i, c -> out[offset + i] = c.code.toByte() }
        }
        fun putInt(offset: Int, value: Int) {
            out[offset] = (value and 0xFF).toByte()
            out[offset + 1] = ((value shr 8) and 0xFF).toByte()
            out[offset + 2] = ((value shr 16) and 0xFF).toByte()
            out[offset + 3] = ((value shr 24) and 0xFF).toByte()
        }
        fun putShort(offset: Int, value: Int) {
            out[offset] = (value and 0xFF).toByte()
            out[offset + 1] = ((value shr 8) and 0xFF).toByte()
        }

        putAscii(0, "RIFF")
        putInt(4, 36 + dataSize)
        putAscii(8, "WAVE")
        putAscii(12, "fmt ")
        putInt(16, 16)                      // fmt chunk size
        putShort(20, 1)                     // PCM
        putShort(22, 1)                     // mono
        putInt(24, SAMPLE_RATE)
        putInt(28, SAMPLE_RATE * 2)         // byte rate
        putShort(32, 2)                     // block align
        putShort(34, 16)                    // bits per sample
        putAscii(36, "data")
        putInt(40, dataSize)

        var offset = 44
        for (sample in samples) {
            out[offset] = (sample.toInt() and 0xFF).toByte()
            out[offset + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
            offset += 2
        }
        return out
    }

    private companion object {
        const val SAMPLE_RATE = 22_050

        /** Peak level of the synthesised cues, leaving headroom for the mixer. */
        const val LEVEL = 0.85

        const val VOLUME = 0.9f
        const val PRIORITY = 1
        const val MAX_STREAMS = 4
    }
}
