package com.cadence.gaitradar.feature.profile

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel = hiltViewModel(),
    onSaved: () -> Unit,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile

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
                    text = "Edit Profile",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Update your local profile details below.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = profile.firstName,
                    onValueChange = { value -> viewModel.updateProfileField { copy(firstName = value) } },
                    label = { Text("First Name *") },
                    isError = uiState.firstNameError != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (uiState.firstNameError != null) {
                    Text(uiState.firstNameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = profile.lastName,
                    onValueChange = { value -> viewModel.updateProfileField { copy(lastName = value) } },
                    label = { Text("Last Name *") },
                    isError = uiState.lastNameError != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (uiState.lastNameError != null) {
                    Text(uiState.lastNameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = profile.ageOrDob,
                    onValueChange = { value -> viewModel.updateProfileField { copy(ageOrDob = value) } },
                    label = { Text("Age or Date of Birth *") },
                    isError = uiState.ageOrDobError != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (uiState.ageOrDobError != null) {
                    Text(uiState.ageOrDobError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = profile.heightCm,
                    onValueChange = { value -> viewModel.updateProfileField { copy(heightCm = value) } },
                    label = { Text("Height (cm) *") },
                    isError = uiState.heightError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (uiState.heightError != null) {
                    Text(uiState.heightError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = profile.mobilityContext,
                    onValueChange = { value -> viewModel.updateProfileField { copy(mobilityContext = value) } },
                    label = { Text("Relevant Mobility Context (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.saveProfile(onSaved) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Save changes",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}
