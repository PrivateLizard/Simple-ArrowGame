package com.arrowpuzzle.game.game

import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.data.model.ScoreRecord

const val ARROW_EXIT_ANIMATION_MILLIS = 460L

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
}

/** Snapshot used by the result screens and for retry navigation. */
data class GameResult(
    val config: LevelConfig,
    val record: ScoreRecord,
    val isNewHighScore: Boolean,
)
