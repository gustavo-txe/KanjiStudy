package com.app.kanjistudy.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
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
import com.app.kanjistudy.onboarding.OnboardingManager
import com.app.kanjistudy.scan.camerascan.ScanScreen
import com.app.kanjistudy.usecase.CustomTab
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val TRANSITION_DURATION_MS = 420
private const val TRANSITION_FADE_DURATION_MS = 280

private fun screenIndex(route: String?): Int = when (route) {
    Screens.Learned.route -> 0
    Screens.Home.route -> 1
    Screens.Scan.route -> 2
    else -> 1
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isForwardNavigation(): Boolean =
    screenIndex(targetState.destination.route) > screenIndex(initialState.destination.route)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(onboardingManager: OnboardingManager, isDarkTheme: Boolean, onThemeChanged: (Boolean) -> Unit) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val customTab = remember { CustomTab() }
    val context = LocalContext.current

    val kanjiViewModel = androidx.hilt.navigation.compose.hiltViewModel<KanjiViewModel>()
    val learnedViewModel = androidx.hilt.navigation.compose.hiltViewModel<LearnedViewModel>()
    val kanjiState by kanjiViewModel.uiState.collectAsStateWithLifecycle()
    val learnedState by learnedViewModel.uiState.collectAsStateWithLifecycle()

    val showJlptFilter = currentRoute == Screens.Home.route || currentRoute == Screens.Learned.route
    val selectedJlpt = if (currentRoute == Screens.Home.route) kanjiState.selectedJlptLevel else learnedState.selectedJlptLevel

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.widthIn(max = 230.dp)) {
                val themeRotation = animateFloatAsState(
                    targetValue = if (isDarkTheme) 360f else 0f,
                    animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
                    label = "drawerThemeRotation"
                ).value

                Text("Settings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp))
                NavigationDrawerItem(
                    icon = { Icon(if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode, null,
                        modifier = Modifier.rotate(themeRotation)) },
                    label = { Text("Dark theme") },
                    selected = false,
                    onClick = { onThemeChanged(!isDarkTheme) }
                )
                Spacer(modifier = Modifier.height(4.dp))

                NavigationDrawerItem(icon = { Icon(Icons.Default.Help, null) },
                    label = { Text("Help") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; navController.navigate(Screens.Help.route) })

                NavigationDrawerItem(icon = { Icon(Icons.Default.Policy, null) },
                    label = { Text("Privacy Policy") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() };
                        customTab.openCustomTab(context, "https://sites.google.com/view/kanji-scanner-privacy-policy/home") })

                NavigationDrawerItem(icon = { Icon(Icons.Default.Rule, null) },
                    label = { Text("Terms of Service") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() };
                        customTab.openCustomTab(context, "https://sites.google.com/view/kanji-scanner-terms-of-use/home") })
            }
        }
    ) {
        Scaffold(
            topBar = {
                HomeTopBar(
                    isDarkTheme = isDarkTheme,
                    showJlptFilter = showJlptFilter,
                    selectedJlptLevel = selectedJlpt,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenJlptFilter = {
                        val next = when (selectedJlpt) { null -> 5; 5 -> 4; 4 -> 3; 3 -> 2; 2 -> 1; else -> null }
                        if (currentRoute == Screens.Home.route) kanjiViewModel.onJlptLevelSelected(next) else learnedViewModel.onJlptLevelSelected(next)
                    },
                    onThemeChanged = onThemeChanged,
                    showBackButton = currentRoute == Screens.Help.route,
                    onBackClick = { navController.popBackStack() }
                )
            },
            bottomBar = {
                AppBottomBar(currentRoute = currentRoute, onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
        ) { innerPadding ->
            NavHost(navController = navController, startDestination = Screens.Home.route, modifier = Modifier.padding(innerPadding), enterTransition = {
                val d = if (isForwardNavigation()) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right
                slideIntoContainer(d, tween(TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)) + fadeIn(tween(TRANSITION_FADE_DURATION_MS))
            }, exitTransition = {
                val d = if (isForwardNavigation()) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right
                slideOutOfContainer(d, tween(TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)) + fadeOut(tween(TRANSITION_FADE_DURATION_MS))
            }) {
                composable(Screens.Learned.route) { LearnedScreen(viewModel = learnedViewModel, onboardingManager = onboardingManager) }
                composable(Screens.Home.route) { KanjiListScreen(viewModel = kanjiViewModel, onboardingManager = onboardingManager) }
                composable(Screens.Scan.route) { ScanScreen(onboardingManager = onboardingManager) }
                composable(Screens.Help.route) { HelpScreen() }
            }
        }
    }
}

@Composable
fun AppBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar(modifier = Modifier.navigationBarsPadding()) {
        listOf(Screens.Learned, Screens.Home, Screens.Scan).forEach { screen ->
            NavigationBarItem(selected = currentRoute == screen.route, onClick = { onNavigate(screen.route) }, icon = { Icon(screen.icon, contentDescription = screen.title) }, label = { Text(screen.title) })
        }
    }
}