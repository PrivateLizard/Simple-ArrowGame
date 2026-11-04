package com.arrowpuzzle.game.data

import android.content.Context
import com.arrowpuzzle.game.data.model.ComplexityLevel
import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.game.LevelGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A single campaign level that was left before it was finished.
 *
 * Campaign levels are generated deterministically from their level id, so the
 * board does not need to be serialised: the removed arrow ids and the counters
 * are enough to rebuild the exact same state.
 */
data class SavedGame(
    val config: LevelConfig,
    /** Arrow count of the actually generated board (the config value is only a target). */
    val levelArrowCount: Int,
    val removedIds: Set<Int>,
    val failures: Int,
    val elapsedSeconds: Double,
    val savedAtMillis: Long,
) {
    val remainingCount: Int get() = (levelArrowCount - removedIds.size).coerceAtLeast(0)
}

/**
 * Persists the one in-progress campaign level that can be resumed from the home
 * screen. Custom runs are intentionally not kept, because their random seed is
 * not reproducible.
 */
class ProgressRepository(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val lock = Any()

    private val _savedGame = MutableStateFlow(load())
    val savedGame: StateFlow<SavedGame?> = _savedGame.asStateFlow()

    /**
     * Set immediately before navigating from the home "resume" button. It is
     * deliberately not persisted: after a process restart the app always lands
     * on the home screen, where the resume button is offered again from [savedGame].
     */
    @Volatile
    private var resumeRequested = false

    fun requestResume() {
        resumeRequested = true
    }

    /** Returns whether a resume was requested and resets the flag. */
    fun consumeResumeRequest(): Boolean {
        val requested = resumeRequested
        resumeRequested = false
        return requested
    }

    fun save(game: SavedGame) {
        if (game.config.isCustom) return
        synchronized(lock) {
            preferences.edit()
                .putInt(KEY_LEVEL_ID, game.config.levelId)
                .putInt(KEY_GENERATOR_VERSION, LevelGenerator.VERSION)
                .putInt(KEY_GRID_WIDTH, game.config.gridWidth)
                .putInt(KEY_GRID_HEIGHT, game.config.gridHeight)
                .putInt(KEY_ARROWS, game.config.arrowCount)
                .putInt(KEY_LEVEL_ARROW_COUNT, game.levelArrowCount)
                .putInt(KEY_MAX_FAILURES, game.config.maxFailures)
                .putInt(KEY_TIME_LIMIT, game.config.timeLimitSeconds ?: -1)
                .putString(KEY_COMPLEXITY, game.config.complexity.name)
                .putInt(KEY_OBSTACLES, game.config.obstacleCount)
                .putString(KEY_REMOVED_IDS, game.removedIds.joinToString(SEPARATOR))
                .putInt(KEY_FAILURES, game.failures)
                .putLong(KEY_ELAPSED_MILLIS, (game.elapsedSeconds * 1000.0).toLong())
                .putLong(KEY_SAVED_AT, game.savedAtMillis)
                .apply()
            _savedGame.value = game
        }
    }

    fun clear() {
        synchronized(lock) {
            preferences.edit()
                .remove(KEY_LEVEL_ID)
                .remove(KEY_GENERATOR_VERSION)
                .remove(KEY_GRID_WIDTH)
                .remove(KEY_GRID_HEIGHT)
                .remove(KEY_ARROWS)
                .remove(KEY_LEVEL_ARROW_COUNT)
                .remove(KEY_MAX_FAILURES)
                .remove(KEY_TIME_LIMIT)
                .remove(KEY_COMPLEXITY)
                .remove(KEY_OBSTACLES)
                .remove(KEY_REMOVED_IDS)
                .remove(KEY_FAILURES)
                .remove(KEY_ELAPSED_MILLIS)
                .remove(KEY_SAVED_AT)
                .apply()
            _savedGame.value = null
        }
    }

    private fun load(): SavedGame? {
        val levelId = preferences.getInt(KEY_LEVEL_ID, Int.MIN_VALUE)
        if (levelId == Int.MIN_VALUE || levelId <= 0) return null
        // A board from another generator version has different arrow ids; the
        // removed ids would point at the wrong arrows. Drop it instead.
        if (preferences.getInt(KEY_GENERATOR_VERSION, 1) != LevelGenerator.VERSION) {
            preferences.edit().clear().apply()
            return null
        }
        val complexity = runCatching {
            ComplexityLevel.valueOf(preferences.getString(KEY_COMPLEXITY, "") ?: "")
        }.getOrDefault(ComplexityLevel.MEDIUM)
        val removedIds = (preferences.getString(KEY_REMOVED_IDS, "") ?: "")
            .split(SEPARATOR)
            .mapNotNull { it.toIntOrNull() }
            .toSet()
        val config = LevelConfig(
            levelId = levelId,
            gridSize = preferences.getInt(KEY_GRID_WIDTH, 3) to
                preferences.getInt(KEY_GRID_HEIGHT, 3),
            arrowCount = preferences.getInt(KEY_ARROWS, 1),
            maxFailures = preferences.getInt(KEY_MAX_FAILURES, 5),
            timeLimitSeconds = preferences.getInt(KEY_TIME_LIMIT, -1).takeIf { it > 0 },
            complexity = complexity,
            obstacleCount = preferences.getInt(KEY_OBSTACLES, 0),
        )
        return SavedGame(
            config = config,
            levelArrowCount = preferences.getInt(KEY_LEVEL_ARROW_COUNT, config.arrowCount),
            removedIds = removedIds,
            failures = preferences.getInt(KEY_FAILURES, 0),
            elapsedSeconds = preferences.getLong(KEY_ELAPSED_MILLIS, 0L) / 1000.0,
            savedAtMillis = preferences.getLong(KEY_SAVED_AT, 0L),
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "arrow_puzzle_progress"
        const val SEPARATOR = ","

        const val KEY_LEVEL_ID = "level_id"
        const val KEY_GENERATOR_VERSION = "generator_version"
        const val KEY_GRID_WIDTH = "grid_width"
        const val KEY_GRID_HEIGHT = "grid_height"
        const val KEY_ARROWS = "arrows"
        const val KEY_LEVEL_ARROW_COUNT = "level_arrow_count"
        const val KEY_MAX_FAILURES = "max_failures"
        const val KEY_TIME_LIMIT = "time_limit"
        const val KEY_COMPLEXITY = "complexity"
        const val KEY_OBSTACLES = "obstacles"
        const val KEY_REMOVED_IDS = "removed_ids"
        const val KEY_FAILURES = "failures"
        const val KEY_ELAPSED_MILLIS = "elapsed_millis"
        const val KEY_SAVED_AT = "saved_at"
    }
}
