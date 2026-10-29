package com.arrowpuzzle.game.game

import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.data.model.ScoreRecord

const val ARROW_EXIT_ANIMATION_MILLIS = 460L

/**
 * Board cells per second the arrowhead travels during the exit animation.
 *
 * The animation is length-based, not duration-based: a long snaking arrow takes
 * proportionally longer than a short one, so every arrow moves at the same
 * visible speed. Using one fixed duration made long arrows race and short ones
 * crawl.
 */
const val ARROW_EXIT_CELLS_PER_SECOND = 34f

/**
 * Duration for an arrow whose visible path is [lengthCells] board cells long.
 * Clamped to a sensible window so a one-cell stub is not instant and a huge
 * snaking arrow does not feel sluggish.
 */
fun arrowExitDurationMillis(lengthCells: Int): Long {
    val seconds = lengthCells.coerceAtLeast(1) / ARROW_EXIT_CELLS_PER_SECOND.toDouble()
    val millis = (seconds * 1000.0).toLong()
    return millis.coerceIn(MIN_ARROW_EXIT_ANIMATION_MILLIS, MAX_ARROW_EXIT_ANIMATION_MILLIS)
}

const val MIN_ARROW_EXIT_ANIMATION_MILLIS = 260L
const val MAX_ARROW_EXIT_ANIMATION_MILLIS = 900L

sealed interface GameState {
    data object Loading : GameState

    data class Playing(
        val level: GeneratedLevel,
        val removedIds: Set<Int>,
        val failures: Int,
        val elapsedSeconds: Double,
        val wrongFlashId: Int? = null,
        val wrongFlashToken: Int = 0,
        val correctFlashId: Int? = null,
        val correctFlashToken: Int = 0,
    ) : GameState {
        val remainingCount: Int get() = level.arrowCount - removedIds.size
    }

    data class Victory(
        val level: GeneratedLevel,
        val record: ScoreRecord,
        val isNewHighScore: Boolean,
    ) : GameState

    data class Defeat(
        val level: GeneratedLevel,
        val reason: DefeatReason,
        val failures: Int,
        val elapsedSeconds: Double,
    ) : GameState

    data class Error(val message: String) : GameState
}

enum class DefeatReason {
    TOO_MANY_FAILURES,
    TIME_EXPIRED,
}

sealed interface GameFeedback {
    val hapticsEnabled: Boolean
    val soundEnabled: Boolean

    /** Distinguishes the cue without having to match on the data class. */
    val kind: Kind get() = when (this) {
        is Correct -> Kind.CORRECT
        is Wrong -> Kind.WRONG
        is Victory -> Kind.VICTORY
        is Defeat -> Kind.DEFEAT
    }

    data class Correct(
        override val hapticsEnabled: Boolean,
        override val soundEnabled: Boolean,
    ) : GameFeedback

    data class Wrong(
        override val hapticsEnabled: Boolean,
        override val soundEnabled: Boolean,
    ) : GameFeedback

    data class Victory(
        override val hapticsEnabled: Boolean,
        override val soundEnabled: Boolean,
    ) : GameFeedback

    data class Defeat(
        override val hapticsEnabled: Boolean,
        override val soundEnabled: Boolean,
    ) : GameFeedback

    enum class Kind { CORRECT, WRONG, VICTORY, DEFEAT }
}

/** Snapshot used by the result screens and for retry navigation. */
data class GameResult(
    val config: LevelConfig,
    val record: ScoreRecord,
    val isNewHighScore: Boolean,
)
