package com.cadence.gaitradar.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadence.gaitradar.core.database.AssessmentEntity
import com.cadence.gaitradar.core.database.AssessmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssessmentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val assessmentRepository: AssessmentRepository
) : ViewModel() {

    private val assessmentId: String = checkNotNull(savedStateHandle["assessmentId"])

    val assessment: StateFlow<AssessmentEntity?> = assessmentRepository
        .getAssessmentById(assessmentId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun deleteAssessment(onDeleted: () -> Unit) {
        viewModelScope.launch {
            assessmentRepository.deleteAssessment(assessmentId)
            onDeleted()
        }
    }
}
