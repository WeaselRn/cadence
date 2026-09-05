package com.cadence.gaitradar.core.baseline

/**
 * Longitudinal mobility status according to personal baseline comparison rules.
 */
enum class LongitudinalStatus(
    val title: String,
    val description: String
) {
    BUILDING_BASELINE(
        title = "Building Personal Baseline",
        description = "Complete at least 3 valid walking assessments to establish your personal mobility baseline."
    ),
    STABLE(
        title = "Stable Mobility Pattern",
        description = "Stable: Your current mobility is consistent with your established baseline."
    ),
    CHANGE_DETECTED(
        title = "Mobility Change Detected",
        description = "Change Detected: Your recent mobility differs from your usual pattern."
    ),
    PERSISTENT_CHANGE(
        title = "Persistent Mobility Change",
        description = "Persistent Change: Your mobility has shown a persistent change from your usual pattern. Consider discussing persistent changes with a healthcare professional."
    )
}

/**
 * Historical baseline statistics calculated from prior valid completed walks.
 */
data class BaselineStats(
    val sampleCount: Int,
    val isEstablished: Boolean,
    val meanScore: Float,
    val stdScore: Float,
    val meanCadence: Float? = null,
    val stdCadence: Float? = null,
    val meanStepTimeVar: Float? = null
)

/**
 * Comparison output evaluating a current walk against personal baseline.
 */
data class BaselineComparison(
    val status: LongitudinalStatus,
    val scoreDelta: Float? = null,
    val scoreZScore: Float? = null,
    val consecutiveDeviations: Int = 0,
    val statusMessage: String = status.description
)
