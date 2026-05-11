package com.app.kanjistudy.navigation

import android.os.Build
import androidx.annotation.RequiresApi
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
