package com.sweep.cleaner.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sweep.cleaner.ui.components.SweepTopBar
import com.sweep.cleaner.ui.components.openAppSettings
import com.sweep.cleaner.ui.theme.BrandAmber
import com.sweep.cleaner.ui.theme.BrandAmberLight
import com.sweep.cleaner.ui.theme.BrandTeal
import com.sweep.cleaner.ui.theme.BrandTealMint
import com.sweep.cleaner.ui.theme.SuccessGreen
import com.sweep.cleaner.ui.viewmodel.SweepViewModel

@Composable
fun SettingsScreen(
    viewModel: SweepViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTheme by viewModel.themeMode.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()

    val hasContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen")
    ) {
        SweepTopBar(
            title = "Settings",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Theme Section
            SettingsGroup(title = "Appearance") {
                ThemeOptionRow(
                    label = "System Default",
                    selected = currentTheme == "system",
                    onSelect = { viewModel.setThemeMode("system") }
                )
                ThemeOptionRow(
                    label = "Light",
                    selected = currentTheme == "light",
                    onSelect = { viewModel.setThemeMode("light") }
                )
                ThemeOptionRow(
                    label = "Dark",
                    selected = currentTheme == "dark",
                    onSelect = { viewModel.setThemeMode("dark") }
                )
            }

            // Reminders Section
            SettingsGroup(title = "Reminders") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily Storage Check", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Calm reminder at 9:00 AM to review storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = BrandTeal, checkedTrackColor = BrandTealMint)
                    )
                }
            }

            // Permissions Section
            SettingsGroup(title = "Permissions & Security") {
                PermissionStatusItem(
                    name = "Contacts",
                    purpose = "Find and merge duplicate entries",
                    isGranted = hasContacts,
                    onManage = { openAppSettings(context) }
                )

                PermissionStatusItem(
                    name = "Storage & Media",
                    purpose = "Scan photos, videos, and downloads",
                    isGranted = true,
                    onManage = { openAppSettings(context) }
                )
            }

            // Zero-Analytics Privacy Manifesto
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(BrandTealMint.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(24.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Privacy Manifesto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Sweep has ZERO network permissions, zero analytics SDKs, and zero tracking. All scans, hashes, and cleanups occur exclusively on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // About Section
            SettingsGroup(title = "About Sweep") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Sweep Storage Cleaner", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(text = "Version 1.0.0 (Build 100)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "Built natively for modern Android Scoped Storage without deceptive claims or fake cleaners.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = BrandTealMint)
        )
    }
}

@Composable
fun PermissionStatusItem(
    name: String,
    purpose: String,
    isGranted: Boolean,
    onManage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onManage)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = purpose, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isGranted) BrandTealMint.copy(alpha = 0.2f) else BrandAmberLight
        ) {
            Text(
                text = if (isGranted) "Granted" else "Manage",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isGranted) BrandTeal else BrandAmber,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
