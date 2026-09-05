package com.cadence.gaitradar.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cadence.gaitradar.feature.home.HomeScreen
import com.cadence.gaitradar.feature.onboarding.HowItWorksScreen
import com.cadence.gaitradar.feature.onboarding.OnboardingViewModel
import com.cadence.gaitradar.feature.onboarding.PersonalInformationScreen
import com.cadence.gaitradar.feature.onboarding.PrivacyIntroScreen
import com.cadence.gaitradar.feature.onboarding.SensorExplanationScreen
import com.cadence.gaitradar.feature.onboarding.WelcomeScreen

@Composable
fun CadenceNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val startDestination = if (uiState.isCompleted) {
        Screen.Home.route
    } else {
        Screen.Welcome.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = Screen.Welcome.route) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(Screen.Privacy.route)
                },
                onHowItWorks = {
                    navController.navigate(Screen.HowItWorks.route)
                }
            )
        }

        composable(route = Screen.HowItWorks.route) {
            HowItWorksScreen(
                page = uiState.howItWorksPage,
                onPageChanged = { page -> viewModel.onHowItWorksPageChanged(page) },
                onFinished = {
                    navController.navigate(Screen.Privacy.route) {
                        popUpTo(Screen.Welcome.route)
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Privacy.route) {
            PrivacyIntroScreen(
                onContinue = {
                    navController.navigate(Screen.PersonalInfo.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.PersonalInfo.route) {
            PersonalInformationScreen(
                profile = uiState.profile,
                firstNameError = uiState.firstNameError,
                lastNameError = uiState.lastNameError,
                ageOrDobError = uiState.ageOrDobError,
                heightError = uiState.heightError,
                onProfileChanged = { update -> viewModel.updateProfileField(update) },
                onContinue = {
                    viewModel.validateAndProceed {
                        navController.navigate(Screen.SensorExplanation.route)
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.SensorExplanation.route) {
            SensorExplanationScreen(
                onFinishOnboarding = {
                    viewModel.completeOnboarding()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Home.route) {
            HomeScreen()
        }
    }
}
