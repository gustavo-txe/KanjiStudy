package com.app.kanjistudy.help

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.kanjistudy.home.kanjis.components.HomeTopBar
import com.app.kanjistudy.theme.KanjiStudyTheme
import com.app.kanjistudy.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HelpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsStateWithLifecycle()

            KanjiStudyTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    topBar = {
                        HomeTopBar(
                            isDarkTheme = isDarkTheme,
                            showJlptFilter = false,
                            selectedJlptLevel = null,
                            onOpenDrawer = {},
                            onOpenJlptFilter = {},
                            onThemeChanged = themeViewModel::onThemeChanged,
                            showBackButton = true,
                            onBackClick = { finish() }
                        )
                    }
                ) { innerPadding ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        HelpScreen()
                    }
                }
            }
        }
    }
}