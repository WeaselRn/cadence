package com.cadence.gaitradar.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadence.gaitradar.core.database.AssessmentEntity
import com.cadence.gaitradar.core.database.AssessmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    assessmentRepository: AssessmentRepository
) : ViewModel() {
    val assessments: StateFlow<List<AssessmentEntity>> = assessmentRepository.assessments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
