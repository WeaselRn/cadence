package com.cadence.gaitradar.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cadence_preferences")

data class AppSettings(
    val largerText: Boolean = false,
    val reducedMotion: Boolean = false,
    val voiceInstructions: Boolean = false,
    val hapticFeedback: Boolean = true,
    val assessmentReminders: Boolean = true,
    val autoDeleteSensors: Boolean = true
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val LARGER_TEXT = booleanPreferencesKey("larger_text")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val VOICE_INSTRUCTIONS = booleanPreferencesKey("voice_instructions")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val ASSESSMENT_REMINDERS = booleanPreferencesKey("assessment_reminders")
        val AUTO_DELETE_SENSORS = booleanPreferencesKey("auto_delete_sensors")
    }

    val appSettings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            largerText = preferences[PreferencesKeys.LARGER_TEXT] ?: false,
            reducedMotion = preferences[PreferencesKeys.REDUCED_MOTION] ?: false,
            voiceInstructions = preferences[PreferencesKeys.VOICE_INSTRUCTIONS] ?: false,
            hapticFeedback = preferences[PreferencesKeys.HAPTIC_FEEDBACK] ?: true,
            assessmentReminders = preferences[PreferencesKeys.ASSESSMENT_REMINDERS] ?: true,
            autoDeleteSensors = preferences[PreferencesKeys.AUTO_DELETE_SENSORS] ?: true
        )
    }

    suspend fun updateLargerText(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LARGER_TEXT] = enabled
        }
    }

    suspend fun updateReducedMotion(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REDUCED_MOTION] = enabled
        }
    }

    suspend fun updateVoiceInstructions(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VOICE_INSTRUCTIONS] = enabled
        }
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_FEEDBACK] = enabled
        }
    }

    suspend fun updateAssessmentReminders(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ASSESSMENT_REMINDERS] = enabled
        }
    }

    suspend fun updateAutoDeleteSensors(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_DELETE_SENSORS] = enabled
        }
    }

    suspend fun clearAllLocalData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
