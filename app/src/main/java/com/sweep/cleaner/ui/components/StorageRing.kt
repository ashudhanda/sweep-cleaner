package com.sweep.cleaner.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sweep.cleaner.ui.theme.BrandTeal
import com.sweep.cleaner.ui.theme.BrandTealBright
import com.sweep.cleaner.ui.theme.BrandTealMint
import com.sweep.cleaner.util.ByteFormatter

@Composable
fun StorageRing(
    usedBytes: Long,
    totalBytes: Long,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    strokeWidth: Dp = 12.dp
) {
    val targetFraction = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0.01f, 1f) else 0f
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(targetFraction) {
        animatedProgress.animateTo(
            targetValue = targetFraction,
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
        )
    }

    val (currentVal, currentUnit) = ByteFormatter.formatBytesParts((usedBytes * (animatedProgress.value / targetFraction.coerceAtLeast(0.01f))).toLong())
    val accessibilityText = "Storage used: $currentVal $currentUnit of ${ByteFormatter.formatBytes(totalBytes)}"

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = accessibilityText }
            .testTag("storage_ring"),
        contentAlignment = Alignment.Center
    ) {
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        val gradientBrush = Brush.sweepGradient(
            listOf(BrandTeal, BrandTealBright, BrandTealMint, BrandTeal)
        )

        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = this.size.width - strokePx

            // Background track ring
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Animated progress sweep arc
            val sweep = animatedProgress.value * 360f
            if (sweep > 0f) {
                drawArc(
                    brush = gradientBrush,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // Center Count-up numbers
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentVal,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$currentUnit used",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
