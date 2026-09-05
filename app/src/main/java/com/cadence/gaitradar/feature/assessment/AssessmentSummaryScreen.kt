package com.cadence.gaitradar.feature.assessment

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadence.gaitradar.core.metrics.GaitMetricsResult
import com.cadence.gaitradar.core.quality.QualityResult
import com.cadence.gaitradar.core.sensors.ImuSession

@Composable
fun AssessmentSummaryScreen(
    session: ImuSession?,
    qualityResult: QualityResult?,
    gaitMetrics: GaitMetricsResult?,
    onRetry: () -> Unit,
    onReturnHome: () -> Unit
) {
    val isValid = qualityResult?.isValid == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        if (isValid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        Color(0xFFE3F2FD)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isValid) "Assessment Complete" else "Quality Check Failed",
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isValid)
                        "Raw 6-axis motion data passed quality gate."
                    else
                        "The recording did not meet quality requirements for analysis.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Quality Gate Status Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isValid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (isValid) "Quality Gate: PASSED ✓" else "Quality Gate: RETRY REQUIRED ⚠️",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isValid) Color(0xFF2E7D32) else Color(0xFFE65100),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isValid)
                                "Session contains reliable 6-axis IMU samples ready for physical feature analysis."
                            else
                                qualityResult?.failureMessage ?: "Session was incomplete or contained irregular motion data.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Physical Gait Metrics Card (Only displayed for quality PASS sessions)
                if (isValid && gaitMetrics != null && gaitMetrics.isCalculated) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Physical Gait Features",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            SummaryRow("Estimated Step Count", "${gaitMetrics.stepCount ?: 0} steps")
                            Spacer(modifier = Modifier.height(10.dp))
                            SummaryRow("Cadence", if (gaitMetrics.cadenceStepsPerMin != null) "${"%.1f".format(gaitMetrics.cadenceStepsPerMin)} steps/min" else "N/A")
                            Spacer(modifier = Modifier.height(10.dp))
                            SummaryRow("Mean Step Interval", if (gaitMetrics.meanStepIntervalMs != null) "${"%.0f".format(gaitMetrics.meanStepIntervalMs)} ms" else "N/A")
                            Spacer(modifier = Modifier.height(10.dp))
                            SummaryRow("Step-Time Variability", if (gaitMetrics.stepTimeVariabilityMs != null) "${"%.1f".format(gaitMetrics.stepTimeVariabilityMs)} ms" else "N/A")
                            Spacer(modifier = Modifier.height(10.dp))
                            SummaryRow("Accel Variability", if (gaitMetrics.accelVariability != null) "${"%.2f".format(gaitMetrics.accelVariability)} m/s²" else "N/A")
                            Spacer(modifier = Modifier.height(10.dp))
                            SummaryRow("Gyro Variability", if (gaitMetrics.gyroVariability != null) "${"%.2f".format(gaitMetrics.gyroVariability)} rad/s" else "N/A")
                            if (gaitMetrics.estimatedSpeedMps != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                SummaryRow("Estimated Speed", "${"%.2f".format(gaitMetrics.estimatedSpeedMps)} m/s")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (session != null && qualityResult != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Validation Details",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            SummaryRow("Duration", "${session.durationMs / 1000}s (${if (qualityResult.details.isDurationValid) "Valid ✓" else "Invalid ✗"})")
                            Spacer(modifier = Modifier.height(10.dp))
                            SummaryRow("IMU Samples", "${session.samples.size} (${if (qualityResult.details.isSampleCountValid) "Valid ✓" else "Low ✗"})")
                            Spacer(modifier = Modifier.height(10.dp))
                            SummaryRow("Avg Rate", "${"%.1f".format(session.averageSamplingRateHz)} Hz (${if (qualityResult.details.isSamplingRateValid) "Valid ✓" else "Irregular ✗"})")
                            Spacer(modifier = Modifier.height(10.dp))
                            SummaryRow("Finite Values", if (qualityResult.details.areValuesFinite) "Pass ✓" else "Fail ✗")
                            Spacer(modifier = Modifier.height(10.dp))
                            SummaryRow("Signal Sanity", if (qualityResult.details.isSignalSane) "Pass ✓" else "Flatline ✗")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                if (!isValid) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "Retry Assessment",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedButton(
                    onClick = onReturnHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Return Home",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
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
