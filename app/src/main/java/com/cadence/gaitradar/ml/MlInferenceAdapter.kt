package com.cadence.gaitradar.ml

/**
 * Abstraction layer for machine learning inference.
 * Keeps UI, view models, and repositories completely decoupled from TensorFlow Lite.
 * TFLite dependencies and concrete implementation will be added in the dedicated ML phase.
 */
interface MlInferenceAdapter {
    /**
     * Executes inference on preprocessed IMU sensor feature windows.
     *
     * @param featureData Array of windowed sensor features (e.g. Accelerometer/Gyroscope time series)
     * @return Result containing inference score / embedding output
     */
    suspend fun runInference(featureData: FloatArray): Result<FloatArray>
}
