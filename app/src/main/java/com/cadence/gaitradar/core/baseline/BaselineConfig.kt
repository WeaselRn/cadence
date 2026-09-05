package com.cadence.gaitradar.core.baseline

/**
 * Centralized configuration for baseline establishment and longitudinal deviation rules.
 */
object BaselineConfig {
    const val MINIMUM_BASELINE_WALKS = 3
    const val DEVIATION_Z_THRESHOLD = -2.0f
    const val MIN_SCORE_DROP_POINTS = 10.0f
    const val PERSISTENT_DEVIATION_COUNT = 3
    const val MIN_STD_DEV = 1.0f
}
