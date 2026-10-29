package com.arrowpuzzle.game.util

import android.media.AudioManager
import android.media.ToneGenerator
import com.arrowpuzzle.game.game.GameFeedback

/**
 * Tiny offline sound layer built on the platform ToneGenerator. No audio assets
 * are bundled, which keeps the APK small and avoids extra permissions.
 */
class SoundEffects {
    private var toneGenerator: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 60)
    }.getOrNull()

    fun play(feedback: GameFeedback) {
        val tone = when (feedback) {
            is GameFeedback.Correct -> ToneGenerator.TONE_PROP_ACK
            is GameFeedback.Wrong -> ToneGenerator.TONE_PROP_NACK
            is GameFeedback.Victory -> ToneGenerator.TONE_PROP_PROMPT
            is GameFeedback.Defeat -> ToneGenerator.TONE_PROP_BEEP2
        }
        val duration = when (feedback) {
            is GameFeedback.Correct -> 45
            is GameFeedback.Wrong -> 90
            is GameFeedback.Victory -> 180
            is GameFeedback.Defeat -> 220
        }
        runCatching { toneGenerator?.startTone(tone, duration) }
    }

    fun release() {
        runCatching { toneGenerator?.release() }
        toneGenerator = null
    }
}
