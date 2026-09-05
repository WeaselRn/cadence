package com.cadence.gaitradar.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AssessmentIntro : Screen("assessment_intro")
    data object AssessmentReadiness : Screen("assessment_readiness")
    data object AssessmentActive : Screen("assessment_active")
    data object AssessmentSummary : Screen("assessment_summary")
    data object History : Screen("history")
    data object AssessmentDetail : Screen("assessment_detail/{assessmentId}") {
        fun createRoute(assessmentId: String) = "assessment_detail/$assessmentId"
    }
    data object Profile : Screen("profile")
    data object EditProfile : Screen("edit_profile")
    data object PrivacyData : Screen("privacy_data")
    data object Settings : Screen("settings")
    data object About : Screen("about")

    // Onboarding routes
    data object Welcome : Screen("onboarding_welcome")
    data object HowItWorks : Screen("onboarding_how_it_works")
    data object Privacy : Screen("onboarding_privacy")
    data object PersonalInfo : Screen("onboarding_personal_info")
    data object SensorExplanation : Screen("onboarding_sensor_explanation")
}
