package com.arrowpuzzle.game.data

import android.content.Context
import com.arrowpuzzle.game.data.model.ScoreRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScoreRepository(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val lock = Any()

    private val _bestScores = MutableStateFlow(loadBestScores())
    val bestScores: StateFlow<Map<Int, Int>> = _bestScores.asStateFlow()

    private val _globalHighScore = MutableStateFlow(preferences.getInt(KEY_GLOBAL_BEST, 0))
    val globalHighScore: StateFlow<Int> = _globalHighScore.asStateFlow()

    /**
     * Sum of the best score of every campaign level. Custom runs (level id 0)
     * are excluded, because the home screen shows campaign progress only.
     */
    private val _campaignScoreTotal = MutableStateFlow(campaignTotal(_bestScores.value))
    val campaignScoreTotal: StateFlow<Int> = _campaignScoreTotal.asStateFlow()

    private val _highestUnlocked = MutableStateFlow(preferences.getInt(KEY_UNLOCKED, 1).coerceAtLeast(1))
    val highestUnlocked: StateFlow<Int> = _highestUnlocked.asStateFlow()

    /**
     * Stores a victory. Returns true when the score is a new personal best for
     * its level (or for custom mode when levelId is zero).
     */
    fun recordScore(record: ScoreRecord): Boolean {
        synchronized(lock) {
            val previousBest = _bestScores.value[record.levelId] ?: 0
            val isNewBest = record.points > previousBest
            if (isNewBest) {
                val updated = _bestScores.value.toMutableMap()
                updated[record.levelId] = record.points
                _bestScores.value = updated
                _campaignScoreTotal.value = campaignTotal(updated)
                preferences.edit().putInt(levelKey(record.levelId), record.points).apply()
            }

            if (record.points > _globalHighScore.value) {
                _globalHighScore.value = record.points
                preferences.edit().putInt(KEY_GLOBAL_BEST, record.points).apply()
            }

            if (record.levelId > 0) {
                val nextLevel = record.levelId + 1
                if (nextLevel > _highestUnlocked.value) {
                    _highestUnlocked.value = nextLevel
                    preferences.edit().putInt(KEY_UNLOCKED, nextLevel).apply()
                }
            }
            return isNewBest
        }
    }

    /**
     * Developer helper: unlock every campaign level, or lock everything again.
     *
     * Unlocking keeps best scores untouched. Locking resets progression to
     * level 1 but, like [clearProgress], leaves scores and settings alone.
     */
    fun setAllLevelsUnlocked(unlocked: Boolean, maxLevel: Int) {
        synchronized(lock) {
            val value = if (unlocked) maxLevel.coerceAtLeast(1) else 1
            _highestUnlocked.value = value
            preferences.edit().putInt(KEY_UNLOCKED, value).apply()
        }
    }

    fun clearProgress() {
        synchronized(lock) {
            val editor = preferences.edit()
            preferences.all.keys
                .filter { it.startsWith(LEVEL_KEY_PREFIX) }
                .forEach(editor::remove)
            editor
                .remove(KEY_GLOBAL_BEST)
                .remove(KEY_UNLOCKED)
                .apply()
            _bestScores.value = emptyMap()
            _campaignScoreTotal.value = 0
            _globalHighScore.value = 0
            _highestUnlocked.value = 1
        }
    }

    private fun campaignTotal(scores: Map<Int, Int>): Int =
        scores.entries
            .filter { (levelId, _) -> levelId > 0 }
            .sumOf { (_, points) -> points }

    private fun loadBestScores(): Map<Int, Int> {
        return preferences.all.mapNotNull { (key, value) ->
            if (!key.startsWith(LEVEL_KEY_PREFIX)) return@mapNotNull null
            val levelId = key.removePrefix(LEVEL_KEY_PREFIX).toIntOrNull()
                ?: return@mapNotNull null
            val points = (value as? Int) ?: return@mapNotNull null
            levelId to points
        }.toMap()
    }

    private fun levelKey(levelId: Int): String = "$LEVEL_KEY_PREFIX$levelId"


    private companion object {
        const val PREFERENCES_NAME = "arrow_puzzle_scores"
        const val LEVEL_KEY_PREFIX = "best_level_"
        const val KEY_GLOBAL_BEST = "global_best"
        const val KEY_UNLOCKED = "highest_unlocked"
    }
}
