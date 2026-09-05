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
import javax.inject.Inject
import javax.inject.Singleton

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

        var latestAccelX = 0f
        var latestAccelY = 0f
        var latestAccelZ = 0f

        var latestGyroX = 0f
        var latestGyroY = 0f
        var latestGyroZ = 0f

        var hasReceivedAccel = false
        var hasReceivedGyro = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        latestAccelX = event.values[0]
                        latestAccelY = event.values[1]
                        latestAccelZ = event.values[2]
                        hasReceivedAccel = true
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        latestGyroX = event.values[0]
                        latestGyroY = event.values[1]
                        latestGyroZ = event.values[2]
                        hasReceivedGyro = true
                    }
                }

                if (hasReceivedAccel && hasReceivedGyro) {
                    val sample = ImuSample(
                        timestampNs = event.timestamp,
                        accelX = latestAccelX,
                        accelY = latestAccelY,
                        accelZ = latestAccelZ,
                        gyroX = latestGyroX,
                        gyroY = latestGyroY,
                        gyroZ = latestGyroZ
                    )
                    trySend(sample)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sm.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_GAME)
        sm.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_GAME)

        awaitClose {
            sm.unregisterListener(listener)
        }
    }

    override fun stopCollection() {
        // Lifecycle unregistration is automatically handled via callbackFlow awaitClose
    }
}
