package com.cadence.gaitradar.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cadence.gaitradar.feature.about.AboutScreen
import com.cadence.gaitradar.feature.assessment.AssessmentActiveScreen
import com.cadence.gaitradar.feature.assessment.AssessmentIntroScreen
import com.cadence.gaitradar.feature.assessment.AssessmentReadinessScreen
import com.cadence.gaitradar.feature.assessment.AssessmentSummaryScreen
import com.cadence.gaitradar.feature.assessment.AssessmentViewModel
import com.cadence.gaitradar.feature.assessment.SessionStatus
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
    onboardingViewModel: OnboardingViewModel = hiltViewModel()
) {
    val onboardingState by onboardingViewModel.uiState.collectAsState()

    val startDestination = if (onboardingState.isCompleted) {
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
                page = onboardingState.howItWorksPage,
                onPageChanged = { page -> onboardingViewModel.onHowItWorksPageChanged(page) },
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
                profile = onboardingState.profile,
                firstNameError = onboardingState.firstNameError,
                lastNameError = onboardingState.lastNameError,
                ageOrDobError = onboardingState.ageOrDobError,
                heightError = onboardingState.heightError,
                onProfileChanged = { update -> onboardingViewModel.updateProfileField(update) },
                onContinue = {
                    onboardingViewModel.validateAndProceed {
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
                    onboardingViewModel.completeOnboarding()
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

        // Phase 3 Real Assessment Flow
        composable(route = Screen.AssessmentIntro.route) {
            AssessmentIntroScreen(
                onGetReady = {
                    navController.navigate(Screen.AssessmentReadiness.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.AssessmentReadiness.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.AssessmentIntro.route)
            }
            val assessmentViewModel: AssessmentViewModel = hiltViewModel(parentEntry)
            val assessmentState by assessmentViewModel.uiState.collectAsState()

            AssessmentReadinessScreen(
                isAccelAvailable = assessmentState.isAccelAvailable,
                isGyroAvailable = assessmentState.isGyroAvailable,
                isSensorsAvailable = assessmentState.isSensorsAvailable,
                onStartWalk = {
                    assessmentViewModel.start30sCollection()
                    navController.navigate(Screen.AssessmentActive.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.AssessmentActive.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.AssessmentIntro.route)
            }
            val assessmentViewModel: AssessmentViewModel = hiltViewModel(parentEntry)
            val assessmentState by assessmentViewModel.uiState.collectAsState()

            var navTriggered by remember { mutableStateOf(false) }

            // Automatically navigate to Summary once when completed
            LaunchedEffect(assessmentState.sessionStatus) {
                if (assessmentState.sessionStatus == SessionStatus.COMPLETED && !navTriggered) {
                    navTriggered = true
                    navController.navigate(Screen.AssessmentSummary.route)
                }
            }

            AssessmentActiveScreen(
                remainingSeconds = assessmentState.remainingSeconds,
                sampleCount = assessmentState.sampleCount,
                lastSample = assessmentState.lastSample,
                processingStatus = assessmentState.processingStatus,
                onStopAssessment = {
                    assessmentViewModel.cancelCollection()
                    navController.popBackStack(Screen.Home.route, false)
                }
            )
        }

        composable(route = Screen.AssessmentSummary.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.AssessmentIntro.route)
            }
            val assessmentViewModel: AssessmentViewModel = hiltViewModel(parentEntry)
            val assessmentState by assessmentViewModel.uiState.collectAsState()

            AssessmentSummaryScreen(
                session = assessmentState.completedSession,
                qualityResult = assessmentState.qualityResult,
                gaitMetrics = assessmentState.gaitMetrics,
                mlPrediction = assessmentState.mlPrediction,
                baselineComparison = assessmentState.baselineComparison,
                onRetry = {
                    assessmentViewModel.resetSession()
                    navController.popBackStack(Screen.AssessmentIntro.route, false)
                },
                onReturnHome = {
                    assessmentViewModel.resetSession()
                    navController.popBackStack(Screen.Home.route, false)
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
