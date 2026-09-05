package com.cadence.gaitradar.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadence.gaitradar.core.storage.AppSettings
import com.cadence.gaitradar.core.storage.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val appSettings: StateFlow<AppSettings> = settingsRepository.appSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun updateLargerText(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateLargerText(enabled)
        }
    }

    fun updateReducedMotion(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateReducedMotion(enabled)
        }
    }

    fun updateVoiceInstructions(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateVoiceInstructions(enabled)
        }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateHapticFeedback(enabled)
        }
    }

    fun updateAssessmentReminders(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAssessmentReminders(enabled)
        }
    }

    fun updateAutoDeleteSensors(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoDeleteSensors(enabled)
        }
    }
}
