package com.cadence.gaitradar.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class UserProfile(
    val firstName: String = "",
    val lastName: String = "",
    val ageOrDob: String = "",
    val heightCm: String = "",
    val mobilityContext: String = ""
)

@Singleton
class OnboardingPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val FIRST_NAME = stringPreferencesKey("first_name")
        val LAST_NAME = stringPreferencesKey("last_name")
        val AGE_OR_DOB = stringPreferencesKey("age_or_dob")
        val HEIGHT_CM = stringPreferencesKey("height_cm")
        val MOBILITY_CONTEXT = stringPreferencesKey("mobility_context")
    }

    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false
        }

    val userProfile: Flow<UserProfile> = dataStore.data
        .map { preferences ->
            UserProfile(
                firstName = preferences[PreferencesKeys.FIRST_NAME] ?: "",
                lastName = preferences[PreferencesKeys.LAST_NAME] ?: "",
                ageOrDob = preferences[PreferencesKeys.AGE_OR_DOB] ?: "",
                heightCm = preferences[PreferencesKeys.HEIGHT_CM] ?: "",
                mobilityContext = preferences[PreferencesKeys.MOBILITY_CONTEXT] ?: ""
            )
        }

    suspend fun saveProfileAndCompleteOnboarding(profile: UserProfile) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_NAME] = profile.firstName
            preferences[PreferencesKeys.LAST_NAME] = profile.lastName
            preferences[PreferencesKeys.AGE_OR_DOB] = profile.ageOrDob
            preferences[PreferencesKeys.HEIGHT_CM] = profile.heightCm
            preferences[PreferencesKeys.MOBILITY_CONTEXT] = profile.mobilityContext
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = true
        }
    }

    suspend fun updateProfile(profile: UserProfile) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_NAME] = profile.firstName
            preferences[PreferencesKeys.LAST_NAME] = profile.lastName
            preferences[PreferencesKeys.AGE_OR_DOB] = profile.ageOrDob
            preferences[PreferencesKeys.HEIGHT_CM] = profile.heightCm
            preferences[PreferencesKeys.MOBILITY_CONTEXT] = profile.mobilityContext
        }
    }
}
