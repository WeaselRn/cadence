package com.cadence.gaitradar.core.sensors

import kotlinx.coroutines.flow.Flow

/**
 * Data holder for 6-axis IMU raw sensor readings (Accelerometer X/Y/Z + Gyroscope X/Y/Z).
 */
data class ImuReading(
    val timestampNs: Long,
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float
)

/**
 * Abstraction layer for Android SensorManager collection.
 * Actual sensor sampling at target rate ~50 Hz will be implemented in Phase 3.
 */
interface SensorCollector {
    fun isSensorAvailable(): Boolean
    fun startCollection(): Flow<ImuReading>
    fun stopCollection()
}
