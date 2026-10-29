package com.arrowpuzzle.game.data.model

data class AppSettings(
    val maxFailures: Int = DEFAULT_MAX_FAILURES,
    val nightMode: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
) {
    companion object {
        const val DEFAULT_MAX_FAILURES = 5
    }
}
