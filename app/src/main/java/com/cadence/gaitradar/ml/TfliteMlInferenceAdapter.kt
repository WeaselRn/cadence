package com.cadence.gaitradar.ml

import android.content.Context
import com.cadence.gaitradar.core.sensors.ImuSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class TfliteMlInferenceAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : MlInferenceAdapter {

    private val modelAssetPath = "models/gait_tcn_quantized.tflite"

    private var interpreter: Interpreter? = null

    private fun getInterpreter(): Interpreter {
        if (interpreter == null) {
            val fileDescriptor = context.assets.openFd(modelAssetPath)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options()
            interpreter = Interpreter(mappedByteBuffer, options)
        }
        return interpreter!!
    }

    override suspend fun predict(session: ImuSession): MlPrediction = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        try {
            val tflite = getInterpreter()

            // Validate tensor metadata
            val inputTensor = tflite.getInputTensor(0)
            val outputTensor = tflite.getOutputTensor(0)

            val inputShape = inputTensor.shape() // Should be [1, 1500, 6]
            val outputShape = outputTensor.shape() // Should be [1, 1]

            // 1. Preprocess raw session into Float32 [1, 1500, 6]
            val inputArray = ImuPreprocessor.preprocessSession(session)

            // 2. Prepare output tensor buffer Float32 [1, 1]
            val outputArray = Array(1) { FloatArray(1) }

            // 3. Execute TFLite Model Inference
            tflite.run(inputArray, outputArray)

            val endTime = System.currentTimeMillis()
            val inferenceTimeMs = endTime - startTime

            val pIrregular = outputArray[0][0].coerceIn(0.0f, 1.0f)
            val score = ((1.0f - pIrregular) * 100f).roundToInt().coerceIn(0, 100)

            MlPrediction(
                pIrregular = pIrregular,
                mobilityStabilityScore = score,
                modelVersion = "1.0.0",
                inferenceTimeMs = inferenceTimeMs,
                inputShape = inputShape,
                isSuccess = true,
                errorMessage = null
            )
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            MlPrediction(
                pIrregular = 0.5f,
                mobilityStabilityScore = 50,
                modelVersion = "1.0.0",
                inferenceTimeMs = endTime - startTime,
                inputShape = intArrayOf(1, 1500, 6),
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "ML Inference failed"
            )
        }
    }
}
