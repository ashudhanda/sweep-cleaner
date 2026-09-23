package com.sweep.cleaner.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sweep.cleaner.ui.screens.apks.ApkInstallersScreen
import com.sweep.cleaner.ui.screens.apps.UnusedAppsScreen
import com.sweep.cleaner.ui.screens.audio.AudioScreen
import com.sweep.cleaner.ui.screens.breakdown.StorageBreakdownScreen
import com.sweep.cleaner.ui.screens.chat.ChatMediaScreen
import com.sweep.cleaner.ui.screens.contacts.DuplicateContactsScreen
import com.sweep.cleaner.ui.screens.dashboard.DashboardScreen
import com.sweep.cleaner.ui.screens.documents.DocumentsScreen
import com.sweep.cleaner.ui.screens.downloads.OldDownloadsScreen
import com.sweep.cleaner.ui.screens.health.StorageHealthScreen
import com.sweep.cleaner.ui.screens.leftovers.AppLeftoversScreen
import com.sweep.cleaner.ui.screens.onboarding.OnboardingScreen
import com.sweep.cleaner.ui.screens.photos.PhotosTabScreen
import com.sweep.cleaner.ui.screens.saf.SafStorageScanScreen
import com.sweep.cleaner.ui.screens.screenshots.ScreenshotsScreen
import com.sweep.cleaner.ui.screens.settings.SettingsScreen
import com.sweep.cleaner.ui.screens.similar.SimilarPhotosScreen
import com.sweep.cleaner.ui.screens.smartscan.SmartScanScreen
import com.sweep.cleaner.ui.screens.tools.ToolsScreen
import com.sweep.cleaner.ui.screens.trash.RecentlyDeletedScreen
import com.sweep.cleaner.ui.screens.videos.LargeVideosScreen
import com.sweep.cleaner.ui.screens.videos.VideosTabScreen
import com.sweep.cleaner.ui.theme.BrandTeal
import com.sweep.cleaner.ui.theme.BrandTealMint
import com.sweep.cleaner.ui.viewmodel.SweepViewModel

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    data object Home : BottomNavItem(NavRoutes.DASHBOARD, "Home", Icons.Default.Home)
    data object Photos : BottomNavItem(NavRoutes.PHOTOS_TAB, "Photos", Icons.Default.PhotoLibrary)
    data object Videos : BottomNavItem(NavRoutes.VIDEOS_TAB, "Videos", Icons.Default.VideoLibrary)
    data object Tools : BottomNavItem(NavRoutes.TOOLS, "Tools", Icons.Default.Build)
}

@Composable
fun SweepAppNavigation(
    viewModel: SweepViewModel,
    tutorialSeen: Boolean,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Photos,
        BottomNavItem.Videos,
        BottomNavItem.Tools
    )

    val showBottomBar = currentRoute in listOf(
        NavRoutes.DASHBOARD,
        NavRoutes.PHOTOS_TAB,
        NavRoutes.VIDEOS_TAB,
        NavRoutes.TOOLS
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("sweep_bottom_navigation")
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandTeal,
                                selectedTextColor = BrandTeal,
                                indicatorColor = BrandTealMint.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (tutorialSeen) NavRoutes.DASHBOARD else NavRoutes.ONBOARDING,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.ONBOARDING) {
                OnboardingScreen(
                    onFinished = {
                        viewModel.setTutorialSeen(true)
                        navController.navigate(NavRoutes.DASHBOARD) {
                            popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            composable(NavRoutes.DASHBOARD) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToSmartScan = { navController.navigate(NavRoutes.SMART_SCAN) },
                    onNavigateToSimilarPhotos = { navController.navigate(NavRoutes.SIMILAR_PHOTOS) },
                    onNavigateToScreenshots = { navController.navigate(NavRoutes.SCREENSHOTS) },
                    onNavigateToLargeVideos = { navController.navigate(NavRoutes.LARGE_VIDEOS) },
                    onNavigateToContacts = { navController.navigate(NavRoutes.DUPLICATE_CONTACTS) },
                    onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                    onNavigateToTrash = { navController.navigate(NavRoutes.RECENTLY_DELETED) },
                    onNavigateToBreakdown = { navController.navigate(NavRoutes.STORAGE_BREAKDOWN) },
                    onNavigateToSafScan = { navController.navigate(NavRoutes.SAF_SCAN) }
                )
            }

            composable(NavRoutes.PHOTOS_TAB) {
                PhotosTabScreen(
                    viewModel = viewModel,
                    onNavigateToSimilarPhotos = { navController.navigate(NavRoutes.SIMILAR_PHOTOS) },
                    onNavigateToScreenshots = { navController.navigate(NavRoutes.SCREENSHOTS) }
                )
            }

            composable(NavRoutes.VIDEOS_TAB) {
                VideosTabScreen(
                    viewModel = viewModel,
                    onNavigateToLargeVideos = { navController.navigate(NavRoutes.LARGE_VIDEOS) }
                )
            }

            composable(NavRoutes.TOOLS) {
                ToolsScreen(
                    viewModel = viewModel,
                    onNavigateToRoute = { route -> navController.navigate(route) }
                )
            }

            composable(NavRoutes.SMART_SCAN) {
                SmartScanScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToRoute = { route -> navController.navigate(route) }
                )
            }

            composable(NavRoutes.SIMILAR_PHOTOS) {
                SimilarPhotosScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.SCREENSHOTS) {
                ScreenshotsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.LARGE_VIDEOS) {
                LargeVideosScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.DOCUMENTS) {
                DocumentsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.AUDIO) {
                AudioScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.APKS) {
                ApkInstallersScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.OLD_DOWNLOADS) {
                OldDownloadsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.CHAT_MEDIA) {
                ChatMediaScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.DUPLICATE_CONTACTS) {
                DuplicateContactsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.APP_LEFTOVERS) {
                AppLeftoversScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.STORAGE_HEALTH) {
                StorageHealthScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.UNUSED_APPS) {
                UnusedAppsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.RECENTLY_DELETED) {
                RecentlyDeletedScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.STORAGE_BREAKDOWN) {
                StorageBreakdownScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToPhotos = { navController.navigate(NavRoutes.SIMILAR_PHOTOS) },
                    onNavigateToVideos = { navController.navigate(NavRoutes.LARGE_VIDEOS) },
                    onNavigateToAudio = { navController.navigate(NavRoutes.AUDIO) },
                    onNavigateToDocs = { navController.navigate(NavRoutes.DOCUMENTS) }
                )
            }

            composable(NavRoutes.SAF_SCAN) {
                SafStorageScanScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
