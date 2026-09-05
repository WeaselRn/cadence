package com.cadence.gaitradar.core.baseline

import com.cadence.gaitradar.core.database.AssessmentEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

interface PersonalBaselineEngine {
    fun computeBaseline(priorAssessments: List<AssessmentEntity>): BaselineStats
    fun evaluateComparison(
        currentScore: Int,
        baselineStats: BaselineStats,
        priorConsecutiveDeviations: Int
    ): BaselineComparison
}

@Singleton
class PersonalBaselineEngineImpl @Inject constructor() : PersonalBaselineEngine {

    override fun computeBaseline(priorAssessments: List<AssessmentEntity>): BaselineStats {
        val count = priorAssessments.size
        if (count < BaselineConfig.MINIMUM_BASELINE_WALKS) {
            return BaselineStats(
                sampleCount = count,
                isEstablished = false,
                meanScore = 0f,
                stdScore = 0f
            )
        }

        val scores = priorAssessments.map { it.mobilityStabilityScore }
        val meanScore = scores.average().toFloat()

        var varSum = 0.0
        for (score in scores) {
            varSum += (score - meanScore).toDouble().pow(2.0)
        }
        val rawStd = sqrt(varSum / count).toFloat()
        val stdScore = rawStd.coerceAtLeast(BaselineConfig.MIN_STD_DEV)

        val cadences = priorAssessments.mapNotNull { it.cadence }
        val meanCadence = if (cadences.isNotEmpty()) cadences.average().toFloat() else null

        val stepVars = priorAssessments.mapNotNull { it.stepTimeVariabilityMs }
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
        baselineStats: BaselineStats,
        priorConsecutiveDeviations: Int
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

        return if (isSubstantialDeviation) {
            val consecutive = priorConsecutiveDeviations + 1
            val status = if (consecutive >= BaselineConfig.PERSISTENT_DEVIATION_COUNT) {
                LongitudinalStatus.PERSISTENT_CHANGE
            } else {
                LongitudinalStatus.CHANGE_DETECTED
            }

            BaselineComparison(
                status = status,
                scoreDelta = delta,
                scoreZScore = zScore,
                consecutiveDeviations = consecutive
            )
        } else {
            BaselineComparison(
                status = LongitudinalStatus.STABLE,
                scoreDelta = delta,
                scoreZScore = zScore,
                consecutiveDeviations = 0
            )
        }
    }
}
