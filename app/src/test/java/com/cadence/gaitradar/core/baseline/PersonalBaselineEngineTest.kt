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
    fun `scenario 1 - no baseline yet`() {
        val walks = listOf(
            createAssessment(85, 1000L),
            createAssessment(88, 2000L)
        )
        val stats = engine.computeBaseline(walks)

        assertFalse(stats.isEstablished)
        assertEquals(2, stats.sampleCount)

        val comparison = engine.evaluateComparison(
            currentScore = 87,
            currentTimestampMs = 3000L,
            allAssessments = walks,
            baselineStats = stats
        )
        assertEquals(LongitudinalStatus.BUILDING_BASELINE, comparison.status)
        assertEquals(0, comparison.consecutiveDeviations)
    }

    @Test
    fun `scenario 7 - exactly 3 baseline assessments`() {
        val walks = listOf(
            createAssessment(85, 1000L),
            createAssessment(90, 2000L),
            createAssessment(95, 3000L)
        )
        val stats = engine.computeBaseline(walks)

        assertTrue(stats.isEstablished)
        assertEquals(3, stats.sampleCount)
        assertEquals(90.0f, stats.meanScore, 0.1f)
    }

    @Test
    fun `scenario 8 - assessment 4 compared against fixed baseline`() {
        val walks = listOf(
            createAssessment(90, 1000L),
            createAssessment(90, 2000L),
            createAssessment(90, 3000L),
            createAssessment(90, 4000L)
        )
        val stats = engine.computeBaseline(walks)

        // Baseline is strictly computed from first 3 walks
        assertEquals(90.0f, stats.meanScore, 0.1f)
        assertEquals(4, stats.sampleCount)

        val comparison = engine.evaluateComparison(
            currentScore = 90,
            currentTimestampMs = 4000L,
            allAssessments = walks,
            baselineStats = stats
        )
        assertEquals(LongitudinalStatus.STABLE, comparison.status)
        assertEquals(0, comparison.consecutiveDeviations)
    }

    @Test
    fun `scenario 2 - first deviation`() {
        val walks = listOf(
            createAssessment(90, 1000L),
            createAssessment(90, 2000L),
            createAssessment(90, 3000L),
            createAssessment(70, 4000L) // deviation
        )
        val stats = engine.computeBaseline(walks)

        val comparison = engine.evaluateComparison(
            currentScore = 70,
            currentTimestampMs = 4000L,
            allAssessments = walks,
            baselineStats = stats
        )

        assertEquals(LongitudinalStatus.CHANGE_DETECTED, comparison.status)
        assertEquals(1, comparison.consecutiveDeviations)
    }

    @Test
    fun `scenario 3 - two consecutive deviations`() {
        val walks = listOf(
            createAssessment(90, 1000L),
            createAssessment(90, 2000L),
            createAssessment(90, 3000L),
            createAssessment(70, 4000L), // dev 1
            createAssessment(70, 5000L)  // dev 2
        )
        val stats = engine.computeBaseline(walks)

        val comparison = engine.evaluateComparison(
            currentScore = 70,
            currentTimestampMs = 5000L,
            allAssessments = walks,
            baselineStats = stats
        )

        assertEquals(LongitudinalStatus.CHANGE_DETECTED, comparison.status)
        assertEquals(2, comparison.consecutiveDeviations)
    }

    @Test
    fun `scenario 4 - three consecutive deviations trigger PERSISTENT_CHANGE`() {
        val walks = listOf(
            createAssessment(90, 1000L),
            createAssessment(90, 2000L),
            createAssessment(90, 3000L),
            createAssessment(70, 4000L), // dev 1
            createAssessment(70, 5000L), // dev 2
            createAssessment(70, 6000L)  // dev 3
        )
        val stats = engine.computeBaseline(walks)

        val comparison = engine.evaluateComparison(
            currentScore = 70,
            currentTimestampMs = 6000L,
            allAssessments = walks,
            baselineStats = stats
        )

        assertEquals(LongitudinalStatus.PERSISTENT_CHANGE, comparison.status)
        assertEquals(3, comparison.consecutiveDeviations)
    }

    @Test
    fun `scenario 5 - deviation interrupted by a normal assessment`() {
        val walks = listOf(
            createAssessment(90, 1000L),
            createAssessment(90, 2000L),
            createAssessment(90, 3000L),
            createAssessment(70, 4000L), // dev 1
            createAssessment(70, 5000L), // dev 2
            createAssessment(90, 6000L), // normal (interruption)
            createAssessment(70, 7000L)  // dev 1 again
        )
        val stats = engine.computeBaseline(walks)

        val comparison = engine.evaluateComparison(
            currentScore = 70,
            currentTimestampMs = 7000L,
            allAssessments = walks,
            baselineStats = stats
        )

        // Previous normal walk at 6000L stops count accumulation, so current is count = 1
        assertEquals(LongitudinalStatus.CHANGE_DETECTED, comparison.status)
        assertEquals(1, comparison.consecutiveDeviations)
    }

    @Test
    fun `scenario 6 - normal assessment after deviations resets count to zero`() {
        val walks = listOf(
            createAssessment(90, 1000L),
            createAssessment(90, 2000L),
            createAssessment(90, 3000L),
            createAssessment(70, 4000L), // dev 1
            createAssessment(70, 5000L), // dev 2
            createAssessment(70, 6000L), // dev 3
            createAssessment(90, 7000L)  // normal (recovery)
        )
        val stats = engine.computeBaseline(walks)

        val comparison = engine.evaluateComparison(
            currentScore = 90,
            currentTimestampMs = 7000L,
            allAssessments = walks,
            baselineStats = stats
        )

        assertEquals(LongitudinalStatus.STABLE, comparison.status)
        assertEquals(0, comparison.consecutiveDeviations)
    }

    @Test
    fun `baseline statistics do not shift when additional walks 4 and 5 are added`() {
        val walksInitial = listOf(
            createAssessment(80, 1000L),
            createAssessment(90, 2000L),
            createAssessment(100, 3000L)
        )
        val statsInitial = engine.computeBaseline(walksInitial) // mean = 90

        val walksExpanded = walksInitial + listOf(
            createAssessment(50, 4000L),
            createAssessment(40, 5000L)
        )
        val statsExpanded = engine.computeBaseline(walksExpanded)

        // Baseline mean remains 90 because it is calculated strictly from the first 3 walks
        assertEquals(90.0f, statsInitial.meanScore, 0.01f)
        assertEquals(90.0f, statsExpanded.meanScore, 0.01f)
    }
}
