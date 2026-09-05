package com.cadence.gaitradar.core.metrics

/**
 * Signal-derived physical gait features extracted from a quality-passed ImuSession.
 * Completely separate from the ML model input path.
 */
data class GaitMetricsResult(
    val stepCount: Int? = null,
    val cadenceStepsPerMin: Float? = null,
    val meanStepIntervalMs: Float? = null,
    val stepTimeVariabilityMs: Float? = null,
    val movementRegularity: Float? = null,
    val accelVariability: Float? = null,
    val gyroVariability: Float? = null,
    val symmetryIndex: Float? = null,
    val estimatedSpeedMps: Float? = null,
    val isCalculated: Boolean = false
)
