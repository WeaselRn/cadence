package com.cadence.gaitradar.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadence.gaitradar.core.baseline.PersonalBaselineEngine
import com.cadence.gaitradar.core.database.AssessmentRepository
import com.cadence.gaitradar.core.storage.OnboardingPreferencesRepository
import com.cadence.gaitradar.core.storage.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProfileBaselineUiState(
    val completedAssessmentCount: Int = 0,
    val baselineStatusText: String = "Building Baseline (0/3)"
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    preferencesRepository: OnboardingPreferencesRepository,
    assessmentRepository: AssessmentRepository,
    baselineEngine: PersonalBaselineEngine
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = preferencesRepository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    val baselineUiState: StateFlow<ProfileBaselineUiState> = assessmentRepository.assessments
        .map { assessments ->
            val count = assessments.size
            val stats = baselineEngine.computeBaseline(assessments)
            val statusText = if (stats.isEstablished) {
                "Established"
            } else {
                "Building Baseline ($count/3)"
            }
            ProfileBaselineUiState(
                completedAssessmentCount = count,
                baselineStatusText = statusText
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileBaselineUiState()
        )
}
