package com.cadence.gaitradar.feature.assessment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadence.gaitradar.core.quality.ImuQualityGate
import com.cadence.gaitradar.core.quality.QualityResult
import com.cadence.gaitradar.core.sensors.ImuSample
import com.cadence.gaitradar.core.sensors.ImuSession
import com.cadence.gaitradar.core.sensors.SensorCollector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class SessionStatus {
    IDLE,
    READINESS_CHECK,
    COLLECTING,
    CANCELLED,
    COMPLETED,
    ERROR
}

data class AssessmentUiState(
    val isAccelAvailable: Boolean = false,
    val isGyroAvailable: Boolean = false,
    val isSensorsAvailable: Boolean = false,
    val sessionStatus: SessionStatus = SessionStatus.IDLE,
    val remainingSeconds: Int = 30,
    val sampleCount: Int = 0,
    val lastSample: ImuSample? = null,
    val completedSession: ImuSession? = null,
    val qualityResult: QualityResult? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AssessmentViewModel @Inject constructor(
    private val sensorCollector: SensorCollector,
    private val qualityGate: ImuQualityGate
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssessmentUiState())
    val uiState: StateFlow<AssessmentUiState> = _uiState.asStateFlow()

    private var collectionJob: Job? = null
    private var timerJob: Job? = null
    private val samplesList = mutableListOf<ImuSample>()

    init {
        checkReadiness()
    }

    fun checkReadiness() {
        val accelOk = sensorCollector.isAccelerometerAvailable()
        val gyroOk = sensorCollector.isGyroscopeAvailable()
        val bothOk = sensorCollector.isSensorsAvailable()

        _uiState.update {
            it.copy(
                isAccelAvailable = accelOk,
                isGyroAvailable = gyroOk,
                isSensorsAvailable = bothOk,
                sessionStatus = if (bothOk) SessionStatus.READINESS_CHECK else SessionStatus.ERROR,
                errorMessage = if (!bothOk) "Required accelerometer or gyroscope sensor is not available on this device." else null
            )
        }
    }

    fun start30sCollection() {
        if (!_uiState.value.isSensorsAvailable) return

        collectionJob?.cancel()
        timerJob?.cancel()
        samplesList.clear()

        val sessionId = UUID.randomUUID().toString()
        val startTimeMs = System.currentTimeMillis()

        _uiState.update {
            it.copy(
                sessionStatus = SessionStatus.COLLECTING,
                remainingSeconds = 30,
                sampleCount = 0,
                lastSample = null,
                completedSession = null,
                qualityResult = null,
                errorMessage = null
            )
        }

        // Start Sensor Collection Flow
        collectionJob = viewModelScope.launch {
            sensorCollector.startCollection().collect { sample ->
                samplesList.add(sample)
                _uiState.update {
                    it.copy(
                        sampleCount = samplesList.size,
                        lastSample = sample
                    )
                }
            }
        }

        // Start 30-Second Countdown Timer
        timerJob = viewModelScope.launch {
            for (sec in 29 downTo 0) {
                delay(1000L)
                _uiState.update { it.copy(remainingSeconds = sec) }
            }
            finishCollection(sessionId, startTimeMs)
        }
    }

    private fun finishCollection(sessionId: String, startTimeMs: Long) {
        collectionJob?.cancel()
        timerJob?.cancel()

        val endTimeMs = System.currentTimeMillis()
        val durationMs = (endTimeMs - startTimeMs).coerceAtLeast(1000L)
        val sampleCount = samplesList.size
        val avgHz = (sampleCount.toFloat() / (durationMs / 1000f))

        val session = ImuSession(
            sessionId = sessionId,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            durationMs = durationMs,
            samples = samplesList.toList(),
            accelEventCount = sampleCount,
            gyroEventCount = sampleCount,
            averageSamplingRateHz = avgHz
        )

        // Evaluate Deterministic IMU Quality Gate
        val qualityEval = qualityGate.evaluate(session)

        _uiState.update {
            it.copy(
                sessionStatus = SessionStatus.COMPLETED,
                completedSession = session,
                qualityResult = qualityEval
            )
        }
    }

    fun cancelCollection() {
        collectionJob?.cancel()
        timerJob?.cancel()
        sensorCollector.stopCollection()

        _uiState.update {
            it.copy(
                sessionStatus = SessionStatus.CANCELLED,
                remainingSeconds = 30
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelCollection()
    }
}
