package com.cadence.gaitradar.ml

import com.cadence.gaitradar.core.sensors.ImuSample
import com.cadence.gaitradar.core.sensors.ImuSession

object ImuPreprocessor {

    // Exact training normalization statistics from model_contract.json
    private val MEAN_ACCEL_X = 1.72378213f
    private val STD_ACCEL_X = 3.84108030f

    private val MEAN_ACCEL_Y = -0.31976627f
    private val STD_ACCEL_Y = 1.08265264f

    private val MEAN_ACCEL_Z = 8.71489520f
    private val STD_ACCEL_Z = 4.42052107f

    private val MEAN_GYRO_X = 0.00015508f
    private val STD_GYRO_X = 0.57736747f

    private val MEAN_GYRO_Y = -0.00027176f
    private val STD_GYRO_Y = 0.58553003f

    private val MEAN_GYRO_Z = -0.00045357f
    private val STD_GYRO_Z = 0.55582134f

    const val TARGET_TIMESTEPS = 1500
    private const val TARGET_INTERVAL_NS = 20_000_000L // 20 ms = 50 Hz

    /**
     * Preprocesses raw ImuSession into exact Float32 [1, 1500, 6] tensor input for TFLite.
     */
    fun preprocessSession(session: ImuSession): Array<Array<FloatArray>> {
        val samples = session.samples
        require(samples.isNotEmpty()) { "ImuSession contains no samples." }

        // 1. Construct deterministic 1500-step time grid at 50 Hz
        val startNs = samples.first().timestampNs
        val resampled = Array(TARGET_TIMESTEPS) { FloatArray(6) }

        var sampleIndex = 0

        for (k in 0 until TARGET_TIMESTEPS) {
            val targetNs = startNs + (k * TARGET_INTERVAL_NS)

            // Advance sampleIndex to find interval containing targetNs
            while (sampleIndex < samples.size - 2 && samples[sampleIndex + 1].timestampNs < targetNs) {
                sampleIndex++
            }

            val s1 = samples[sampleIndex]
            val s2 = if (sampleIndex < samples.size - 1) samples[sampleIndex + 1] else s1

            val t1 = s1.timestampNs
            val t2 = s2.timestampNs

            val alpha = if (t2 > t1) {
                ((targetNs - t1).toDouble() / (t2 - t1).toDouble()).coerceIn(0.0, 1.0).toFloat()
            } else 0.0f

            // Linear Interpolation for raw 6-axis values
            val ax = s1.accelX + alpha * (s2.accelX - s1.accelX)
            val ay = s1.accelY + alpha * (s2.accelY - s1.accelY)
            val az = s1.accelZ + alpha * (s2.accelZ - s1.accelZ)
            val gx = s1.gyroX + alpha * (s2.gyroX - s1.gyroX)
            val gy = s1.gyroY + alpha * (s2.gyroY - s1.gyroY)
            val gz = s1.gyroZ + alpha * (s2.gyroZ - s1.gyroZ)

            // 2. Per-Channel Z-Score Normalization using contract training statistics
            resampled[k][0] = (ax - MEAN_ACCEL_X) / STD_ACCEL_X
            resampled[k][1] = (ay - MEAN_ACCEL_Y) / STD_ACCEL_Y
            resampled[k][2] = (az - MEAN_ACCEL_Z) / STD_ACCEL_Z
            resampled[k][3] = (gx - MEAN_GYRO_X) / STD_GYRO_X
            resampled[k][4] = (gy - MEAN_GYRO_Y) / STD_GYRO_Y
            resampled[k][5] = (gz - MEAN_GYRO_Z) / STD_GYRO_Z
        }

        // Return batch-dimensioned array: Float32 [1, 1500, 6]
        return arrayOf(resampled)
    }
}
