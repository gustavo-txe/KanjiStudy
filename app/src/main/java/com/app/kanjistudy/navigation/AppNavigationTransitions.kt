package com.app.kanjistudy.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

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

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideDirection(): AnimatedContentTransitionScope.SlideDirection =
    if (isForwardNavigation()) {
        AnimatedContentTransitionScope.SlideDirection.Left
    } else {
        AnimatedContentTransitionScope.SlideDirection.Right
    }

fun AnimatedContentTransitionScope<NavBackStackEntry>.materialEnterTransition(): EnterTransition =
    slideIntoContainer(
        towards = slideDirection(),
        animationSpec = tween(TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
    ) + fadeIn(tween(TRANSITION_FADE_DURATION_MS))

fun AnimatedContentTransitionScope<NavBackStackEntry>.materialExitTransition(): ExitTransition =
    slideOutOfContainer(
        towards = slideDirection(),
        animationSpec = tween(TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
    ) + fadeOut(tween(TRANSITION_FADE_DURATION_MS))