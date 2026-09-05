package com.cadence.gaitradar.navigation

/**
 * Navigation routes for Gait Functional Decline Radar application.
 * All future feature routes are defined here to establish the navigation foundation.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Onboarding : Screen("onboarding")
    data object Assessment : Screen("assessment")
    data object Results : Screen("results")
    data object History : Screen("history")
    data object Profile : Screen("profile")
    data object Privacy : Screen("privacy")
    data object Settings : Screen("settings")
}
