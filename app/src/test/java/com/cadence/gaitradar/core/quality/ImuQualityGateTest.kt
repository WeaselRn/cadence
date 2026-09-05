package com.cadence.gaitradar.core.quality

import com.cadence.gaitradar.core.sensors.ImuSample
import com.cadence.gaitradar.core.sensors.ImuSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ImuQualityGateTest {

    private val qualityGate = ImuQualityGateImpl()

    private fun generateSamples(
        count: Int,
        durationMs: Long,
        addVariance: Boolean = true,
        injectNan: Boolean = false,
        injectGap: Boolean = false
    ): List<ImuSample> {
        val list = mutableListOf<ImuSample>()
        val startNs = 1_000_000_000_000L
        val intervalNs = (durationMs * 1_000_000L) / count.coerceAtLeast(1)

        for (i in 0 until count) {
            val ts = if (injectGap && i == count / 2) {
                startNs + (i * intervalNs) + 1_000_000_000L // 1 second gap
            } else {
                startNs + (i * intervalNs)
            }

            val varVal = if (addVariance) (i % 10) * 0.1f else 0.0f
            val ax = if (injectNan && i == count / 2) Float.NaN else 0.5f + varVal
            val ay = 9.8f + varVal
            val az = 1.0f + varVal
            val gx = 0.01f + varVal
            val gy = 0.02f + varVal
            val gz = 0.03f + varVal

            list.add(
                ImuSample(
                    timestampNs = ts,
                    accelX = ax,
                    accelY = ay,
                    accelZ = az,
                    gyroX = gx,
                    gyroY = gy,
                    gyroZ = gz
                )
            )
        }
        return list
    }

    @Test
    fun `valid 30s session returns PASS`() {
        val samples = generateSamples(count = 1500, durationMs = 30_000L)
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

        val result = qualityGate.evaluate(session)

        assertTrue(result.isValid)
        assertNull(result.failureReason)
        assertTrue(result.details.isDurationValid)
        assertTrue(result.details.hasSixAxisCompleteness)
        assertTrue(result.details.isSampleCountValid)
        assertTrue(result.details.isSamplingRateValid)
        assertTrue(result.details.isTimestampMonotonic)
        assertTrue(result.details.areValuesFinite)
        assertTrue(result.details.isSignalSane)
    }

    @Test
    fun `short 5s session returns SESSION_TOO_SHORT`() {
        val samples = generateSamples(count = 250, durationMs = 5_000L)
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

        val result = qualityGate.evaluate(session)

        assertFalse(result.isValid)
        assertEquals(QualityFailureReason.SESSION_TOO_SHORT, result.failureReason)
        assertFalse(result.details.isDurationValid)
    }

    @Test
    fun `empty session returns MISSING_SENSOR_DATA`() {
        val session = ImuSession(
            sessionId = UUID.randomUUID().toString(),
            startTimeMs = 1000L,
            endTimeMs = 31000L,
            durationMs = 30_000L,
            samples = emptyList(),
            accelEventCount = 0,
            gyroEventCount = 0,
            averageSamplingRateHz = 0.0f
        )

        val result = qualityGate.evaluate(session)

        assertFalse(result.isValid)
        assertEquals(QualityFailureReason.MISSING_SENSOR_DATA, result.failureReason)
        assertFalse(result.details.hasSixAxisCompleteness)
    }

    @Test
    fun `session with low sample count returns INSUFFICIENT_SAMPLES`() {
        val samples = generateSamples(count = 500, durationMs = 25_000L)
        val session = ImuSession(
            sessionId = UUID.randomUUID().toString(),
            startTimeMs = 1000L,
            endTimeMs = 26000L,
            durationMs = 25_000L,
            samples = samples,
            accelEventCount = 500,
            gyroEventCount = 500,
            averageSamplingRateHz = 20.0f
        )

        val result = qualityGate.evaluate(session)

        assertFalse(result.isValid)
        assertEquals(QualityFailureReason.INSUFFICIENT_SAMPLES, result.failureReason)
        assertFalse(result.details.isSampleCountValid)
    }

    @Test
    fun `session with NaN sensor values returns NON_FINITE_VALUES`() {
        val samples = generateSamples(count = 1500, durationMs = 30_000L, injectNan = true)
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

        val result = qualityGate.evaluate(session)

        assertFalse(result.isValid)
        assertEquals(QualityFailureReason.NON_FINITE_VALUES, result.failureReason)
        assertFalse(result.details.areValuesFinite)
    }

    @Test
    fun `session with large timestamp gap returns TIMESTAMP_ANOMALY`() {
        val samples = generateSamples(count = 1500, durationMs = 30_000L, injectGap = true)
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

        val result = qualityGate.evaluate(session)

        assertFalse(result.isValid)
        assertEquals(QualityFailureReason.TIMESTAMP_ANOMALY, result.failureReason)
        assertFalse(result.details.isTimestampMonotonic)
    }

    @Test
    fun `session with flatline signal returns FLATLINE_SIGNAL`() {
        val samples = generateSamples(count = 1500, durationMs = 30_000L, addVariance = false)
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

        val result = qualityGate.evaluate(session)

        assertFalse(result.isValid)
        assertEquals(QualityFailureReason.FLATLINE_SIGNAL, result.failureReason)
        assertFalse(result.details.isSignalSane)
    }

    @Test
    fun `session with realistic ~50Hz sampling variation returns PASS`() {
        val samples = generateSamples(count = 1350, durationMs = 30_000L) // 45 Hz avg
        val session = ImuSession(
            sessionId = UUID.randomUUID().toString(),
            startTimeMs = 1000L,
            endTimeMs = 31000L,
            durationMs = 30_000L,
            samples = samples,
            accelEventCount = 1350,
            gyroEventCount = 1350,
            averageSamplingRateHz = 45.0f
        )

        val result = qualityGate.evaluate(session)

        assertTrue(result.isValid)
        assertNull(result.failureReason)
    }
}
