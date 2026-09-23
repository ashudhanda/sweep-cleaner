package com.sweep.cleaner

import android.app.Application
import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.sweep.cleaner.model.JunkCategoryItem
import com.sweep.cleaner.model.JunkCleanStage
import com.sweep.cleaner.model.OneTapJunkState
import com.sweep.cleaner.model.StorageSummary
import com.sweep.cleaner.ui.navigation.SweepAppNavigation
import com.sweep.cleaner.ui.screens.breakdown.StorageBreakdownScreen
import com.sweep.cleaner.ui.screens.dashboard.DashboardScreen
import com.sweep.cleaner.ui.screens.junk.OneTapJunkCleanScreen
import com.sweep.cleaner.ui.screens.tools.ToolsScreen
import com.sweep.cleaner.ui.theme.SweepTheme
import com.sweep.cleaner.ui.viewmodel.SweepViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class UserFlowScreenshotsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var application: Application
    private lateinit var viewModel: SweepViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = SweepViewModel(application)
    }

    @Test
    fun capture_dashboard_before_cleaning() {
        composeTestRule.setContent {
            SweepTheme(themeMode = "light") {
                Surface {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToSmartScan = {},
                        onNavigateToSimilarPhotos = {},
                        onNavigateToScreenshots = {},
                        onNavigateToLargeVideos = {},
                        onNavigateToContacts = {},
                        onNavigateToSettings = {},
                        onNavigateToTrash = {},
                        onNavigateToBreakdown = {},
                        onNavigateToSafScan = {},
                        onNavigateToJunkClean = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "app/src/test/screenshots/1_dashboard_before_cleaning.png")
    }

    @Test
    fun capture_one_tap_junk_clean_ready() {
        val testCategories = listOf(
            JunkCategoryItem("cache", "App & Browser Cache", "Temporary downloaded cache from apps", 420_000_000L, 84, "cache"),
            JunkCategoryItem("logs", "Crash Logs & Diagnostics", "Old system reports and error dumps", 65_000_000L, 23, "logs"),
            JunkCategoryItem("apk", "Obsolete APK Installers", "Leftover application installation packages", 124_000_000L, 2, "apk"),
            JunkCategoryItem("thumbnail", "Image Thumbnail Cache", "Cached visual thumbnails", 75_000_000L, 310, "thumbnail")
        )
        val field = SweepViewModel::class.java.getDeclaredField("_oneTapJunkState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<OneTapJunkState>
        stateFlow.value = OneTapJunkState(
            stage = JunkCleanStage.READY,
            scanProgress = 1.0f,
            currentScanAction = "Analysis complete! Ready for one-tap sweep.",
            categories = testCategories,
            totalJunkBytes = testCategories.sumOf { it.bytes }
        )

        composeTestRule.setContent {
            SweepTheme(themeMode = "light") {
                Surface {
                    OneTapJunkCleanScreen(
                        viewModel = viewModel,
                        onBackClick = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "app/src/test/screenshots/2_one_tap_junk_screen.png")
    }

    @Test
    fun capture_one_tap_junk_clean_completed() {
        val testCategories = listOf(
            JunkCategoryItem("cache", "App & Browser Cache", "Temporary downloaded cache from apps", 420_000_000L, 84, "cache", isCleaned = true),
            JunkCategoryItem("logs", "Crash Logs & Diagnostics", "Old system reports and error dumps", 65_000_000L, 23, "logs", isCleaned = true),
            JunkCategoryItem("apk", "Obsolete APK Installers", "Leftover application installation packages", 124_000_000L, 2, "apk", isCleaned = true),
            JunkCategoryItem("thumbnail", "Image Thumbnail Cache", "Cached visual thumbnails", 75_000_000L, 310, "thumbnail", isCleaned = true)
        )
        val field = SweepViewModel::class.java.getDeclaredField("_oneTapJunkState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<OneTapJunkState>
        stateFlow.value = OneTapJunkState(
            stage = JunkCleanStage.COMPLETED,
            scanProgress = 1.0f,
            currentScanAction = "Storage swept cleanly!",
            categories = testCategories,
            totalJunkBytes = 684_000_000L,
            reclaimedBytes = 684_000_000L,
            totalCleanedItems = 419,
            cleanDurationMs = 1840L
        )

        composeTestRule.setContent {
            SweepTheme(themeMode = "light") {
                Surface {
                    OneTapJunkCleanScreen(
                        viewModel = viewModel,
                        onBackClick = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "app/src/test/screenshots/3_one_tap_junk_completed.png")
    }

    @Test
    fun capture_dashboard_after_cleaning() {
        val field = SweepViewModel::class.java.getDeclaredField("_dashboardJunkBytes")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val junkFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<Long>
        junkFlow.value = 0L

        composeTestRule.setContent {
            SweepTheme(themeMode = "light") {
                Surface {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToSmartScan = {},
                        onNavigateToSimilarPhotos = {},
                        onNavigateToScreenshots = {},
                        onNavigateToLargeVideos = {},
                        onNavigateToContacts = {},
                        onNavigateToSettings = {},
                        onNavigateToTrash = {},
                        onNavigateToBreakdown = {},
                        onNavigateToSafScan = {},
                        onNavigateToJunkClean = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "app/src/test/screenshots/4_dashboard_after_clean.png")
    }

    @Test
    fun capture_tools_screen() {
        composeTestRule.setContent {
            SweepTheme(themeMode = "light") {
                Surface {
                    ToolsScreen(
                        viewModel = viewModel,
                        onNavigateToRoute = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "app/src/test/screenshots/5_tools_screen.png")
    }

    @Test
    fun capture_full_storage_breakdown_screen() {
        val field = SweepViewModel::class.java.getDeclaredField("_storageSummary")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val summaryFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<StorageSummary>
        summaryFlow.value = StorageSummary(
            totalBytes = 128_000_000_000L,
            usedBytes = 76_400_000_000L,
            freeBytes = 51_600_000_000L,
            photoBytes = 18_200_000_000L,
            videoBytes = 32_500_000_000L,
            audioBytes = 4_800_000_000L,
            docBytes = 3_100_000_000L,
            otherBytes = 17_800_000_000L,
            photoCount = 1693,
            videoCount = 33,
            audioCount = 142,
            docCount = 89
        )

        composeTestRule.setContent {
            SweepTheme(themeMode = "light") {
                Surface {
                    StorageBreakdownScreen(
                        viewModel = viewModel,
                        onBackClick = {},
                        onNavigateToPhotos = {},
                        onNavigateToVideos = {},
                        onNavigateToAudio = {},
                        onNavigateToDocs = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "app/src/test/screenshots/6_storage_breakdown_screen.png")
    }
}
