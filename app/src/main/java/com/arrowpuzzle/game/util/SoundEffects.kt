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
 *
 * [preload] must be called as early as possible (screen entry), because
 * SoundPool decodes asynchronously. An earlier version only loaded on the first
 * [play] call and silently dropped the cue while loading, which is why the very
 * first tap of a level made no sound and the second one was cut short.
 */
class SoundEffects(private val context: Context) {

    private val lock = Any()

    /** Lazily created so the cost is only paid when a cue is played. */
    private var soundPool: SoundPool? = null
    private val sampleIds = mutableMapOf<GameFeedback.Kind, Int>()
    private var loadedIds = emptySet<Int>()

    /**
     * Cue requested while its sample was still decoding. It is played once
     * loading finishes instead of being thrown away.
     */
    private var pendingKind: GameFeedback.Kind? = null

    /** Starts creating and loading the samples. Safe to call repeatedly. */
    fun preload() {
        ensureLoaded()
    }

    fun play(feedback: GameFeedback) {
        ensureLoaded()
        val kind = feedback.kind
        val pool = soundPool ?: return
        val id = sampleIds[kind] ?: return

        val ready = synchronized(lock) { id in loadedIds }
        if (!ready) {
            // Still decoding: remember it and let the load callback fire it.
            synchronized(lock) { pendingKind = kind }
            return
        }
        playNow(pool, id)
    }

    private fun playNow(pool: SoundPool, id: Int) {
        runCatching {
            pool.play(
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
                if (status != 0) return@setOnLoadCompleteListener
                synchronized(lock) { loadedIds = loadedIds + sampleId }

                // Play anything that was requested while this sample was
                // still decoding, so the first tap of a level is not silent.
                val deferred = synchronized(lock) {
                    val kind = pendingKind ?: return@synchronized null
                    val deferredId = sampleIds[kind]
                    if (deferredId == sampleId) {
                        pendingKind = null
                        deferredId
                    } else {
                        null
                    }
                }
                if (deferred != null) {
                    playNow(pool, deferred)
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
        val body = when (kind) {
            GameFeedback.Kind.CORRECT -> synthesizeBottlePlop()
            GameFeedback.Kind.WRONG -> synthesizeThud()
            GameFeedback.Kind.VICTORY -> concatenate(
                synthesizePlop(540.0, 640.0, 0.13),
                silence(0.04),
                synthesizePlop(720.0, 880.0, 0.13),
            )
            GameFeedback.Kind.DEFEAT -> synthesizePlop(420.0, 230.0, 0.24)
        }
        // Pad both ends with silence. The leading gap is the important part: a
        // cold audio path (after idle time or with the screen just back on)
        // swallows the first few dozen milliseconds of a stream. Without this
        // padding that latency ate into the attack and the cue sounded clipped
        // off - exactly like the first tap of a level. The trailing silence
        // gives the decay room to reach true zero before the stream ends.
        val samples = concatenate(
            silence(LEAD_IN_SECONDS),
            body,
            silence(TAIL_OUT_SECONDS),
        )
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
            val seconds = i.toDouble() / SAMPLE_RATE
            val frequency = start + (end - start) * progress
            phase += 2.0 * PI * frequency / SAMPLE_RATE
            val attack = (seconds / ATTACK_SECONDS).coerceAtMost(1.0)
            val decay = exp(-5.0 * progress)
            val sample = sin(phase) * attack * decay * LEVEL
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    /**
     * Deep, ploppy "bottle cap" cue for a successful tap: a fast downward
     * pitch drop with a little second harmonic and a short cap-release click.
     */
    private fun synthesizeBottlePlop(): ShortArray {
        val duration = 0.17
        val count = (SAMPLE_RATE * duration).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        var harmonicPhase = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val seconds = i.toDouble() / SAMPLE_RATE
            // 280 Hz down to ~74 Hz quickly: that is the "plop".
            val frequency = 70.0 + 210.0 * exp(-4.0 * progress)
            phase += 2.0 * PI * frequency / SAMPLE_RATE
            harmonicPhase += 2.0 * PI * (frequency * 2.0) / SAMPLE_RATE
            val attack = (seconds / 0.004).coerceAtMost(1.0)
            val decay = exp(-5.0 * progress)
            val body = sin(phase) * 0.80 + sin(harmonicPhase) * 0.15
            // Very short bright click for the cap releasing, gone in ~10 ms.
            val click = sin(2.0 * PI * 1200.0 * seconds) *
                exp(-seconds / 0.004) * 0.10
            val sample = (body * attack * decay + click) * LEVEL
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    /** Duller, lower thud for a wrong tap. */
    private fun synthesizeThud(): ShortArray {
        val duration = 0.18
        val count = (SAMPLE_RATE * duration).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val seconds = i.toDouble() / SAMPLE_RATE
            val frequency = 210.0 - 90.0 * progress
            phase += 2.0 * PI * frequency / SAMPLE_RATE
            val attack = (seconds / ATTACK_SECONDS).coerceAtMost(1.0)
            val decay = exp(-6.0 * progress)
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

        /**
         * Peak level of the synthesised cues. Normalised near full scale: the
         * earlier 0.85 * 0.9 combination was clearly too quiet on a phone
         * speaker.
         */
        const val LEVEL = 0.95

        const val VOLUME = 1.0f
        const val PRIORITY = 1

        /**
         * Fast attack so a crisp transient survives even when the very start
         * of the stream is lost to audio-path wake-up latency.
         */
        const val ATTACK_SECONDS = 0.01

        /**
         * Silence before the audible cue. This is deliberately consumed by the
         * cold-start latency of the audio output path instead of the attack.
         * 60 ms is long enough to cover typical device wake-up times while
         * staying below the ~100 ms where a game cue starts to feel laggy.
         */
        const val LEAD_IN_SECONDS = 0.06

        /** Lets the decay reach true zero before the stream is stopped. */
        const val TAIL_OUT_SECONDS = 0.02

        /**
         * Enough streams that a fast tap series never steals the previous
         * cue's stream, which used to cut it off mid-playback.
         */
        const val MAX_STREAMS = 8
    }
}
