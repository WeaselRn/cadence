package com.cadence.gaitradar.core.metrics

import com.cadence.gaitradar.core.quality.QualityResult
import com.cadence.gaitradar.core.sensors.ImuSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

interface GaitMetricsEngine {
    suspend fun calculateMetrics(session: ImuSession, qualityResult: QualityResult): GaitMetricsResult
}

@Singleton
class GaitMetricsEngineImpl @Inject constructor() : GaitMetricsEngine {

    override suspend fun calculateMetrics(
        session: ImuSession,
        qualityResult: QualityResult
    ): GaitMetricsResult = withContext(Dispatchers.Default) {
        if (!qualityResult.isValid || session.samples.isEmpty()) {
            return@withContext GaitMetricsResult(isCalculated = false)
        }

        val samples = session.samples
        val count = samples.size
        val durationSec = session.durationMs / 1000f

        if (count < 10 || durationSec <= 0f) {
            return@withContext GaitMetricsResult(isCalculated = false)
        }

        // 1. Calculate orientation-robust 3D vector magnitudes
        val accelMag = FloatArray(count)
        val gyroMag = FloatArray(count)

        for (i in 0 until count) {
            val s = samples[i]
            accelMag[i] = sqrt(s.accelX * s.accelX + s.accelY * s.accelY + s.accelZ * s.accelZ)
            gyroMag[i] = sqrt(s.gyroX * s.gyroX + s.gyroY * s.gyroY + s.gyroZ * s.gyroZ)
        }

        // 2. Compute Accel & Gyro Variability (Std Dev of Magnitudes)
        val accelMean = accelMag.average().toFloat()
        val gyroMean = gyroMag.average().toFloat()

        var accelVarSum = 0.0
        var gyroVarSum = 0.0
        for (i in 0 until count) {
            accelVarSum += (accelMag[i] - accelMean).toDouble().pow(2.0)
            gyroVarSum += (gyroMag[i] - gyroMean).toDouble().pow(2.0)
        }
        val accelStdDev = sqrt(accelVarSum / count).toFloat()
        val gyroStdDev = sqrt(gyroVarSum / count).toFloat()

        // 3. Low-Pass Smoothing Filter for Peak Detection (Moving Average W=5)
        val smoothedAccel = FloatArray(count)
        val window = 5
        for (i in 0 until count) {
            var sum = 0f
            var wCount = 0
            for (j in (i - window / 2)..(i + window / 2)) {
                if (j in 0 until count) {
                    sum += accelMag[j]
                    wCount++
                }
            }
            smoothedAccel[i] = sum / wCount
        }

        // 4. Step Peak Detection
        val peakThreshold = accelMean + 0.35f * accelStdDev
        val minPeakDistanceNs = 300_000_000L // 300 ms minimum step interval (~200 steps/min max)

        val stepTimestampsNs = mutableListOf<Long>()
        var lastPeakNs = -1L

        for (i in 1 until count - 1) {
            val curr = smoothedAccel[i]
            val prev = smoothedAccel[i - 1]
            val next = smoothedAccel[i + 1]

            if (curr > prev && curr > next && curr > peakThreshold) {
                val ts = samples[i].timestampNs
                if (lastPeakNs < 0L || (ts - lastPeakNs) >= minPeakDistanceNs) {
                    stepTimestampsNs.add(ts)
                    lastPeakNs = ts
                }
            }
        }

        val stepCount = stepTimestampsNs.size

        // 5. Derive Cadence (steps / min)
        val cadence = if (durationSec > 0f && stepCount > 0) {
            (stepCount / durationSec) * 60f
        } else null

        // 6. Step Intervals & Step-Time Variability
        val stepIntervalsMs = mutableListOf<Float>()
        for (i in 1 until stepCount) {
            val intervalMs = (stepTimestampsNs[i] - stepTimestampsNs[i - 1]) / 1_000_000f
            stepIntervalsMs.add(intervalMs)
        }

        val meanStepInterval = if (stepIntervalsMs.isNotEmpty()) stepIntervalsMs.average().toFloat() else null
        val stepTimeVar = if (stepIntervalsMs.size > 1) {
            val m = meanStepInterval!!
            var varSum = 0.0
            for (interval in stepIntervalsMs) {
                varSum += (interval - m).toDouble().pow(2.0)
            }
            sqrt(varSum / stepIntervalsMs.size).toFloat()
        } else null

        // 7. Movement Regularity (Autocorrelation at 1-step lag)
        val regularity = if (stepIntervalsMs.size >= 3) {
            var dotProd = 0.0
            var normA = 0.0
            var normB = 0.0
            val m = meanStepInterval!!
            for (i in 0 until stepIntervalsMs.size - 1) {
                val a = stepIntervalsMs[i] - m
                val b = stepIntervalsMs[i + 1] - m
                dotProd += a * b
                normA += a * a
                normB += b * b
            }
            val denom = sqrt(normA * normB)
            if (denom > 1e-6) (dotProd / denom).toFloat().coerceIn(-1.0f, 1.0f) else null
        } else null

        // 8. Step Symmetry Index
        val symmetry = if (stepIntervalsMs.size >= 4) {
            val evenIntervals = mutableListOf<Float>()
            val oddIntervals = mutableListOf<Float>()
            for (i in stepIntervalsMs.indices) {
                if (i % 2 == 0) evenIntervals.add(stepIntervalsMs[i]) else oddIntervals.add(stepIntervalsMs[i])
            }
            val evenAvg = evenIntervals.average()
            val oddAvg = oddIntervals.average()
            val maxAvg = maxOf(evenAvg, oddAvg)
            if (maxAvg > 0.0) (minOf(evenAvg, oddAvg) / maxAvg).toFloat() else null
        } else null

        // 9. Estimated Speed (m/s) via Weinberg Step Length Model
        var maxAccel = Float.MIN_VALUE
        var minAccel = Float.MAX_VALUE
        for (a in accelMag) {
            if (a > maxAccel) maxAccel = a
            if (a < minAccel) minAccel = a
        }
        val accelRange = (maxAccel - minAccel).coerceAtLeast(0.1f)
        val estimatedStepLengthM = 0.45f * accelRange.toDouble().pow(0.25).toFloat()
        val estimatedSpeed = if (durationSec > 0f && stepCount > 0) {
            (stepCount * estimatedStepLengthM) / durationSec
        } else null

        GaitMetricsResult(
            stepCount = stepCount,
            cadenceStepsPerMin = cadence,
            meanStepIntervalMs = meanStepInterval,
            stepTimeVariabilityMs = stepTimeVar,
            movementRegularity = regularity,
            accelVariability = accelStdDev,
            gyroVariability = gyroStdDev,
            symmetryIndex = symmetry,
            estimatedSpeedMps = estimatedSpeed,
            isCalculated = true
        )
    }
}
