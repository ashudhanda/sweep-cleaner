package com.sweep.cleaner.ui.screens.junk

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sweep.cleaner.model.JunkCategoryItem
import com.sweep.cleaner.model.JunkCleanStage
import com.sweep.cleaner.ui.theme.BrandAmber
import com.sweep.cleaner.ui.theme.BrandTeal
import com.sweep.cleaner.ui.theme.BrandTealBright
import com.sweep.cleaner.ui.theme.BrandTealMint
import com.sweep.cleaner.ui.theme.BrandTealSurface
import com.sweep.cleaner.ui.viewmodel.SweepViewModel
import com.sweep.cleaner.util.ByteFormatter
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OneTapJunkCleanScreen(
    viewModel: SweepViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val junkState by viewModel.oneTapJunkState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (junkState.stage == JunkCleanStage.IDLE) {
            viewModel.startOneTapJunkScan()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("one_tap_junk_clean_screen")
    ) {
        // App Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("junk_clean_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "1-Tap Junk Cleaner",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Real cache, logs & temporary clutter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (junkState.stage == JunkCleanStage.READY || junkState.stage == JunkCleanStage.COMPLETED) {
                IconButton(onClick = { viewModel.startOneTapJunkScan() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rescan",
                        tint = BrandTeal
                    )
                }
            }
        }

        // Main Content Switcher
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            when (junkState.stage) {
                JunkCleanStage.IDLE, JunkCleanStage.SCANNING -> {
                    JunkScanningContent(
                        progress = junkState.scanProgress,
                        actionText = junkState.currentScanAction
                    )
                }

                JunkCleanStage.READY -> {
                    JunkReadyContent(
                        categories = junkState.categories,
                        totalBytes = junkState.totalJunkBytes,
                        onCleanClick = { viewModel.executeOneTapJunkClean() }
                    )
                }

                JunkCleanStage.CLEANING -> {
                    JunkCleaningAnimationContent(
                        categories = junkState.categories,
                        reclaimedBytes = junkState.reclaimedBytes,
                        totalBytes = junkState.totalJunkBytes,
                        actionText = junkState.currentScanAction
                    )
                }

                JunkCleanStage.COMPLETED -> {
                    JunkCompletedContent(
                        reclaimedBytes = junkState.reclaimedBytes,
                        cleanedItems = junkState.totalCleanedItems,
                        durationMs = junkState.cleanDurationMs,
                        onDoneClick = {
                            viewModel.resetOneTapJunkState()
                            onBackClick()
                        }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. Scanning State with Radar Pulse & Energy Particles
// -------------------------------------------------------------

@Composable
private fun JunkScanningContent(
    progress: Float,
    actionText: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning_radar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_rotation"
    )
    val pulseRing by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_ring"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Holographic Canvas Radar Visualizer
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension / 2.6f

                // Outer pulsing glow circle
                drawCircle(
                    color = BrandTealMint.copy(alpha = 0.15f * pulseRing),
                    radius = baseRadius * pulseRing,
                    center = center
                )

                // Middle dashed decorative ring
                drawCircle(
                    color = BrandTealBright.copy(alpha = 0.4f),
                    radius = baseRadius * 0.85f,
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), rotation)
                    )
                )

                // Inner steady ring
                drawCircle(
                    color = BrandTeal.copy(alpha = 0.6f),
                    radius = baseRadius * 0.55f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Rotating radar beam line
                val rad = Math.toRadians(rotation.toDouble())
                val beamEnd = Offset(
                    (center.x + baseRadius * cos(rad)).toFloat(),
                    (center.y + baseRadius * sin(rad)).toFloat()
                )
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(BrandTealMint, Color.Transparent),
                        start = center,
                        end = beamEnd
                    ),
                    start = center,
                    end = beamEnd,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Orbiting glowing particle sparks
                for (i in 0..5) {
                    val pAngle = Math.toRadians((rotation * 1.5 + (i * 60)).toDouble())
                    val pRadius = baseRadius * (0.4f + (i * 0.1f))
                    val pX = (center.x + pRadius * cos(pAngle)).toFloat()
                    val pY = (center.y + pRadius * sin(pAngle)).toFloat()
                    drawCircle(
                        color = if (i % 2 == 0) BrandTealMint else BrandAmber,
                        radius = 4.dp.toPx(),
                        center = Offset(pX, pY)
                    )
                }
            }

            // Center Cleaning Icon Core
            Surface(
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                color = BrandTeal,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "Analyzing Device Storage...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = actionText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = BrandTeal,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// -------------------------------------------------------------
// 2. Ready State with Detailed Junk Categories & Glowing Button
// -------------------------------------------------------------

@Composable
private fun JunkReadyContent(
    categories: List<JunkCategoryItem>,
    totalBytes: Long,
    onCleanClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Glowing Pulse for 1-Tap Clean CTA
    val infiniteTransition = rememberInfiniteTransition(label = "cta_glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Junk Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(BrandTealMint.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = BrandTeal,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (totalBytes > 0) ByteFormatter.formatBytes(totalBytes) else "0 B",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandTeal
                )

                Text(
                    text = if (totalBytes > 0) "Total junk safe to sweep" else "Storage is clean & optimized",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // One-Tap Glowing Clean Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer pulsating glow aura
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        BrandTealMint.copy(alpha = glowPulse * 0.7f),
                                        BrandTealBright.copy(alpha = glowPulse * 0.9f),
                                        BrandTealMint.copy(alpha = glowPulse * 0.7f)
                                    )
                                )
                            )
                    )

                    Button(
                        onClick = onCleanClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .padding(horizontal = 2.dp)
                            .border(
                                width = 1.5.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        BrandTealMint.copy(alpha = 0.9f),
                                        Color.White.copy(alpha = 0.8f),
                                        BrandTealMint.copy(alpha = 0.9f)
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .testTag("one_tap_sweep_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandTeal,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = BrandTealMint,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (totalBytes > 0) "1-Tap Clean (${ByteFormatter.formatBytes(totalBytes)})" else "Sweep Empty Folders & Refresh",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "Categories to Sweep",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Category Cards List
        categories.forEach { category ->
            JunkCategoryRow(category = category)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun JunkCategoryRow(category: JunkCategoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandTealMint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (category.iconType) {
                        "cache" -> Icons.Default.CleaningServices
                        "logs" -> Icons.Default.Speed
                        "apk" -> Icons.Default.Android
                        "folder" -> Icons.Default.FolderZip
                        "thumbnail" -> Icons.Default.Image
                        else -> Icons.Default.Delete
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = BrandTeal,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (category.bytes > 0) ByteFormatter.formatBytes(category.bytes) else "${category.itemCount} items",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandTeal
                )
                if (category.itemCount > 0 && category.bytes > 0) {
                    Text(
                        text = "${category.itemCount} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. Cleaning State with Dynamic Sweeping Whirlwind & Real-time Progress
// -------------------------------------------------------------

@Composable
private fun JunkCleaningAnimationContent(
    categories: List<JunkCategoryItem>,
    reclaimedBytes: Long,
    totalBytes: Long,
    actionText: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cleaning_tornado")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    val progress = if (totalBytes > 0) (reclaimedBytes.toFloat() / totalBytes).coerceIn(0.1f, 1f) else 0.5f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "clean_progress")

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Sweeping Energy Tornado Visualizer
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension / 2.4f

                // Outer progress track
                drawCircle(
                    color = BrandTealMint.copy(alpha = 0.2f),
                    radius = maxRadius,
                    center = center,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )

                // Animated glowing progress arc
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(BrandTealMint, BrandTeal, BrandTealBright, BrandTealMint)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )

                // Swirling particles inside
                for (i in 0..7) {
                    val angle = Math.toRadians((sweepAngle * 2 + (i * 45)).toDouble())
                    val radius = maxRadius * (0.3f + (i % 3) * 0.25f)
                    val px = (center.x + radius * cos(angle)).toFloat()
                    val py = (center.y + radius * sin(angle)).toFloat()
                    drawCircle(
                        color = if (i % 2 == 0) BrandTealMint else BrandAmber,
                        radius = 4.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }

            // Center Dynamic Byte Counter
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = BrandTealMint,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ByteFormatter.formatBytes(reclaimedBytes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BrandTeal
                )
                Text(
                    text = "Freed so far",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Sweeping in Progress...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = actionText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Mini Cleaned Checkmarks List
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.take(3).forEach { cat ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (cat.isCleaned) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandTeal
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BrandTeal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = BrandTealMint
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. Completed State: Celebration Fireworks & Stats
// -------------------------------------------------------------

@Composable
private fun JunkCompletedContent(
    reclaimedBytes: Long,
    cleanedItems: Int,
    durationMs: Long,
    onDoneClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "celebration_sparkles")
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Celebration Sparkle Ring
        Box(
            modifier = Modifier.size(170.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2.3f

                // Concentric celebration ring
                drawCircle(
                    color = BrandTealMint.copy(alpha = 0.2f),
                    radius = radius * sparkleScale,
                    center = center
                )

                // Confetti / floating sparkle dots
                for (i in 0..11) {
                    val angle = Math.toRadians((i * 30).toDouble())
                    val dotDist = radius * (0.85f + (i % 3) * 0.15f)
                    val dx = (center.x + dotDist * cos(angle)).toFloat()
                    val dy = (center.y + dotDist * sin(angle)).toFloat()
                    drawCircle(
                        color = when (i % 3) {
                            0 -> BrandTealMint
                            1 -> BrandAmber
                            else -> BrandTealBright
                        },
                        radius = 4.dp.toPx(),
                        center = Offset(dx, dy)
                    )
                }
            }

            Surface(
                modifier = Modifier.size(90.dp),
                shape = CircleShape,
                color = BrandTeal,
                shadowElevation = 10.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Storage Swept Clean!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your device is running at optimal capacity",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Reclaimed Space Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Space Reclaimed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ByteFormatter.formatBytes(reclaimedBytes),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandTeal
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$cleanedItems",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Items Cleared",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "100%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandTeal
                        )
                        Text(
                            text = "Safe & Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Optimal 🟢",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Device Health",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Return to Dashboard Action Button
        Button(
            onClick = onDoneClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("junk_clean_done_button"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandTeal,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Back to Dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = BrandTealMint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
