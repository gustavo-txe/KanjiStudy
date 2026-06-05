package com.app.kanjistudy.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.kanjistudy.help.HelpScreen
import com.app.kanjistudy.home.kanjis.KanjiListScreen
import com.app.kanjistudy.home.kanjis.KanjiViewModel
import com.app.kanjistudy.home.kanjis.components.HomeTopBar
import com.app.kanjistudy.learned.LearnedScreen
import com.app.kanjistudy.learned.LearnedViewModel
import com.app.kanjistudy.learned.io.IOKanjiViewModel
import com.app.kanjistudy.onboarding.OnboardingManager
import com.app.kanjistudy.scan.camerascan.ScanScreen
import com.app.kanjistudy.usecase.CustomTab
import kotlinx.coroutines.launch

private const val LEARNED_KANJI_BACKUP_FILE_NAME = "learned-kanji.json"

@Composable
fun AppNavigation(
    onboardingManager: OnboardingManager,
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val customTab = remember { CustomTab() }

    val kanjiViewModel = hiltViewModel<KanjiViewModel>()
    val learnedViewModel = hiltViewModel<LearnedViewModel>()
    val ioKanjiViewModel = hiltViewModel<IOKanjiViewModel>()

    val kanjiState by kanjiViewModel.uiState.collectAsStateWithLifecycle()
    val learnedState by learnedViewModel.uiState.collectAsStateWithLifecycle()
    val transferState by ioKanjiViewModel.uiState.collectAsStateWithLifecycle()

    var showBackupDialog by remember { mutableStateOf(false) }
    val isKanjiDownloading = kanjiState.isLoading
    val showJlptFilter = currentRoute == Screens.Home.route || currentRoute == Screens.Learned.route
    val selectedJlptLevel = when (currentRoute) {
        Screens.Home.route -> kanjiState.selectedJlptLevel
        Screens.Learned.route -> learnedState.selectedJlptLevel
        else -> null
    }

    fun showKanjiDownloadMessage() {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(KANJI_DOWNLOAD_IN_PROGRESS_MESSAGE)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { selectedUri ->
            if (isKanjiDownloading) {
                showKanjiDownloadMessage()
            } else {
                ioKanjiViewModel.exportLearnedKanjis(selectedUri)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            if (isKanjiDownloading) {
                showKanjiDownloadMessage()
            } else {
                ioKanjiViewModel.importLearnedKanjis(selectedUri)
            }
        }
    }

    LaunchedEffect(transferState.userMessage) {
        transferState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            ioKanjiViewModel.clearUserMessage()
        }
    }

    AppDrawer(
        drawerState = drawerState,
        isDarkTheme = isDarkTheme,
        isKanjiDownloading = isKanjiDownloading,
        onThemeChanged = onThemeChanged,
        onBackupClick = {
            if (isKanjiDownloading) {
                showKanjiDownloadMessage()
            } else {
                showBackupDialog = true
            }
        },
        onHelpClick = { navController.navigateSingleTop(Screens.Help.route) },
        onPrivacyPolicyClick = {
            customTab.openCustomTab(context, PRIVACY_POLICY_URL)
        },
        onTermsClick = {
            customTab.openCustomTab(context, TERMS_OF_SERVICE_URL)
        }
    ) {
        if (showBackupDialog) {
            LearnedKanjiBackupDialog(
                isBusy = transferState.isBusy,
                isKanjiDownloading = isKanjiDownloading,
                busyMessage = transferState.busyMessage,
                onDismiss = { if (!transferState.isBusy) showBackupDialog = false },
                onImportClick = {
                    if (isKanjiDownloading) {
                        showKanjiDownloadMessage()
                    } else {
                        importLauncher.launch(LEARNED_KANJI_IMPORT_MIME_TYPES)
                    }
                },
                onExportClick = {
                    if (isKanjiDownloading) {
                        showKanjiDownloadMessage()
                    } else {
                        exportLauncher.launch(LEARNED_KANJI_BACKUP_FILE_NAME)
                    }
                }
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                HomeTopBar(
                    isDarkTheme = isDarkTheme,
                    showJlptFilter = showJlptFilter,
                    selectedJlptLevel = selectedJlptLevel,
                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                    onOpenJlptFilter = {
                        val nextLevel = nextJlptLevel(selectedJlptLevel)
                        when (currentRoute) {
                            Screens.Home.route -> kanjiViewModel.onJlptLevelSelected(nextLevel)
                            Screens.Learned.route -> learnedViewModel.onJlptLevelSelected(nextLevel)
                        }
                    },
                    onThemeChanged = onThemeChanged,
                    showBackButton = currentRoute == Screens.Help.route,
                    onBackClick = { navController.popBackStack() }
                )
            },
            bottomBar = {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = navController::navigateToTopLevel
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screens.Home.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { materialEnterTransition() },
                exitTransition = { materialExitTransition() }
            ) {
                composable(Screens.Learned.route) {
                    LearnedScreen(
                        viewModel = learnedViewModel,
                        onboardingManager = onboardingManager
                    )
                }
                composable(Screens.Home.route) {
                    KanjiListScreen(
                        viewModel = kanjiViewModel,
                        onboardingManager = onboardingManager
                    )
                }
                composable(Screens.Scan.route) {
                    ScanScreen(onboardingManager = onboardingManager)
                }
                composable(Screens.Help.route) {
                    HelpScreen()
                }
            }
        }
    }
}

private fun nextJlptLevel(currentLevel: Int?): Int? = when (currentLevel) {
    null -> 5
    5 -> 4
    4 -> 3
    3 -> 2
    2 -> 1
    else -> null
}

private fun NavController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}
