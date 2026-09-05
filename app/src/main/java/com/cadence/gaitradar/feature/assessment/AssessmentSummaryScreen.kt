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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cadence.gaitradar.core.baseline.BaselineComparison
import com.cadence.gaitradar.core.baseline.LongitudinalStatus
import com.cadence.gaitradar.core.metrics.GaitMetricsResult
import com.cadence.gaitradar.core.quality.QualityResult
import com.cadence.gaitradar.core.sensors.ImuSession
import com.cadence.gaitradar.ml.MlPrediction

@Composable
fun AssessmentSummaryScreen(
    session: ImuSession?,
    qualityResult: QualityResult?,
    gaitMetrics: GaitMetricsResult?,
    mlPrediction: MlPrediction?,
    baselineComparison: BaselineComparison? = null,
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
                    text = if (isValid) "Assessment Complete" else "We couldn't get a reliable assessment",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isValid)
                        "Raw 6-axis motion data analyzed locally via TFLite TCN."
                    else
                        "The movement data wasn't clear enough to analyze this time.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Mobility Stability Score Card (Primary ML Result)
                if (isValid && mlPrediction != null && mlPrediction.isSuccess) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = MaterialTheme.shapes.extraLarge,
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Mobility Stability Score",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "${mlPrediction.mobilityStabilityScore}",
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 64.sp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Text(
                                text = "Out of 100",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Movement-pattern consistency score based on your 30-second walk.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Personal Baseline Comparison Card
                if (isValid && baselineComparison != null) {
                    val statusColor = when (baselineComparison.status) {
                        LongitudinalStatus.BUILDING_BASELINE -> Color(0xFF0288D1)
                        LongitudinalStatus.STABLE -> Color(0xFF2E7D32)
                        LongitudinalStatus.CHANGE_DETECTED -> Color(0xFFE65100)
                        LongitudinalStatus.PERSISTENT_CHANGE -> Color(0xFFC62828)
                    }

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
                                text = "Personal Baseline Comparison",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = baselineComparison.status.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = baselineComparison.statusMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (baselineComparison.scoreDelta != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                val deltaText = if (baselineComparison.scoreDelta >= 0f) {
                                    "+${"%.1f".format(baselineComparison.scoreDelta)} pts vs usual baseline"
                                } else {
                                    "${"%.1f".format(baselineComparison.scoreDelta)} pts vs usual baseline"
                                }
                                SummaryRow("Delta vs Usual Baseline", deltaText)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

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
                            text = if (isValid) "Quality Gate: PASSED ✓" else "Quality Check: TRY AGAIN ⚠️",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isValid) Color(0xFF2E7D32) else Color(0xFFE65100),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isValid)
                                "Session contains reliable 6-axis IMU samples evaluated by TFLite model."
                            else
                                qualityResult?.failureMessage ?: "The movement data wasn't clear enough to analyze this time.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Physical Gait Metrics Card
                if (isValid && gaitMetrics != null) {
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
                                text = "Physical Gait Features",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            SummaryRow("Cadence", gaitMetrics.cadenceStepsPerMin?.let { "${"%.1f".format(it)} steps/min" } ?: "—")
                            Spacer(modifier = Modifier.height(10.dp))
                            SummaryRow("Estimated Walking Speed", gaitMetrics.estimatedSpeedMps?.let { "${"%.2f".format(it)} m/s" } ?: "—")
                            Spacer(modifier = Modifier.height(10.dp))
                            SummaryRow("Step Time Variability", gaitMetrics.stepTimeVariabilityMs?.let { "${"%.1f".format(it)} ms" } ?: "—")
                            Spacer(modifier = Modifier.height(10.dp))
                            SummaryRow("Symmetry Index", gaitMetrics.symmetryIndex?.let { "%.2f".format(it) } ?: "—")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Debug ML Analysis Details Card
                if (isValid && mlPrediction != null && mlPrediction.isSuccess) {
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
                                text = "ML Model Analysis (Debug)",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            SummaryRow("Model Version", mlPrediction.modelVersion)
                            Spacer(modifier = Modifier.height(10.dp))
                            SummaryRow("Input Tensor Shape", mlPrediction.inputShape.joinToString(" × "))
                            Spacer(modifier = Modifier.height(10.dp))
                            SummaryRow("Inference Time", "${mlPrediction.inferenceTimeMs} ms")
                            Spacer(modifier = Modifier.height(10.dp))
                            SummaryRow("Raw p_irregular", "%.4f".format(mlPrediction.pIrregular))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
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
                            text = "Try Again",
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
