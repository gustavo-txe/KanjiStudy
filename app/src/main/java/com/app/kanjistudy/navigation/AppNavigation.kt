package com.app.kanjistudy.navigation

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.kanjistudy.home.kanjis.KanjiListScreen
import com.app.kanjistudy.learned.LearnedScreen
import com.app.kanjistudy.home.kanjis.components.HomeTopBar
import com.app.kanjistudy.scan.ScanScreen
import com.app.kanjistudy.onboarding.OnboardingManager

@Composable
fun AppNavigation(
    onboardingManager: OnboardingManager
) {
    val navController = rememberNavController()
    var showTutorial by remember { mutableStateOf(onboardingManager.shouldShowTutorial()) }
    var tutorialStep by remember { mutableIntStateOf(0) }
    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route



    Scaffold(
        topBar = {
            if (currentRoute == Screens.Home.route) {
                HomeTopBar()
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
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screens.Learned.route) { LearnedScreen(onboardingManager = onboardingManager) }
            composable(Screens.Home.route) { KanjiListScreen(onboardingManager = onboardingManager) }
            composable(Screens.Scan.route) { ScanScreen(onboardingManager = onboardingManager) }
        }
    }

    if (showTutorial) {
        val steps = listOf(
            "Welcome to KanjiStudy! This quick tutorial will help you use the app features.",
            "Home: First, wait for the kanji download progress bar to complete. Only then kanji will be available for search and to mark/unmark as learned.",
            "Scan: Use OCR with your camera to detect kanji in real time. Tap a kanji to open details, or long-press to copy it.",
            "Learned: Review every Joyo kanji you marked as learned and track your progress.",
        )

        AlertDialog(
            onDismissRequest = { },
            title = { Text("Interactive Onboarding") },
            text = { Text(steps[tutorialStep]) },
            confirmButton = {
                TextButton(onClick = {
                    if (tutorialStep < steps.lastIndex) {
                        tutorialStep++
                    } else {
                        onboardingManager.setTutorialCompleted()
                        showTutorial = false
                    }
                }) {
                    Text(if (tutorialStep < steps.lastIndex) "Next" else "Finish")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onboardingManager.setTutorialCompleted()
                    showTutorial = false
                }) {
                    Text("Skip")
                }
            }
        )
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
