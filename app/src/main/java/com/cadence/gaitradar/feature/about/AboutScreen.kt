package com.cadence.gaitradar.feature.about

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadence.gaitradar.core.AppConfig

@Composable
fun AboutScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

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
                    text = "About & Help",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Gait Functional Decline Radar (v1.0)",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Medical Disclaimer Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Medical Disclaimer",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This app is designed to help observe changes in your walking dynamics over time. It is NOT a clinical diagnostic tool and does NOT replace professional medical evaluation.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Expandable Accordion Sections
                AccordionCard(
                    title = "1. What Is the Gait Assessment?",
                    content = "Gait refers to your pattern of walking. Changes in walking speed, symmetry, cadence, and step time consistency can provide valuable insights into functional mobility.\n\nThis application uses built-in phone motion sensors to observe your personal walking patterns over time."
                )

                Spacer(modifier = Modifier.height(12.dp))

                AccordionCard(
                    title = "2. How Does the Assessment Work?",
                    content = "Step-by-step workflow:\n\n" +
                            "1. You start a 30-second walking assessment.\n" +
                            "2. Built-in motion sensors record movement while carried in your pocket.\n" +
                            "3. Accelerometer & gyroscope measurements are captured at ~50 Hz.\n" +
                            "4. Sensor data undergoes a quality gate on your phone.\n" +
                            "5. Data is preprocessed into 1,500 6-axis samples.\n" +
                            "6. Trained on-device TensorFlow Lite model analyzes gait patterns.\n" +
                            "7. App calculates your Mobility Stability Score.\n" +
                            "8. Result is compared with your established personal baseline.\n" +
                            "9. Longitudinal status is presented with non-diagnostic guidance."
                )

                Spacer(modifier = Modifier.height(12.dp))

                AccordionCard(
                    title = "3. What Sensors Are Used?",
                    content = "• Accelerometer (3-Axis): Measures linear acceleration, stride impacts, and gravity orientation.\n\n" +
                            "• Gyroscope (3-Axis): Measures angular velocity and leg swing rotation.\n\n" +
                            "No camera or video recording is ever used."
                )

                Spacer(modifier = Modifier.height(12.dp))

                AccordionCard(
                    title = "4. What Is the Machine-Learning Model?",
                    content = "The application uses a quantized 1D Temporal Convolutional Network (TCN) trained on 6-axis motion dynamics. It executes entirely on your phone via TensorFlow Lite to derive a movement pattern consistency score out of 100."
                )

                Spacer(modifier = Modifier.height(12.dp))

                AccordionCard(
                    title = "5. What Is My Personal Baseline?",
                    content = "Your baseline represents your typical walking pattern calculated from your first 3 valid walking assessments. Comparing you against yourself provides personalized insights without relying on generalized population averages."
                )

                Spacer(modifier = Modifier.height(12.dp))

                AccordionCard(
                    title = "6. How Should I Interpret My Result?",
                    content = "• Mobility Score: Overall movement pattern score for the current 30-second walk.\n" +
                            "• Baseline Comparison: Difference between your current score and your usual average.\n" +
                            "• A single isolated variation is normal and not functional decline. Repeated changes across multiple sessions carry greater significance."
                )

                Spacer(modifier = Modifier.height(12.dp))

                AccordionCard(
                    title = "7. What Can Affect an Assessment?",
                    content = "Factors that influence sensor measurements:\n" +
                            "• Phone placement in pocket\n" +
                            "• Walking surface or terrain\n" +
                            "• Walking speed variations\n" +
                            "• Pauses or obstacles during the walk"
                )

                Spacer(modifier = Modifier.height(12.dp))

                AccordionCard(
                    title = "8. Privacy & On-Device Processing",
                    content = "Your movement data is sensitive:\n" +
                            "• Sensor processing & ML inference run 100% locally on your phone.\n" +
                            "• Raw sensor data is never uploaded to external servers.\n" +
                            "• You retain full control to delete history or local data anytime in Settings."
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Support & Contact Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Support & Contact",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Have questions or feedback? Contact our team at ${AppConfig.SUPPORT_EMAIL}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:${AppConfig.SUPPORT_EMAIL}")
                                    putExtra(Intent.EXTRA_SUBJECT, "Gait Radar Support Request")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "No email app found. Support email: ${AppConfig.SUPPORT_EMAIL}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Contact Support via Email")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AccordionCard(
    title: String,
    content: String
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
