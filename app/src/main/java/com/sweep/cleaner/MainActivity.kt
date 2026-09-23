package com.sweep.cleaner

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sweep.cleaner.ui.navigation.SweepAppNavigation
import com.sweep.cleaner.ui.theme.SweepTheme
import com.sweep.cleaner.ui.viewmodel.SweepViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SweepViewModel by viewModels()

    private val intentSenderLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.refreshStorageSummary()
        }
        viewModel.clearPendingIntentSender()
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshStorageSummary()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestInitialPermissions()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val tutorialSeen by viewModel.tutorialSeen.collectAsStateWithLifecycle()
            val pendingSender by viewModel.pendingIntentSender.collectAsStateWithLifecycle()

            LaunchedEffect(pendingSender) {
                pendingSender?.let { sender ->
                    val request = IntentSenderRequest.Builder(sender).build()
                    intentSenderLauncher.launch(request)
                }
            }

            SweepTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SweepAppNavigation(
                        viewModel = viewModel,
                        tutorialSeen = tutorialSeen
                    )
                }
            }
        }
    }

    private fun requestInitialPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        storagePermissionLauncher.launch(permissions.toTypedArray())
    }
}
