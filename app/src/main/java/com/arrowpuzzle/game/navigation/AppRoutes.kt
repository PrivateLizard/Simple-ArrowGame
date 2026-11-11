package com.arrowpuzzle.game.navigation

import android.os.Bundle
import com.arrowpuzzle.game.data.model.ComplexityLevel
import com.arrowpuzzle.game.data.model.LevelConfig
import com.arrowpuzzle.game.data.model.ScoreRecord
import com.arrowpuzzle.game.game.DefeatReason
import com.arrowpuzzle.game.game.GameResult
import com.arrowpuzzle.game.game.GameState

data class DefeatRouteData(
    val config: LevelConfig,
    val reason: DefeatReason,
    val failures: Int,
    val elapsedSeconds: Double,
    val arrowCount: Int,
)

object AppRoutes {
    const val HOME = "home"
    const val FIRST_START = "first_start"
    const val LEVELS = "levels"
    const val SETTINGS = "settings"
    const val CUSTOM = "custom"
    const val TEST = "test"

    /** Route placeholder for "no shape" (route segments must not be empty). */
    private const val NO_SHAPE = "-"

    const val GAME =
        "game/{levelId}/{gw}/{gh}/{arrows}/{maxFailures}/{timeLimit}/{complexity}/{obstacles}/{shape}/{shapeCells}/{perArrow}"
    const val VICTORY =
        "victory/{levelId}/{gw}/{gh}/{arrows}/{maxFailures}/{timeLimit}/{complexity}/{obstacles}/{shape}/{shapeCells}/{perArrow}" +
            "/{points}/{timeMillis}/{failures}/{newBest}/{arrowsUsed}"
    const val DEFEAT =
        "defeat/{levelId}/{gw}/{gh}/{arrows}/{maxFailures}/{timeLimit}/{complexity}/{obstacles}/{shape}/{shapeCells}/{perArrow}" +
            "/{reason}/{failures}/{timeMillis}/{arrowCount}"

    fun game(config: LevelConfig): String = buildString {
        append("game/")
        append(config.levelId)
        append('/')
        append(config.gridWidth)
        append('/')
        append(config.gridHeight)
        append('/')
        append(config.arrowCount)
        append('/')
        append(config.maxFailures)
        append('/')
        append(config.timeLimitSeconds ?: -1)
        append('/')
        append(config.complexity.name)
        append('/')
        append(config.obstacleCount)
        append('/')
        append(config.shapeId ?: NO_SHAPE)
        append('/')
        append(config.shapeCells)
        append('/')
        append(config.secondsPerArrow ?: -1)
    }

    fun victory(result: GameResult): String = buildString {
        append("victory/")
        append(result.config.levelId)
        append('/')
        append(result.config.gridWidth)
        append('/')
        append(result.config.gridHeight)
        append('/')
        append(result.config.arrowCount)
        append('/')
        append(result.config.maxFailures)
        append('/')
        append(result.config.timeLimitSeconds ?: -1)
        append('/')
        append(result.config.complexity.name)
        append('/')
        append(result.config.obstacleCount)
        append('/')
        append(result.config.shapeId ?: NO_SHAPE)
        append('/')
        append(result.config.shapeCells)
        append('/')
        append(result.config.secondsPerArrow ?: -1)
        append('/')
        append(result.record.points)
        append('/')
        append((result.record.timeElapsed * 1000.0).toLong())
        append('/')
        append(result.record.failures)
        append('/')
        append(if (result.isNewHighScore) "true" else "false")
        append('/')
        // The real board size: the config value is only the generator target,
        // and the score breakdown must be recomputed from the real count.
        append(result.record.arrowsUsed)
    }

    fun defeat(state: GameState.Defeat, config: LevelConfig): String = buildString {
        append("defeat/")
        append(config.levelId)
        append('/')
        append(config.gridWidth)
        append('/')
        append(config.gridHeight)
        append('/')
        append(config.arrowCount)
        append('/')
        append(config.maxFailures)
        append('/')
        append(config.timeLimitSeconds ?: -1)
        append('/')
        append(config.complexity.name)
        append('/')
        append(config.obstacleCount)
        append('/')
        append(config.shapeId ?: NO_SHAPE)
        append('/')
        append(config.shapeCells)
        append('/')
        append(config.secondsPerArrow ?: -1)
        append('/')
        append(state.reason.name)
        append('/')
        append(state.failures)
        append('/')
        append((state.elapsedSeconds * 1000.0).toLong())
        append('/')
        append(state.level.arrowCount)
    }

    fun configFrom(arguments: Bundle?): LevelConfig? {
        if (arguments == null) return null
        val levelId = arguments.getInt("levelId", Int.MIN_VALUE)
        if (levelId == Int.MIN_VALUE) return null
        val width = arguments.getInt("gw", 3)
        val height = arguments.getInt("gh", 3)
        val arrows = arguments.getInt("arrows", 6)
        val maxFailures = arguments.getInt("maxFailures", 5)
        val timeLimit = arguments.getInt("timeLimit", -1).takeIf { it > 0 }
        val complexity = runCatching {
            ComplexityLevel.valueOf(arguments.getString("complexity") ?: "")
        }.getOrDefault(ComplexityLevel.MEDIUM)
        val obstacles = arguments.getInt("obstacles", 0)
        val shape = arguments.getString("shape")?.takeIf { it.isNotBlank() && it != NO_SHAPE }
        val shapeCells = arguments.getInt("shapeCells", 0)
        val perArrow = arguments.getInt("perArrow", -1).takeIf { it > 0 }
        return LevelConfig(
            levelId = levelId,
            gridSize = width to height,
            arrowCount = arrows,
            maxFailures = maxFailures,
            timeLimitSeconds = timeLimit,
            complexity = complexity,
            obstacleCount = obstacles,
            shapeId = shape,
            shapeCells = shapeCells,
            secondsPerArrow = perArrow,
        )
    }

    fun victoryResultFrom(arguments: Bundle?): GameResult? {
        val config = configFrom(arguments) ?: return null
        val points = arguments?.getInt("points", 0) ?: 0
        val timeMillis = arguments?.getLong("timeMillis", 0L) ?: 0L
        val failures = arguments?.getInt("failures", 0) ?: 0
        val newBest = arguments?.getBoolean("newBest", false) ?: false
        val arrowsUsed = arguments?.getInt("arrowsUsed", config.arrowCount) ?: config.arrowCount
        val record = ScoreRecord(
            levelId = config.levelId,
            points = points,
            arrowsUsed = arrowsUsed,
            timeElapsed = timeMillis / 1000.0,
            failures = failures,
            timestamp = 0L,
        )
        return GameResult(config = config, record = record, isNewHighScore = newBest)
    }

    fun defeatDataFrom(arguments: Bundle?): DefeatRouteData? {
        val config = configFrom(arguments) ?: return null
        val reason = runCatching {
            DefeatReason.valueOf(arguments?.getString("reason") ?: "")
        }.getOrNull() ?: return null
        val failures = arguments?.getInt("failures", 0) ?: 0
        val timeMillis = arguments?.getLong("timeMillis", 0L) ?: 0L
        val arrowCount = arguments?.getInt("arrowCount", config.arrowCount) ?: config.arrowCount
        return DefeatRouteData(
            config = config,
            reason = reason,
            failures = failures,
            elapsedSeconds = timeMillis / 1000.0,
            arrowCount = arrowCount,
        )
    }
}
