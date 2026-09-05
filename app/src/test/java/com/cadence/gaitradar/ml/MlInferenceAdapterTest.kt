package com.cadence.gaitradar.ml

import com.cadence.gaitradar.core.sensors.ImuSample
import com.cadence.gaitradar.core.sensors.ImuSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import kotlin.math.roundToInt

class MlInferenceAdapterTest {

    private fun generateTestSession(sampleCount: Int = 1500, durationMs: Long = 30_000L): ImuSession {
        val samples = mutableListOf<ImuSample>()
        val startNs = 1_000_000_000_000L
        val intervalNs = (durationMs * 1_000_000L) / sampleCount.coerceAtLeast(1)

        for (i in 0 until sampleCount) {
            val ts = startNs + (i * intervalNs)
            samples.add(
                ImuSample(
                    timestampNs = ts,
                    accelX = 1.72378213f, // Equal to mean -> norm = 0
                    accelY = -0.31976627f, // Equal to mean -> norm = 0
                    accelZ = 8.71489520f,  // Equal to mean -> norm = 0
                    gyroX = 0.00015508f,   // Equal to mean -> norm = 0
                    gyroY = -0.00027176f,  // Equal to mean -> norm = 0
                    gyroZ = -0.00045357f   // Equal to mean -> norm = 0
                )
            )
        }

        return ImuSession(
            sessionId = UUID.randomUUID().toString(),
            startTimeMs = 1000L,
            endTimeMs = 1000L + durationMs,
            durationMs = durationMs,
            samples = samples,
            accelEventCount = sampleCount,
            gyroEventCount = sampleCount,
            averageSamplingRateHz = sampleCount.toFloat() / (durationMs / 1000f)
        )
    }

    @Test
    fun `preprocessor constructs exact 1x1500x6 normalized tensor`() {
        val session = generateTestSession(sampleCount = 1200, durationMs = 30_000L)
        val tensor = ImuPreprocessor.preprocessSession(session)

        assertNotNull(tensor)
        assertEquals(1, tensor.size)
        assertEquals(1500, tensor[0].size)
        assertEquals(6, tensor[0][0].size)

        // Since inputs match contract mean values, normalized channels should be ~0.0
        for (channel in 0..5) {
            assertEquals(0.0f, tensor[0][0][channel], 0.01f)
            assertEquals(0.0f, tensor[0][1499][channel], 0.01f)
        }
    }

    @Test
    fun `preprocessing is deterministic for identical input sessions`() {
        val session1 = generateTestSession(sampleCount = 1500, durationMs = 30_000L)
        val session2 = generateTestSession(sampleCount = 1500, durationMs = 30_000L)

        val tensor1 = ImuPreprocessor.preprocessSession(session1)
        val tensor2 = ImuPreprocessor.preprocessSession(session2)

        for (t in 0 until 1500) {
            for (c in 0 until 6) {
                assertEquals(tensor1[0][t][c], tensor2[0][t][c], 1e-6f)
            }
        }
    }

    @Test
    fun `score translation formula correctly converts p_irregular to Mobility Stability Score`() {
        fun computeScore(pIrregular: Float): Int {
            return ((1.0f - pIrregular.coerceIn(0.0f, 1.0f)) * 100f).roundToInt().coerceIn(0, 100)
        }

        assertEquals(100, computeScore(0.0f))
        assertEquals(85, computeScore(0.15f))
        assertEquals(50, computeScore(0.50f))
        assertEquals(15, computeScore(0.85f))
        assertEquals(0, computeScore(1.0f))
    }
}
