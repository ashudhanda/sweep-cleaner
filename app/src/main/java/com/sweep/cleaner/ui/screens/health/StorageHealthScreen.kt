package com.sweep.cleaner.ui.screens.health

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sweep.cleaner.ui.components.SweepTopBar
import com.sweep.cleaner.ui.theme.BrandAmber
import com.sweep.cleaner.ui.theme.BrandAmberLight
import com.sweep.cleaner.ui.theme.BrandTeal
import com.sweep.cleaner.ui.theme.BrandTealMint
import com.sweep.cleaner.ui.viewmodel.SweepViewModel
import com.sweep.cleaner.util.ByteFormatter

@Composable
fun StorageHealthScreen(
    viewModel: SweepViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.storageSummary.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("storage_health_screen")
    ) {
        SweepTopBar(
            title = "Storage Health & Tips",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hygiene Tip Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = BrandAmber, modifier = Modifier.size(22.dp))
                        }
                        Text(text = "Healthy Storage Tips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    TipItem(title = "Keep 15% Free Space", body = "Android solid state storage performs best when at least 15% remains unallocated for wear leveling and background TRIM operations.")
                    TipItem(title = "Review Chat Auto-Downloads", body = "Turn off automatic media downloads in WhatsApp and Telegram settings to prevent constant storage creep.")
                    TipItem(title = "Stream Large Playlists", body = "Offline playlists and cached podcast episodes are common silent space hogs.")
                }
            }

            // Forecasting honesty card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Predictive Forecasting Notice", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Sweep refuses to invent fake 'days until full' predictions. True forecasting requires multiple weeks of historical snapshots to establish real consumption trends.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TipItem(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = BrandTealMint, modifier = Modifier.size(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Text(text = body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 22.dp))
    }
}
