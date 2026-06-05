package com.app.kanjistudy.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

const val KANJI_DOWNLOAD_IN_PROGRESS_MESSAGE =
    "Please wait until the kanji download finishes before importing or exporting."
const val PRIVACY_POLICY_URL =
    "https://sites.google.com/view/kanji-scanner-privacy-policy/home"
const val TERMS_OF_SERVICE_URL =
    "https://sites.google.com/view/kanji-scanner-terms-of-use/home"

@Composable
fun AppDrawer(
    drawerState: DrawerState,
    isDarkTheme: Boolean,
    isKanjiDownloading: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    onBackupClick: () -> Unit,
    onHelpClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    fun closeDrawerThen(action: () -> Unit) {
        coroutineScope.launch {
            drawerState.close()
            action()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                isDarkTheme = isDarkTheme,
                isKanjiDownloading = isKanjiDownloading,
                onThemeChanged = onThemeChanged,
                onBackupClick = { closeDrawerThen(onBackupClick) },
                onHelpClick = { closeDrawerThen(onHelpClick) },
                onPrivacyPolicyClick = { closeDrawerThen(onPrivacyPolicyClick) },
                onTermsClick = { closeDrawerThen(onTermsClick) }
            )
        },
        content = content
    )
}

@Composable
private fun AppDrawerContent(
    isDarkTheme: Boolean,
    isKanjiDownloading: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    onBackupClick: () -> Unit,
    onHelpClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsClick: () -> Unit
) {
    val themeRotation = animateFloatAsState(
        targetValue = if (isDarkTheme) 360f else 0f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "drawerThemeRotation"
    ).value

    ModalDrawerSheet(modifier = Modifier.widthIn(max = 270.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DrawerHeader()
            Spacer(modifier = Modifier.height(4.dp))
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        modifier = Modifier.rotate(themeRotation)
                    )
                },
                label = { Text(if (isDarkTheme) "Dark theme" else "Light theme") },
                selected = false,
                onClick = { onThemeChanged(!isDarkTheme) }
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.ImportExport, contentDescription = null) },
                label = { Text("Import / Export") },
                badge = if (isKanjiDownloading) {
                    { Text("Busy") }
                } else {
                    null
                },
                selected = false,
                onClick = onBackupClick
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) },
                label = { Text("Help") },
                selected = false,
                onClick = onHelpClick
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Policy, contentDescription = null) },
                label = { Text("Privacy Policy") },
                selected = false,
                onClick = onPrivacyPolicyClick
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Filled.Rule, contentDescription = null) },
                label = { Text("Terms of Service") },
                selected = false,
                onClick = onTermsClick
            )
        }
    }
}

@Composable
private fun DrawerHeader() {

    Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Kanji Scanner",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Settings",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
