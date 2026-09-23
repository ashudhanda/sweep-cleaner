package com.sweep.cleaner.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sweep.cleaner.ui.theme.BrandAmber
import com.sweep.cleaner.ui.theme.BrandTeal
import com.sweep.cleaner.ui.theme.BrandTealMint

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = listOf(
        OnboardingPageData(
            title = "See what is using storage",
            subtitle = "Byte-accurate hardware facts",
            description = "Sweep queries native Android storage APIs directly. No fabricated numbers, no fake 'cache booster' metrics.",
            icon = Icons.Default.PieChart
        ),
        OnboardingPageData(
            title = "Smart scan finds review candidates",
            subtitle = "Organized opportunities",
            description = "Identifies near-identical photos, heavy video files, old screenshots, and leftover installers so you can reclaim space fast.",
            icon = Icons.Default.AutoAwesome
        ),
        OnboardingPageData(
            title = "Nothing deleted without your approval",
            subtitle = "Reversible 30-day trash",
            description = "Every cleanup moves files to your private on-device trash first. You can review, restore anytime, or empty manually.",
            icon = Icons.Default.Shield
        )
    )

    var currentPage by remember { mutableIntStateOf(0) }
    val isLastPage = currentPage == pages.size - 1

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isLastPage) {
                    TextButton(onClick = onFinished) {
                        Text(
                            text = "Skip",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            // Center Content
            AnimatedContent(targetState = currentPage, label = "onboarding_page") { pageIdx ->
                val page = pages[pageIdx]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = page.title,
                            tint = BrandTealMint,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = page.subtitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandAmber,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

            // Bottom controls: Indicators and Action button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 28.dp)
                ) {
                    pages.indices.forEach { index ->
                        val isSelected = index == currentPage
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) BrandTealMint else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (isLastPage) {
                            onFinished()
                        } else {
                            currentPage++
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("onboarding_action_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                ) {
                    Text(
                        text = if (isLastPage) "Get Started with Sweep" else "Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
