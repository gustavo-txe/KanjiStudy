package com.app.kanjistudy.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.app.kanjistudy.R

sealed class Screens(val route: String, @androidx.annotation.StringRes val title: Int, val icon: ImageVector) {

    object Learned : Screens("learned", R.string.learned, Icons.Default.Create)
    object Home : Screens("home", R.string.kanji, Icons.Default.Home)
    object Scan : Screens("scan", R.string.scan, Icons.Default.Camera)
    object Help : Screens("help", R.string.help, Icons.AutoMirrored.Filled.Help)

}
