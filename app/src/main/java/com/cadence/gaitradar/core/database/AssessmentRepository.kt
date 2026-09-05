package com.cadence.gaitradar.core.database

import com.cadence.gaitradar.core.metrics.GaitMetricsResult
import com.cadence.gaitradar.core.sensors.ImuSession
import com.cadence.gaitradar.ml.MlPrediction
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssessmentRepository @Inject constructor(
    private val assessmentDao: AssessmentDao
) {
    val assessments: Flow<List<AssessmentEntity>> = assessmentDao.getAllAssessments()

    suspend fun saveAssessment(
        session: ImuSession,
        gaitMetrics: GaitMetricsResult?,
        mlPrediction: MlPrediction,
        consecutiveDeviations: Int = 0
    ) {
        val score = mlPrediction.mobilityStabilityScore ?: return
        val pIrr = mlPrediction.pIrregular ?: 0f
        val entity = AssessmentEntity(
            id = UUID.randomUUID().toString(),
            sessionId = session.sessionId,
            timestampMs = session.endTimeMs,
            durationMs = session.durationMs,
            mobilityStabilityScore = score,
            pIrregular = pIrr,
            modelVersion = mlPrediction.modelVersion,
            stepCount = gaitMetrics?.stepCount,
            cadence = gaitMetrics?.cadenceStepsPerMin,
            meanStepIntervalMs = gaitMetrics?.meanStepIntervalMs,
            stepTimeVariabilityMs = gaitMetrics?.stepTimeVariabilityMs,
            accelVariability = gaitMetrics?.accelVariability,
            gyroVariability = gaitMetrics?.gyroVariability,
            consecutiveDeviations = consecutiveDeviations
        )
        assessmentDao.insertAssessment(entity)
    }

    fun getAssessmentById(id: String): Flow<AssessmentEntity?> {
        return assessmentDao.getAssessmentById(id)
    }

    suspend fun deleteAssessment(id: String) {
        assessmentDao.deleteAssessment(id)
    }

    suspend fun deleteAllAssessments() {
        assessmentDao.deleteAllAssessments()
    }
}
