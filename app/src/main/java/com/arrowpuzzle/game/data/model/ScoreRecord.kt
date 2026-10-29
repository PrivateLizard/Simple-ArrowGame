package com.arrowpuzzle.game.data.model

data class ScoreRecord(
    val levelId: Int,
    val points: Int,
    val arrowsUsed: Int,
    val timeElapsed: Double,
    val failures: Int,
    val timestamp: Long,
)
