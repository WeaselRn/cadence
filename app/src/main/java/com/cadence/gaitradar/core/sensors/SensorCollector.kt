package com.cadence.gaitradar.core.sensors

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction layer for Android SensorManager collection.
 */
interface SensorCollector {
    fun isAccelerometerAvailable(): Boolean
    fun isGyroscopeAvailable(): Boolean
    fun isSensorsAvailable(): Boolean
    fun startCollection(): Flow<ImuSample>
    fun stopCollection()
}
