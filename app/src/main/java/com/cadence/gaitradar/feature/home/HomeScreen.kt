package com.cadence.gaitradar.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cadence.gaitradar.core.baseline.LongitudinalStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onStartAssessment: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateAbout: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    val baselineState by viewModel.baselineUiState.collectAsState()
    val name = profile.firstName.ifBlank { "there" }

    val comparison = baselineState.latestComparison
    val latest = baselineState.latestAssessment
    val stats = baselineState.baselineStats

    val isBaselineEstablished = stats?.isEstablished == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome back, $name",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Status / Dashboard Header Card
                val statusTitle = comparison?.status?.title ?: "Build Your Personal Baseline"
                val statusColor = when (comparison?.status) {
                    LongitudinalStatus.BUILDING_BASELINE -> MaterialTheme.colorScheme.primary
                    LongitudinalStatus.STABLE -> Color(0xFF2E7D32)
                    LongitudinalStatus.CHANGE_DETECTED -> Color(0xFFE65100)
                    LongitudinalStatus.PERSISTENT_CHANGE -> Color(0xFFC62828)
                    null -> MaterialTheme.colorScheme.primary
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "CURRENT MOBILITY STATUS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (latest != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Latest Score",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${latest.mobilityStabilityScore}",
                                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                if (isBaselineEstablished && comparison?.scoreDelta != null) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "vs Baseline",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val deltaStr = if (comparison.scoreDelta >= 0f) {
                                            "+${"%.1f".format(comparison.scoreDelta)} pts"
                                        } else {
                                            "${"%.1f".format(comparison.scoreDelta)} pts"
                                        }
                                        Text(
                                            text = deltaStr,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = if (comparison.scoreDelta >= 0f) Color(0xFF2E7D32) else Color(0xFFE65100),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Complete 3 walking assessments to build your personal mobility baseline.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onStartAssessment,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = "Start assessment",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recent Assessment Summary Card
                if (latest != null) {
                    val dateStr = rememberFormattedHomeDate(latest.timestampMs)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Latest Assessment",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            DashboardRow("Date", dateStr)
                            Spacer(modifier = Modifier.height(6.dp))
                            DashboardRow("Score", "${latest.mobilityStabilityScore} / 100")
                            Spacer(modifier = Modifier.height(6.dp))
                            DashboardRow("Steps", "${latest.stepCount ?: 0}")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Baseline Progress Context Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Assessment Context",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBaselineEstablished) "Personal baseline established" else "Building baseline: ${baselineState.assessmentCount} / 3 walks",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "${baselineState.assessmentCount} total",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateProfile,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Profile")
                    }

                    OutlinedButton(
                        onClick = onNavigateHistory,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("History (${baselineState.assessmentCount})")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = onNavigateSettings) {
                    Text("Settings")
                }
                OutlinedButton(onClick = onNavigateAbout) {
                    Text("About & Help")
                }
            }
        }
    }
}

@Composable
private fun DashboardRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun rememberFormattedHomeDate(timestampMs: Long): String {
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.getDefault()) }
    return remember(timestampMs) { sdf.format(Date(timestampMs)) }
}
