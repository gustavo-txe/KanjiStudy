package com.app.kanjistudy

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screens(val route: String, val title: String, val icon: ImageVector) {
    object Learned : Screens("learned", "Learned", Icons.Default.Create)
    object Home : Screens("home", "Kanjis", Icons.Default.Home)
    object Review : Screens("review", "Review", Icons.Default.DateRange)
}