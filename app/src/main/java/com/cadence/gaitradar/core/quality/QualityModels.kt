package com.cadence.gaitradar.core.quality

/**
 * Centralized thresholds for validating 6-axis IMU walking sessions (~30s at ~50 Hz).
 */
object QualityThresholds {
    const val MIN_DURATION_MS: Long = 20_000L
    const val MAX_DURATION_MS: Long = 45_000L
    const val MIN_SAMPLES: Int = 1000
    const val MIN_AVERAGE_SAMPLING_RATE_HZ: Float = 35.0f
    const val MAX_AVERAGE_SAMPLING_RATE_HZ: Float = 65.0f
    const val MAX_TIMESTAMP_GAP_NS: Long = 500_000_000L // 500ms
    const val MIN_ACCEL_VARIANCE: Float = 0.001f
    const val MIN_GYRO_VARIANCE: Float = 0.0001f
}

/**
 * Explicit failure reasons explaining why an ImuSession failed the quality gate.
 */
enum class QualityFailureReason(val userMessage: String) {
    SESSION_TOO_SHORT("Walking session was too short. Please walk for the full 30 seconds."),
    SESSION_TOO_LONG("Walking session exceeded max duration threshold."),
    MISSING_SENSOR_DATA("Six-axis sensor data incomplete. Both accelerometer and gyroscope are required."),
    INSUFFICIENT_SAMPLES("Not enough IMU samples recorded during the session."),
    IRREGULAR_SAMPLING_RATE("Sensor sampling rate was unstable or outside acceptable 35–65 Hz range."),
    TIMESTAMP_ANOMALY("Sensor timestamps contained gaps or non-monotonic jumps."),
    NON_FINITE_VALUES("Invalid sensor readings detected (NaN or Infinite values)."),
    FLATLINE_SIGNAL("No movement detected. Please ensure the phone is placed in your pocket while walking.")
}

/**
 * Detailed breakdown of individual checks performed during quality evaluation.
 */
data class QualityCheckDetail(
    val isDurationValid: Boolean,
    val hasSixAxisCompleteness: Boolean,
    val isSampleCountValid: Boolean,
    val isSamplingRateValid: Boolean,
    val isTimestampMonotonic: Boolean,
    val areValuesFinite: Boolean,
    val isSignalSane: Boolean
)

/**
 * Result output from ImuQualityGate evaluation.
 */
data class QualityResult(
    val isValid: Boolean,
    val failureReason: QualityFailureReason? = null,
    val failureMessage: String? = null,
    val durationMs: Long,
    val sampleCount: Int,
    val observedSamplingRateHz: Float,
    val details: QualityCheckDetail
)
