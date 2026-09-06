package com.cadence.gaitradar.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cadence.gaitradar.core.storage.UserProfile

@Composable
fun PersonalInformationScreen(
    profile: UserProfile,
    firstNameError: String?,
    lastNameError: String?,
    ageOrDobError: String?,
    heightError: String?,
    onProfileChanged: (UserProfile.() -> UserProfile) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Personal Information",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This information helps personalize your baseline mobility profile locally on your device.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = profile.firstName,
                    onValueChange = { value -> onProfileChanged { copy(firstName = value) } },
                    label = { Text("First Name *") },
                    isError = firstNameError != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (firstNameError != null) {
                    Text(firstNameError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = profile.lastName,
                    onValueChange = { value -> onProfileChanged { copy(lastName = value) } },
                    label = { Text("Last Name *") },
                    isError = lastNameError != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (lastNameError != null) {
                    Text(lastNameError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = profile.ageOrDob,
                    onValueChange = { value -> onProfileChanged { copy(ageOrDob = value) } },
                    label = { Text("Age or Date of Birth *") },
                    isError = ageOrDobError != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (ageOrDobError != null) {
                    Text(ageOrDobError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = profile.heightCm,
                    onValueChange = { value -> onProfileChanged { copy(heightCm = value) } },
                    label = { Text("Height (cm) *") },
                    isError = heightError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (heightError != null) {
                    Text(heightError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = profile.mobilityContext,
                    onValueChange = { value -> onProfileChanged { copy(mobilityContext = value) } },
                    label = { Text("Relevant Mobility Context (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}
