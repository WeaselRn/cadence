package com.cadence.gaitradar.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class SensorCollectorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorCollector {

    private val sensorManager: SensorManager? by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    override fun isAccelerometerAvailable(): Boolean {
        return sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }

    override fun isGyroscopeAvailable(): Boolean {
        return sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }

    override fun isSensorsAvailable(): Boolean {
        return isAccelerometerAvailable() && isGyroscopeAvailable()
    }

    companion object {
        const val TARGET_SAMPLING_PERIOD_US = 20_000 // 20,000 µs = 20 ms = 50 Hz
        const val MAX_PAIRING_TOLERANCE_NS = 15_000_000L // 15 ms max difference for accel/gyro alignment
    }

    private data class AccelData(val timestampNs: Long, val x: Float, val y: Float, val z: Float)
    private data class GyroData(val timestampNs: Long, val x: Float, val y: Float, val z: Float)

    override fun startCollection(): Flow<ImuSample> = callbackFlow {
        val sm = sensorManager
        if (sm == null) {
            close()
            return@callbackFlow
        }

        val accelSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroSensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (accelSensor == null || gyroSensor == null) {
            close()
            return@callbackFlow
        }

        val accelQueue = ArrayDeque<AccelData>()
        val gyroQueue = ArrayDeque<GyroData>()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                synchronized(this) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> {
                            accelQueue.addLast(
                                AccelData(event.timestamp, event.values[0], event.values[1], event.values[2])
                            )
                        }
                        Sensor.TYPE_GYROSCOPE -> {
                            gyroQueue.addLast(
                                GyroData(event.timestamp, event.values[0], event.values[1], event.values[2])
                            )
                        }
                    }

                    // Align queued accelerometer and gyroscope readings by nearest timestamp
                    while (accelQueue.isNotEmpty() && gyroQueue.isNotEmpty()) {
                        val accel = accelQueue.first
                        val gyro = gyroQueue.first
                        val timeDiffNs = accel.timestampNs - gyro.timestampNs

                        if (abs(timeDiffNs) <= MAX_PAIRING_TOLERANCE_NS) {
                            val sample = ImuSample(
                                timestampNs = (accel.timestampNs + gyro.timestampNs) / 2,
                                accelX = accel.x,
                                accelY = accel.y,
                                accelZ = accel.z,
                                gyroX = gyro.x,
                                gyroY = gyro.y,
                                gyroZ = gyro.z
                            )
                            trySend(sample)
                            accelQueue.removeFirst()
                            gyroQueue.removeFirst()
                        } else if (accel.timestampNs < gyro.timestampNs) {
                            // Accel sample is too old without a matching gyro sample
                            accelQueue.removeFirst()
                        } else {
                            // Gyro sample is too old without a matching accel sample
                            gyroQueue.removeFirst()
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sm.registerListener(listener, accelSensor, TARGET_SAMPLING_PERIOD_US)
        sm.registerListener(listener, gyroSensor, TARGET_SAMPLING_PERIOD_US)

        awaitClose {
            sm.unregisterListener(listener)
        }
    }

    override fun stopCollection() {
        // Lifecycle unregistration is automatically handled via callbackFlow awaitClose
    }
}
