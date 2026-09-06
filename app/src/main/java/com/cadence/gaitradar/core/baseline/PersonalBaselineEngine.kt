package com.cadence.gaitradar.core.baseline

import com.cadence.gaitradar.core.database.AssessmentEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

interface PersonalBaselineEngine {
    /**
     * Computes baseline statistics strictly from the first 3 valid assessments (chronologically sorted).
     */
    fun computeBaseline(allAssessments: List<AssessmentEntity>): BaselineStats

    /**
     * Evaluates a current assessment score against established baseline and calculates
     * consecutive deviations dynamically from historical assessment records.
     */
    fun evaluateComparison(
        currentScore: Int,
        currentTimestampMs: Long = System.currentTimeMillis(),
        allAssessments: List<AssessmentEntity>,
        baselineStats: BaselineStats
    ): BaselineComparison
}

@Singleton
class PersonalBaselineEngineImpl @Inject constructor() : PersonalBaselineEngine {

    override fun computeBaseline(allAssessments: List<AssessmentEntity>): BaselineStats {
        val sorted = allAssessments.sortedBy { it.timestampMs }
        val count = sorted.size

        if (count < BaselineConfig.MINIMUM_BASELINE_WALKS) {
            return BaselineStats(
                sampleCount = count,
                isEstablished = false,
                meanScore = 0f,
                stdScore = 0f
            )
        }

        // The baseline is established strictly from the FIRST 3 valid assessments
        val baselineWalks = sorted.take(BaselineConfig.MINIMUM_BASELINE_WALKS)
        val scores = baselineWalks.map { it.mobilityStabilityScore }
        val meanScore = scores.average().toFloat()

        var varSum = 0.0
        for (score in scores) {
            varSum += (score - meanScore).toDouble().pow(2.0)
        }
        val rawStd = sqrt(varSum / baselineWalks.size).toFloat()
        val stdScore = rawStd.coerceAtLeast(BaselineConfig.MIN_STD_DEV)

        val cadences = baselineWalks.mapNotNull { it.cadence }
        val meanCadence = if (cadences.isNotEmpty()) cadences.average().toFloat() else null

        val stepVars = baselineWalks.mapNotNull { it.stepTimeVariabilityMs }
        val meanStepVar = if (stepVars.isNotEmpty()) stepVars.average().toFloat() else null

        return BaselineStats(
            sampleCount = count,
            isEstablished = true,
            meanScore = meanScore,
            stdScore = stdScore,
            meanCadence = meanCadence,
            meanStepTimeVar = meanStepVar
        )
    }

    override fun evaluateComparison(
        currentScore: Int,
        currentTimestampMs: Long,
        allAssessments: List<AssessmentEntity>,
        baselineStats: BaselineStats
    ): BaselineComparison {
        if (!baselineStats.isEstablished) {
            return BaselineComparison(
                status = LongitudinalStatus.BUILDING_BASELINE,
                consecutiveDeviations = 0,
                statusMessage = "Building personal baseline: ${baselineStats.sampleCount} / ${BaselineConfig.MINIMUM_BASELINE_WALKS} walks completed."
            )
        }

        val delta = currentScore - baselineStats.meanScore
        val zScore = delta / baselineStats.stdScore

        val isSubstantialDeviation = zScore <= BaselineConfig.DEVIATION_Z_THRESHOLD || delta <= -BaselineConfig.MIN_SCORE_DROP_POINTS

        if (!isSubstantialDeviation) {
            return BaselineComparison(
                status = LongitudinalStatus.STABLE,
                scoreDelta = delta,
                scoreZScore = zScore,
                consecutiveDeviations = 0
            )
        }

        // Calculate consecutive deviations dynamically by inspecting preceding post-baseline assessments backward in time
        val sortedAsc = allAssessments.sortedBy { it.timestampMs }
        val postBaselinePreceding = sortedAsc
            .drop(BaselineConfig.MINIMUM_BASELINE_WALKS)
            .filter { it.timestampMs < currentTimestampMs }
            .reversed()

        var precedingConsecutive = 0
        for (priorAcc in postBaselinePreceding) {
            val priorDelta = priorAcc.mobilityStabilityScore - baselineStats.meanScore
            val priorZ = priorDelta / baselineStats.stdScore
            val priorIsDev = priorZ <= BaselineConfig.DEVIATION_Z_THRESHOLD || priorDelta <= -BaselineConfig.MIN_SCORE_DROP_POINTS

            if (priorIsDev) {
                precedingConsecutive++
            } else {
                break // Stop at the first non-deviation session
            }
        }

        val totalConsecutive = precedingConsecutive + 1
        val status = if (totalConsecutive >= BaselineConfig.PERSISTENT_DEVIATION_COUNT) {
            LongitudinalStatus.PERSISTENT_CHANGE
        } else {
            LongitudinalStatus.CHANGE_DETECTED
        }

        return BaselineComparison(
            status = status,
            scoreDelta = delta,
            scoreZScore = zScore,
            consecutiveDeviations = totalConsecutive
        )
    }
}
