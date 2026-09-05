package com.cadence.gaitradar.core.baseline

import com.cadence.gaitradar.core.database.AssessmentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PersonalBaselineEngineTest {

    private val engine = PersonalBaselineEngineImpl()

    private fun createAssessment(score: Int, timestampMs: Long): AssessmentEntity {
        return AssessmentEntity(
            id = UUID.randomUUID().toString(),
            sessionId = UUID.randomUUID().toString(),
            timestampMs = timestampMs,
            durationMs = 30_000L,
            mobilityStabilityScore = score,
            pIrregular = (100 - score) / 100f,
            modelVersion = "1.0.0",
            stepCount = 30,
            cadence = 60.0f,
            meanStepIntervalMs = 1000f,
            stepTimeVariabilityMs = 15f,
            accelVariability = 1.2f,
            gyroVariability = 0.5f,
            consecutiveDeviations = 0
        )
    }

    @Test
    fun `computeBaseline with less than 3 walks is not established`() {
        val priorWalks = listOf(
            createAssessment(85, 1000L),
            createAssessment(88, 2000L)
        )

        val stats = engine.computeBaseline(priorWalks)

        assertFalse(stats.isEstablished)
        assertEquals(2, stats.sampleCount)

        val comparison = engine.evaluateComparison(currentScore = 87, baselineStats = stats, priorConsecutiveDeviations = 0)
        assertEquals(LongitudinalStatus.BUILDING_BASELINE, comparison.status)
    }

    @Test
    fun `computeBaseline with 3 or more walks is established`() {
        val priorWalks = listOf(
            createAssessment(85, 1000L),
            createAssessment(90, 2000L),
            createAssessment(95, 3000L)
        )

        val stats = engine.computeBaseline(priorWalks)

        assertTrue(stats.isEstablished)
        assertEquals(3, stats.sampleCount)
        assertEquals(90.0f, stats.meanScore, 0.1f)
        assertTrue(stats.stdScore >= 1.0f)
    }

    @Test
    fun `stable walk evaluates to STABLE and resets consecutive deviations`() {
        val priorWalks = listOf(
            createAssessment(88, 1000L),
            createAssessment(90, 2000L),
            createAssessment(92, 3000L)
        )
        val stats = engine.computeBaseline(priorWalks)

        val comparison = engine.evaluateComparison(
            currentScore = 89,
            baselineStats = stats,
            priorConsecutiveDeviations = 1
        )

        assertEquals(LongitudinalStatus.STABLE, comparison.status)
        assertEquals(0, comparison.consecutiveDeviations)
        assertNotNull(comparison.scoreDelta)
    }

    @Test
    fun `substantial score drop evaluates to CHANGE_DETECTED`() {
        val priorWalks = listOf(
            createAssessment(90, 1000L),
            createAssessment(90, 2000L),
            createAssessment(90, 3000L)
        )
        val stats = engine.computeBaseline(priorWalks) // mean = 90

        val comparison = engine.evaluateComparison(
            currentScore = 70, // 20 points drop
            baselineStats = stats,
            priorConsecutiveDeviations = 0
        )

        assertEquals(LongitudinalStatus.CHANGE_DETECTED, comparison.status)
        assertEquals(1, comparison.consecutiveDeviations)
    }

    @Test
    fun `three consecutive deviations trigger PERSISTENT_CHANGE`() {
        val priorWalks = listOf(
            createAssessment(90, 1000L),
            createAssessment(90, 2000L),
            createAssessment(90, 3000L)
        )
        val stats = engine.computeBaseline(priorWalks)

        val comparison = engine.evaluateComparison(
            currentScore = 65,
            baselineStats = stats,
            priorConsecutiveDeviations = 2 // already 2 prior deviations
        )

        assertEquals(LongitudinalStatus.PERSISTENT_CHANGE, comparison.status)
        assertEquals(3, comparison.consecutiveDeviations)
    }

    @Test
    fun `zero variance in baseline is handled safely with min std threshold`() {
        val priorWalks = listOf(
            createAssessment(90, 1000L),
            createAssessment(90, 2000L),
            createAssessment(90, 3000L)
        )
        val stats = engine.computeBaseline(priorWalks)

        assertEquals(1.0f, stats.stdScore, 0.01f) // MIN_STD_DEV

        val comparison = engine.evaluateComparison(
            currentScore = 90,
            baselineStats = stats,
            priorConsecutiveDeviations = 0
        )

        assertEquals(LongitudinalStatus.STABLE, comparison.status)
        assertFalse(comparison.scoreZScore!!.isNaN())
        assertFalse(comparison.scoreZScore!!.isInfinite())
    }

    @Test
    fun `stable walk recovers from persistent change back to STABLE`() {
        val priorWalks = listOf(
            createAssessment(90, 1000L),
            createAssessment(90, 2000L),
            createAssessment(90, 3000L)
        )
        val stats = engine.computeBaseline(priorWalks)

        val comparison = engine.evaluateComparison(
            currentScore = 90,
            baselineStats = stats,
            priorConsecutiveDeviations = 3
        )

        assertEquals(LongitudinalStatus.STABLE, comparison.status)
        assertEquals(0, comparison.consecutiveDeviations)
    }
}
