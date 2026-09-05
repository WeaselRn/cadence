package com.cadence.gaitradar.core.sensors

/**
 * Data model for a single 6-axis IMU raw sensor reading.
 * Preserves raw Android sensor values in six-channel order [ax, ay, az, gx, gy, gz].
 */
data class ImuSample(
    val timestampNs: Long,
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float
)

/**
 * Complete timestamped walking assessment session containing raw 6-axis IMU samples.
 */
data class ImuSession(
    val sessionId: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val samples: List<ImuSample>,
    val accelEventCount: Int,
    val gyroEventCount: Int,
    val averageSamplingRateHz: Float
)
