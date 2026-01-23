package com.app.kanjistudy.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screens(val route: String, val title: String, val icon: ImageVector) {

    object Learned : Screens("learned", "Learned", Icons.Default.Create)
    object Home : Screens("home", "Kanjis", Icons.Default.Home)
    object Scan : Screens("scan", "Scan", Icons.Default.Camera)

}