package com.sweep.cleaner.ui.screens.contacts

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sweep.cleaner.model.DuplicateContactGroup
import com.sweep.cleaner.ui.components.PermissionBanner
import com.sweep.cleaner.ui.components.SweepTopBar
import com.sweep.cleaner.ui.theme.BrandAmber
import com.sweep.cleaner.ui.theme.BrandAmberLight
import com.sweep.cleaner.ui.theme.BrandTeal
import com.sweep.cleaner.ui.theme.BrandTealMint
import com.sweep.cleaner.ui.theme.DangerRed
import com.sweep.cleaner.ui.viewmodel.SweepViewModel

@Composable
fun DuplicateContactsScreen(
    viewModel: SweepViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.loadDuplicateContacts()
        }
    }

    val groups by viewModel.duplicateContacts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isContactsLoading.collectAsStateWithLifecycle()

    LaunchedEffect(hasPermission) {
        if (hasPermission && groups.isEmpty()) {
            viewModel.loadDuplicateContacts()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("duplicate_contacts_screen")
    ) {
        SweepTopBar(
            title = "Duplicate Contacts",
            subtitle = if (groups.isNotEmpty()) "${groups.size} duplicate sets detected" else null,
            onBackClick = onBackClick
        )

        if (!hasPermission) {
            PermissionBanner(
                title = "Contacts Access Needed",
                description = "To scan for duplicate names and phone numbers on your device, Sweep needs read-only access to Contacts. Your data never leaves your device.",
                onRequestPermission = { launcher.launch(Manifest.permission.READ_CONTACTS) }
            )
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandTealMint)
            }
        } else if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No duplicate contacts found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Your address book is clean with zero duplicate phone numbers or names.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // Critical safety warning: contacts cannot be recovered from trash
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BrandAmberLight)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(22.dp))
                        Text(
                            text = "Warning: Merged or deleted contacts cannot be recovered from the 30-day file trash. Review all fields before merging.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                items(groups) { group ->
                    DuplicateContactGroupCard(group = group)
                }
            }
        }
    }
}

@Composable
fun DuplicateContactGroupCard(group: DuplicateContactGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BrandAmberLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = BrandAmber, modifier = Modifier.size(20.dp))
                    }
                    Text(text = group.primaryContact.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        text = "${group.duplicates.size + 1} records",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Merge Preview Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Surviving record preview:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = BrandTeal)
                    Text(text = "Name: ${group.primaryContact.displayName}", style = MaterialTheme.typography.bodySmall)
                    if (group.survivingPhoneNumbers.isNotEmpty()) {
                        Text(text = "Phones: ${group.survivingPhoneNumbers.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (group.survivingEmails.isNotEmpty()) {
                        Text(text = "Emails: ${group.survivingEmails.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
