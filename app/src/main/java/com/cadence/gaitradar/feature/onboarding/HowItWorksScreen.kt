package com.cadence.gaitradar.feature.onboarding

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
import androidx.compose.foundation.layout.width
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

data class CarouselSlide(
    val title: String,
    val body: String
)

@Composable
fun HowItWorksScreen(
    page: Int,
    onPageChanged: (Int) -> Unit,
    onFinished: () -> Unit,
    onBack: () -> Unit
) {
    val slides = listOf(
        CarouselSlide(
            title = "Your phone can sense movement",
            body = "Your phone's built-in motion sensors can capture patterns from the way you walk."
        ),
        CarouselSlide(
            title = "Repeat assessments create a baseline",
            body = "Repeated walks help establish your usual movement pattern."
        ),
        CarouselSlide(
            title = "Compare you with you",
            body = "Future assessments are compared with your own history rather than generalized averages."
        ),
        CarouselSlide(
            title = "Designed for privacy",
            body = "Movement data and analysis are designed to remain on your device."
        )
    )

    val currentSlide = slides[page.coerceIn(0, slides.size - 1)]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFE8F5E9),
                        Color(0xFFE3F2FD)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar / Progress indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (page > 0) {
                    OutlinedButton(onClick = { onPageChanged(page - 1) }) {
                        Text("Back")
                    }
                } else {
                    OutlinedButton(onClick = onBack) {
                        Text("Back")
                    }
                }

                Text(
                    text = "${page + 1} of ${slides.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(64.dp))
            }

            // Center Content Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentSlide.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = currentSlide.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom CTA
            Button(
                onClick = {
                    if (page < slides.size - 1) {
                        onPageChanged(page + 1)
                    } else {
                        onFinished()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (page < slides.size - 1) "Next" else "Continue",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}
