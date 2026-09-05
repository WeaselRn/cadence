package com.cadence.gaitradar.core.metrics

import com.cadence.gaitradar.core.quality.QualityCheckDetail
import com.cadence.gaitradar.core.quality.QualityFailureReason
import com.cadence.gaitradar.core.quality.QualityResult
import com.cadence.gaitradar.core.sensors.ImuSample
import com.cadence.gaitradar.core.sensors.ImuSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import kotlin.math.sin

class GaitMetricsEngineTest {

    private val engine = GaitMetricsEngineImpl()

    private fun createQualityResult(isValid: Boolean): QualityResult {
        val detail = QualityCheckDetail(
            isDurationValid = isValid,
            hasSixAxisCompleteness = isValid,
            isSampleCountValid = isValid,
            isSamplingRateValid = isValid,
            isTimestampMonotonic = isValid,
            areValuesFinite = isValid,
            isSignalSane = isValid
        )
        return QualityResult(
            isValid = isValid,
            failureReason = if (isValid) null else QualityFailureReason.SESSION_TOO_SHORT,
            failureMessage = if (isValid) null else QualityFailureReason.SESSION_TOO_SHORT.userMessage,
            durationMs = if (isValid) 30_000L else 5_000L,
            sampleCount = if (isValid) 1500 else 250,
            observedSamplingRateHz = 50.0f,
            details = detail
        )
    }

    private fun generateGaitSamples(
        count: Int = 1500,
        durationMs: Long = 30_000L,
        stepFrequencyHz: Float = 1.0f, // 1 step per second = 30 steps in 30s = 60 steps/min
        axisRotate: Boolean = false
    ): List<ImuSample> {
        val samples = mutableListOf<ImuSample>()
        val startNs = 1_000_000_000_000L
        val intervalNs = (durationMs * 1_000_000L) / count.coerceAtLeast(1)

        for (i in 0 until count) {
            val ts = startNs + (i * intervalNs)
            val timeSec = (i * intervalNs) / 1_000_000_000f

            // Sinusoidal gait acceleration wave with peaks
            val gaitSignal = 2.5f * sin(2.0 * Math.PI * stepFrequencyHz * timeSec).toFloat()

            val rawAx = 0.5f + gaitSignal
            val rawAy = 9.8f
            val rawAz = 0.2f
            val rawGx = 0.05f + gaitSignal * 0.1f
            val rawGy = 0.02f
            val rawGz = 0.01f

            val (ax, ay, az) = if (axisRotate) Triple(rawAz, rawAx, rawAy) else Triple(rawAx, rawAy, rawAz)

            samples.add(
                ImuSample(
                    timestampNs = ts,
                    accelX = ax,
                    accelY = ay,
                    accelZ = az,
                    gyroX = rawGx,
                    gyroY = rawGy,
                    gyroZ = rawGz
                )
            )
        }
        return samples
    }

    @Test
    fun `quality-passed walking session calculates valid gait metrics`() = runBlocking {
        val samples = generateGaitSamples(count = 1500, durationMs = 30_000L, stepFrequencyHz = 1.0f)
        val session = ImuSession(
            sessionId = UUID.randomUUID().toString(),
            startTimeMs = 1000L,
            endTimeMs = 31000L,
            durationMs = 30_000L,
            samples = samples,
            accelEventCount = 1500,
            gyroEventCount = 1500,
            averageSamplingRateHz = 50.0f
        )
        val qualityResult = createQualityResult(isValid = true)

        val metrics = engine.calculateMetrics(session, qualityResult)

        assertTrue(metrics.isCalculated)
        assertNotNull(metrics.stepCount)
        assertTrue(metrics.stepCount!! > 0)
        assertNotNull(metrics.cadenceStepsPerMin)
        assertTrue(metrics.cadenceStepsPerMin!! > 0f)
        assertNotNull(metrics.accelVariability)
        assertTrue(metrics.accelVariability!! > 0f)
        assertNotNull(metrics.gyroVariability)
    }

    @Test
    fun `quality-failed session returns uncalculated metrics with nulls`() = runBlocking {
        val samples = generateGaitSamples(count = 250, durationMs = 5_000L)
        val session = ImuSession(
            sessionId = UUID.randomUUID().toString(),
            startTimeMs = 1000L,
            endTimeMs = 6000L,
            durationMs = 5_000L,
            samples = samples,
            accelEventCount = 250,
            gyroEventCount = 250,
            averageSamplingRateHz = 50.0f
        )
        val qualityResult = createQualityResult(isValid = false)

        val metrics = engine.calculateMetrics(session, qualityResult)

        assertFalse(metrics.isCalculated)
        assertNull(metrics.stepCount)
        assertNull(metrics.cadenceStepsPerMin)
        assertNull(metrics.meanStepIntervalMs)
        assertNull(metrics.accelVariability)
    }

    @Test
    fun `orientation-rotated axes yield identical magnitude-based metrics`() = runBlocking {
        val normalSamples = generateGaitSamples(count = 1500, durationMs = 30_000L, axisRotate = false)
        val rotatedSamples = generateGaitSamples(count = 1500, durationMs = 30_000L, axisRotate = true)

        val sessionNormal = ImuSession(
            sessionId = "1", startTimeMs = 0L, endTimeMs = 30000L, durationMs = 30000L,
            samples = normalSamples, accelEventCount = 1500, gyroEventCount = 1500, averageSamplingRateHz = 50.0f
        )
        val sessionRotated = ImuSession(
            sessionId = "2", startTimeMs = 0L, endTimeMs = 30000L, durationMs = 30000L,
            samples = rotatedSamples, accelEventCount = 1500, gyroEventCount = 1500, averageSamplingRateHz = 50.0f
        )
        val qualityResult = createQualityResult(isValid = true)

        val m1 = engine.calculateMetrics(sessionNormal, qualityResult)
        val m2 = engine.calculateMetrics(sessionRotated, qualityResult)

        assertEquals(m1.stepCount, m2.stepCount)
        assertEquals(m1.cadenceStepsPerMin!!, m2.cadenceStepsPerMin!!, 0.1f)
        assertEquals(m1.accelVariability!!, m2.accelVariability!!, 0.01f)
    }

    @Test
    fun `faster walking frequency increases step count and cadence`() = runBlocking {
        val slowSamples = generateGaitSamples(count = 1500, durationMs = 30_000L, stepFrequencyHz = 0.8f)
        val fastSamples = generateGaitSamples(count = 1500, durationMs = 30_000L, stepFrequencyHz = 1.6f)

        val sessionSlow = ImuSession(
            sessionId = "slow", startTimeMs = 0L, endTimeMs = 30000L, durationMs = 30000L,
            samples = slowSamples, accelEventCount = 1500, gyroEventCount = 1500, averageSamplingRateHz = 50.0f
        )
        val sessionFast = ImuSession(
            sessionId = "fast", startTimeMs = 0L, endTimeMs = 30000L, durationMs = 30000L,
            samples = fastSamples, accelEventCount = 1500, gyroEventCount = 1500, averageSamplingRateHz = 50.0f
        )
        val qualityResult = createQualityResult(isValid = true)

        val mSlow = engine.calculateMetrics(sessionSlow, qualityResult)
        val mFast = engine.calculateMetrics(sessionFast, qualityResult)

        assertTrue(mFast.stepCount!! > mSlow.stepCount!!)
        assertTrue(mFast.cadenceStepsPerMin!! > mSlow.cadenceStepsPerMin!!)
    }
}
