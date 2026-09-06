package com.cadence.gaitradar.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadence.gaitradar.core.baseline.BaselineComparison
import com.cadence.gaitradar.core.baseline.BaselineStats
import com.cadence.gaitradar.core.baseline.PersonalBaselineEngine
import com.cadence.gaitradar.core.database.AssessmentEntity
import com.cadence.gaitradar.core.database.AssessmentRepository
import com.cadence.gaitradar.core.storage.OnboardingPreferencesRepository
import com.cadence.gaitradar.core.storage.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeBaselineUiState(
    val assessmentCount: Int = 0,
    val latestAssessment: AssessmentEntity? = null,
    val baselineStats: BaselineStats? = null,
    val latestComparison: BaselineComparison? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
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

    val baselineUiState: StateFlow<HomeBaselineUiState> = assessmentRepository.assessments
        .map { assessments ->
            val count = assessments.size
            if (assessments.isEmpty()) {
                HomeBaselineUiState(assessmentCount = 0)
            } else {
                val sortedAsc = assessments.sortedBy { it.timestampMs }
                val latest = sortedAsc.last()

                val stats = baselineEngine.computeBaseline(sortedAsc)
                val comparison = baselineEngine.evaluateComparison(
                    currentScore = latest.mobilityStabilityScore,
                    currentTimestampMs = latest.timestampMs,
                    allAssessments = sortedAsc,
                    baselineStats = stats
                )

                HomeBaselineUiState(
                    assessmentCount = count,
                    latestAssessment = latest,
                    baselineStats = stats,
                    latestComparison = comparison
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeBaselineUiState()
        )
}
