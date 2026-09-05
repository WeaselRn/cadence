package com.cadence.gaitradar.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadence.gaitradar.core.storage.OnboardingPreferencesRepository
import com.cadence.gaitradar.core.storage.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    preferencesRepository: OnboardingPreferencesRepository
) : ViewModel() {
    val userProfile: StateFlow<UserProfile> = preferencesRepository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )
}
