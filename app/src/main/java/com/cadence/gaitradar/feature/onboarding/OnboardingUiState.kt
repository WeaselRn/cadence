package com.cadence.gaitradar.feature.onboarding

import com.cadence.gaitradar.core.storage.UserProfile

enum class OnboardingStep {
    WELCOME,
    HOW_IT_WORKS,
    PRIVACY,
    PERSONAL_INFO,
    SENSOR_EXPLANATION
}

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val howItWorksPage: Int = 0,
    val profile: UserProfile = UserProfile(),
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val ageOrDobError: String? = null,
    val heightError: String? = null,
    val isLoading: Boolean = false,
    val isCompleted: Boolean = false
)
