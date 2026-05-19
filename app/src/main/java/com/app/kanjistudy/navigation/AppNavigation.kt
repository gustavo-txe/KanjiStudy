package com.app.kanjistudy.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.kanjistudy.help.HelpScreen
import com.app.kanjistudy.home.kanjis.KanjiListScreen
import com.app.kanjistudy.learned.LearnedScreen
import com.app.kanjistudy.home.kanjis.components.HomeTopBar
import com.app.kanjistudy.scan.camerascan.ScanScreen
import com.app.kanjistudy.onboarding.OnboardingManager

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

@Composable
fun AppNavigation(
    onboardingManager: OnboardingManager,
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit) {
    val navController = rememberNavController()
    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route

    val routesWithTopBar = setOf(Screens.Learned.route, Screens.Home.route, Screens.Scan.route, Screens.Help.route)
    Scaffold(
        topBar = {
            if (currentRoute in routesWithTopBar) {
                HomeTopBar(
                    isDarkTheme = isDarkTheme,
                    onThemeChanged = onThemeChanged,
                    onHelpClick = { navController.navigate(Screens.Help.route) },
                    showBackButton = currentRoute == Screens.Help.route,
                    onBackClick = { navController.popBackStack() }                )
            }
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screens.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                val slideDirection = if (isForwardNavigation()) {
                    AnimatedContentTransitionScope.SlideDirection.Left
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Right
                }

                slideIntoContainer(
                    towards = slideDirection,
                    animationSpec = tween(
                        durationMillis = TRANSITION_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = TRANSITION_FADE_DURATION_MS)
                )
            },
            exitTransition = {
                val slideDirection = if (isForwardNavigation()) {
                    AnimatedContentTransitionScope.SlideDirection.Left
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Right
                }

                slideOutOfContainer(
                    towards = slideDirection,
                    animationSpec = tween(
                        durationMillis = TRANSITION_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeOut(
                    animationSpec = tween(durationMillis = TRANSITION_FADE_DURATION_MS)
                )
            },
            popEnterTransition = {
                val slideDirection = if (isForwardNavigation()) {
                    AnimatedContentTransitionScope.SlideDirection.Left
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Right
                }

                slideIntoContainer(
                    towards = slideDirection,
                    animationSpec = tween(
                        durationMillis = TRANSITION_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = TRANSITION_FADE_DURATION_MS)
                )
            },
            popExitTransition = {
                val slideDirection = if (isForwardNavigation()) {
                    AnimatedContentTransitionScope.SlideDirection.Left
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Right
                }

                slideOutOfContainer(
                    towards = slideDirection,
                    animationSpec = tween(
                        durationMillis = TRANSITION_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeOut(
                    animationSpec = tween(durationMillis = TRANSITION_FADE_DURATION_MS)
                )
            }
        ) {
            composable(Screens.Learned.route) { LearnedScreen(onboardingManager = onboardingManager) }
            composable(Screens.Home.route) { KanjiListScreen(onboardingManager = onboardingManager) }
            composable(Screens.Scan.route) { ScanScreen(onboardingManager = onboardingManager) }
            composable(Screens.Help.route) { HelpScreen() }

        }
    }
}

@Composable
fun AppBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier.navigationBarsPadding()
    ) {
        listOf(Screens.Learned, Screens.Home, Screens.Scan).forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = { onNavigate(screen.route) },
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) }
            )
        }
    }
}
