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
import com.cadence.gaitradar.feature.about.AboutScreen
import com.cadence.gaitradar.feature.assessment.AssessmentIntroScreen
import com.cadence.gaitradar.feature.history.HistoryScreen
import com.cadence.gaitradar.feature.home.HomeScreen
import com.cadence.gaitradar.feature.onboarding.HowItWorksScreen
import com.cadence.gaitradar.feature.onboarding.OnboardingViewModel
import com.cadence.gaitradar.feature.onboarding.PersonalInformationScreen
import com.cadence.gaitradar.feature.onboarding.PrivacyIntroScreen
import com.cadence.gaitradar.feature.onboarding.SensorExplanationScreen
import com.cadence.gaitradar.feature.onboarding.WelcomeScreen
import com.cadence.gaitradar.feature.privacy.PrivacyDataScreen
import com.cadence.gaitradar.feature.profile.EditProfileScreen
import com.cadence.gaitradar.feature.profile.ProfileScreen
import com.cadence.gaitradar.feature.settings.SettingsScreen

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
        // Onboarding flow
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

        // Main App Shell Flow
        composable(route = Screen.Home.route) {
            HomeScreen(
                onStartAssessment = {
                    navController.navigate(Screen.AssessmentIntro.route)
                },
                onNavigateProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateAbout = {
                    navController.navigate(Screen.About.route)
                }
            )
        }

        composable(route = Screen.AssessmentIntro.route) {
            AssessmentIntroScreen(
                onGetReady = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Profile.route) {
            ProfileScreen(
                onEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                }
            )
        }

        composable(route = Screen.EditProfile.route) {
            EditProfileScreen(
                onSaved = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.History.route) {
            HistoryScreen(
                onStartAssessment = {
                    navController.navigate(Screen.AssessmentIntro.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.PrivacyData.route) {
            PrivacyDataScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigatePrivacyData = {
                    navController.navigate(Screen.PrivacyData.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.About.route) {
            AboutScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
