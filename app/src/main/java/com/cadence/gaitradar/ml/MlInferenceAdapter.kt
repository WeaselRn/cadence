package com.cadence.gaitradar.ml

import com.cadence.gaitradar.core.sensors.ImuSession

/**
 * Structured ML inference result from the trained 1D TCN TFLite model.
 */
data class MlPrediction(
    val pIrregular: Float,
    val mobilityStabilityScore: Int,
    val modelVersion: String = "1.0.0",
    val inferenceTimeMs: Long = 0L,
    val inputShape: IntArray = intArrayOf(1, 1500, 6),
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MlPrediction
        if (pIrregular != other.pIrregular) return false
        if (mobilityStabilityScore != other.mobilityStabilityScore) return false
        if (modelVersion != other.modelVersion) return false
        if (inferenceTimeMs != other.inferenceTimeMs) return false
        if (!inputShape.contentEquals(other.inputShape)) return false
        if (isSuccess != other.isSuccess) return false
        if (errorMessage != other.errorMessage) return false
        return true
    }

    override fun hashCode(): Int {
        var result = pIrregular.hashCode()
        result = 31 * result + mobilityStabilityScore
        result = 31 * result + modelVersion.hashCode()
        result = 31 * result + inferenceTimeMs.hashCode()
        result = 31 * result + inputShape.contentHashCode()
        result = 31 * result + isSuccess.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        return result
    }
}

/**
 * Abstraction layer for machine learning inference.
 * Keeps UI, view models, and repositories completely decoupled from TensorFlow Lite.
 */
interface MlInferenceAdapter {
    /**
     * Executes preprocessing and TFLite model inference on a quality-passed ImuSession.
     */
    suspend fun predict(session: ImuSession): MlPrediction
}
