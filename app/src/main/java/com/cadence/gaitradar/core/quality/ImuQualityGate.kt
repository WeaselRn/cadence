package com.cadence.gaitradar.core.quality

import com.cadence.gaitradar.core.sensors.ImuSession
import javax.inject.Inject
import javax.inject.Singleton

interface ImuQualityGate {
    fun evaluate(session: ImuSession): QualityResult
}

@Singleton
class ImuQualityGateImpl @Inject constructor() : ImuQualityGate {

    override fun evaluate(session: ImuSession): QualityResult {
        val samples = session.samples
        val durationMs = session.durationMs
        val sampleCount = samples.size
        val avgHz = session.averageSamplingRateHz

        // 1. Check six-axis data availability & non-empty
        val hasSixAxis = samples.isNotEmpty() &&
                session.accelEventCount > 0 &&
                session.gyroEventCount > 0

        if (!hasSixAxis) {
            val details = QualityCheckDetail(
                isDurationValid = false,
                hasSixAxisCompleteness = false,
                isSampleCountValid = false,
                isSamplingRateValid = false,
                isTimestampMonotonic = false,
                areValuesFinite = false,
                isSignalSane = false
            )
            return QualityResult(
                isValid = false,
                failureReason = QualityFailureReason.MISSING_SENSOR_DATA,
                failureMessage = QualityFailureReason.MISSING_SENSOR_DATA.userMessage,
                durationMs = durationMs,
                sampleCount = sampleCount,
                observedSamplingRateHz = avgHz,
                details = details
            )
        }

        // 2. Check for finite values (no NaN or Infinities)
        var areValuesFinite = true
        for (s in samples) {
            if (s.accelX.isNaN() || s.accelX.isInfinite() ||
                s.accelY.isNaN() || s.accelY.isInfinite() ||
                s.accelZ.isNaN() || s.accelZ.isInfinite() ||
                s.gyroX.isNaN() || s.gyroX.isInfinite() ||
                s.gyroY.isNaN() || s.gyroY.isInfinite() ||
                s.gyroZ.isNaN() || s.gyroZ.isInfinite()
            ) {
                areValuesFinite = false
                break
            }
        }

        // 3. Check Duration
        val isDurationValid = durationMs in QualityThresholds.MIN_DURATION_MS..QualityThresholds.MAX_DURATION_MS
        val isDurationTooShort = durationMs < QualityThresholds.MIN_DURATION_MS
        val isDurationTooLong = durationMs > QualityThresholds.MAX_DURATION_MS

        // 4. Check Sample Count
        val isSampleCountValid = sampleCount >= QualityThresholds.MIN_SAMPLES

        // 5. Check Sampling Rate
        val isSamplingRateValid = avgHz in QualityThresholds.MIN_AVERAGE_SAMPLING_RATE_HZ..QualityThresholds.MAX_AVERAGE_SAMPLING_RATE_HZ

        // 6. Check Timestamps Monotonicity and Gaps
        var isTimestampMonotonic = true
        for (i in 1 until samples.size) {
            val prevNs = samples[i - 1].timestampNs
            val currNs = samples[i].timestampNs
            val diffNs = currNs - prevNs

            if (diffNs <= 0 || diffNs > QualityThresholds.MAX_TIMESTAMP_GAP_NS) {
                isTimestampMonotonic = false
                break
            }
        }

        // 7. Check Signal Sanity (no zero-variance flatline)
        var isSignalSane = true
        var accelXSum = 0f
        var accelYSum = 0f
        var accelZSum = 0f
        for (s in samples) {
            accelXSum += s.accelX
            accelYSum += s.accelY
            accelZSum += s.accelZ
        }
        val meanX = accelXSum / sampleCount
        val meanY = accelYSum / sampleCount
        val meanZ = accelZSum / sampleCount

        var varX = 0f
        var varY = 0f
        var varZ = 0f
        for (s in samples) {
            varX += (s.accelX - meanX) * (s.accelX - meanX)
            varY += (s.accelY - meanY) * (s.accelY - meanY)
            varZ += (s.accelZ - meanZ) * (s.accelZ - meanZ)
        }
        val totalAccelVar = (varX + varY + varZ) / sampleCount
        if (totalAccelVar < QualityThresholds.MIN_ACCEL_VARIANCE) {
            isSignalSane = false
        }

        val details = QualityCheckDetail(
            isDurationValid = isDurationValid,
            hasSixAxisCompleteness = hasSixAxis,
            isSampleCountValid = isSampleCountValid,
            isSamplingRateValid = isSamplingRateValid,
            isTimestampMonotonic = isTimestampMonotonic,
            areValuesFinite = areValuesFinite,
            isSignalSane = isSignalSane
        )

        val failureReason = when {
            !areValuesFinite -> QualityFailureReason.NON_FINITE_VALUES
            isDurationTooShort -> QualityFailureReason.SESSION_TOO_SHORT
            isDurationTooLong -> QualityFailureReason.SESSION_TOO_LONG
            !isSampleCountValid -> QualityFailureReason.INSUFFICIENT_SAMPLES
            !isSamplingRateValid -> QualityFailureReason.IRREGULAR_SAMPLING_RATE
            !isTimestampMonotonic -> QualityFailureReason.TIMESTAMP_ANOMALY
            !isSignalSane -> QualityFailureReason.FLATLINE_SIGNAL
            else -> null
        }

        val isValid = failureReason == null

        return QualityResult(
            isValid = isValid,
            failureReason = failureReason,
            failureMessage = failureReason?.userMessage,
            durationMs = durationMs,
            sampleCount = sampleCount,
            observedSamplingRateHz = avgHz,
            details = details
        )
    }
}
