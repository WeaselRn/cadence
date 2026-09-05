package com.cadence.gaitradar.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadence.gaitradar.core.storage.OnboardingPreferencesRepository
import com.cadence.gaitradar.core.storage.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val profile: UserProfile = UserProfile(),
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val ageOrDobError: String? = null,
    val heightError: String? = null,
    val isSaved: Boolean = false
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val preferencesRepository: OnboardingPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val currentProfile = preferencesRepository.userProfile.first()
            _uiState.update { it.copy(profile = currentProfile) }
        }
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

    fun saveProfile(onSuccess: () -> Unit) {
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
            viewModelScope.launch {
                preferencesRepository.updateProfile(profile)
                _uiState.update { it.copy(isSaved = true) }
                onSuccess()
            }
        }
    }
}
