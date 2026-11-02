package com.arrowpuzzle.game.data.model

data class AppSettings(
    val maxFailures: Int = DEFAULT_MAX_FAILURES,
    val nightMode: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    /** Periodic visual hint (one open arrow wiggles). */
    val hintsEnabled: Boolean = true,
    /** Empty means: follow the device language. */
    val language: String = "",
) {
    companion object {
        const val DEFAULT_MAX_FAILURES = 5

        /**
         * Sentinel for "failures off": the failure counter never reaches this
         * value, so a level cannot be lost by wrong taps.
         */
        const val NO_FAILURE_LIMIT = Int.MAX_VALUE

        /** The fixed choices offered in Settings. 0 means "no mistake allowed". */
        val FAILURE_OPTIONS = listOf(0, 1, 2, 3, 5, 10, 20, NO_FAILURE_LIMIT)
    }
}
