package com.cadence.gaitradar.feature.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadence.gaitradar.core.database.AssessmentRepository
import com.cadence.gaitradar.core.storage.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacyDataViewModel @Inject constructor(
    private val assessmentRepository: AssessmentRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    fun deleteAllAssessmentData(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            assessmentRepository.deleteAllAssessments()
            onSuccess()
        }
    }

    fun deleteAllLocalApplicationData(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            assessmentRepository.deleteAllAssessments()
            settingsRepository.clearAllLocalData()
            onSuccess()
        }
    }
}
