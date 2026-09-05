package com.cadence.gaitradar.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadence.gaitradar.core.storage.OnboardingPreferencesRepository
import com.cadence.gaitradar.core.storage.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: OnboardingPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.hasCompletedOnboarding.collect { completed ->
                if (completed) {
                    _uiState.update { it.copy(isCompleted = true) }
                }
            }
        }
    }

    fun onStepChanged(step: OnboardingStep) {
        _uiState.update { it.copy(currentStep = step) }
    }

    fun onHowItWorksPageChanged(page: Int) {
        _uiState.update { it.copy(howItWorksPage = page) }
    }

    fun updateProfileField(update: UserProfile.() -> UserProfile) {
        _uiState.update { state ->
            state.copy(
                profile = state.profile.update(),
                firstNameError = null,
                lastNameError = null,
                ageOrDobError = null,
                heightError = null
            )
        }
    }

    fun validateAndProceed(onSuccess: () -> Unit) {
        val profile = _uiState.value.profile
        var isValid = true

        val firstNameErr = if (profile.firstName.isBlank()) "First name is required" else null
        val lastNameErr = if (profile.lastName.isBlank()) "Last name is required" else null
        val ageErr = if (profile.ageOrDob.isBlank()) "Age or Date of Birth is required" else null

        val heightValue = profile.heightCm.toFloatOrNull()
        val heightErr = when {
            profile.heightCm.isBlank() -> "Height is required"
            heightValue == null || heightValue <= 0f || heightValue > 300f -> "Enter a valid height in cm (e.g., 170)"
            else -> null
        }

        if (firstNameErr != null || lastNameErr != null || ageErr != null || heightErr != null) {
            isValid = false
        }

        _uiState.update {
            it.copy(
                firstNameError = firstNameErr,
                lastNameError = lastNameErr,
                ageOrDobError = ageErr,
                heightError = heightErr
            )
        }

        if (isValid) {
            onSuccess()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            preferencesRepository.saveProfileAndCompleteOnboarding(_uiState.value.profile)
            _uiState.update { it.copy(isLoading = false, isCompleted = true) }
        }
    }
}
