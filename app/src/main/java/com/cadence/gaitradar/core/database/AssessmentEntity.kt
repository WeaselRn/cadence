package com.cadence.gaitradar.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assessments")
data class AssessmentEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val timestampMs: Long,
    val durationMs: Long,
    val mobilityStabilityScore: Int,
    val pIrregular: Float,
    val modelVersion: String,
    val stepCount: Int?,
    val cadence: Float?,
    val meanStepIntervalMs: Float?,
    val stepTimeVariabilityMs: Float?,
    val accelVariability: Float?,
    val gyroVariability: Float?
)
