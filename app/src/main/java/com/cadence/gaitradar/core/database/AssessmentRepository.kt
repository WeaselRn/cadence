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
        mlPrediction: MlPrediction
    ) {
        val entity = AssessmentEntity(
            id = UUID.randomUUID().toString(),
            sessionId = session.sessionId,
            timestampMs = session.endTimeMs,
            durationMs = session.durationMs,
            mobilityStabilityScore = mlPrediction.mobilityStabilityScore,
            pIrregular = mlPrediction.pIrregular,
            modelVersion = mlPrediction.modelVersion,
            stepCount = gaitMetrics?.stepCount,
            cadence = gaitMetrics?.cadenceStepsPerMin,
            meanStepIntervalMs = gaitMetrics?.meanStepIntervalMs,
            stepTimeVariabilityMs = gaitMetrics?.stepTimeVariabilityMs,
            accelVariability = gaitMetrics?.accelVariability,
            gyroVariability = gaitMetrics?.gyroVariability
        )
        assessmentDao.insertAssessment(entity)
    }

    suspend fun deleteAssessment(id: String) {
        assessmentDao.deleteAssessment(id)
    }
}
