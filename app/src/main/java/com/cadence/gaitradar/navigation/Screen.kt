package com.cadence.gaitradar.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Welcome : Screen("onboarding_welcome")
    data object HowItWorks : Screen("onboarding_how_it_works")
    data object Privacy : Screen("onboarding_privacy")
    data object PersonalInfo : Screen("onboarding_personal_info")
    data object SensorExplanation : Screen("onboarding_sensor_explanation")

    // Future routes
    data object Assessment : Screen("assessment")
    data object Results : Screen("results")
    data object History : Screen("history")
    data object Profile : Screen("profile")
    data object PrivacySettings : Screen("privacy_settings")
    data object Settings : Screen("settings")
}
