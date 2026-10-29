package com.arrowpuzzle.game.game

object ArrowLogic {
    sealed interface ClickResult {
        data class Correct(
            val arrowId: Int,
            val removedCount: Int,
        ) : ClickResult

        data class Wrong(
            val arrowId: Int,
            val failures: Int,
        ) : ClickResult

        data object AlreadyRemoved : ClickResult

        data object Ignored : ClickResult
    }

    fun evaluateClick(
        level: GeneratedLevel,
        removedIds: Set<Int>,
        arrowId: Int,
        failures: Int,
    ): ClickResult {
        if (arrowId in removedIds) return ClickResult.AlreadyRemoved
        if (level.arrowById(arrowId) == null) return ClickResult.Ignored

        return if (ArrowGeometry.isClickable(level, removedIds, arrowId)) {
            ClickResult.Correct(arrowId, removedIds.size + 1)
        } else {
            ClickResult.Wrong(arrowId, failures + 1)
        }
    }
}
