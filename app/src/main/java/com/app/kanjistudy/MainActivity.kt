package com.app.kanjistudy

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.kanjistudy.ui.theme.KanjiStudyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KanjiStudyTheme {
                BottomNavigationBar()
            }
        }
    }

    fun openCustomTab(context: Context, url: String) {
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }

    @Composable
    fun SearchBar(onSearch: (String) -> Unit) {
        var query by remember { mutableStateOf("") }

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            label = { Text("Search for kanjis, meanings...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )
    }

    @Composable
    fun KanjiListScreen(viewModel: KanjiViewModel = hiltViewModel()) {
        val kanjis by viewModel.joyoKanjis
        var filteredList by remember { mutableStateOf(kanjis) }
        val kanjisKunMap by viewModel.kanjisKunMap
        val kanjisOnMap by viewModel.kanjisOnMap
        val kanjisMeanings by viewModel.kanjisMeanings

        LaunchedEffect(key1 = kanjis) {
            kanjis.forEach { kanji ->
                viewModel.getKanjisKun(kanji)
                viewModel.getKanjisOn(kanji)
                viewModel.getJoyoKanjiMeanings(kanji)

                filteredList = kanjis
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "JLPT N5",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.CenterHorizontally)
            )
            SearchBar { query ->
                val lowerQuery = query.trim().lowercase()

                filteredList = kanjis.filter { kanji ->
                    val kun = kanjisKunMap[kanji]?.joinToString(" ")?.lowercase() ?: ""
                    val on = kanjisOnMap[kanji]?.joinToString(" ")?.lowercase() ?: ""
                    val mean = kanjisMeanings[kanji]?.joinToString(" ")?.lowercase() ?: ""

                    kanji.lowercase().contains(lowerQuery) ||
                            kun.contains(lowerQuery) ||
                            on.contains(lowerQuery) ||
                            mean.contains(lowerQuery)
                }
            }

            LazyColumn {
                items(filteredList) { kanjis ->
                    val context = LocalContext.current
                    var showDialog by remember { mutableStateOf(false) }
                    val kunReadings = kanjisKunMap[kanjis] ?: listOf("loading...")
                    val onReadings = kanjisOnMap[kanjis] ?: listOf("loading...")
                    val kanjisMeanings = kanjisMeanings[kanjis] ?: listOf("loading...")

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 16.dp)
                            .clickable {
                                showDialog = true
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Bottom

                        ) {
                            Text(
                                text = kanjis, fontSize = 150.sp, modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentSize(Alignment.Center),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start

                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {

                                    Text(
                                        "Kun'yomi:",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    kunReadings.forEach {
                                        Text(
                                            text = it, fontSize = 15.sp,
                                        )
                                    }
                                }
                                Column(modifier = Modifier.padding(16.dp)) {

                                    Text(
                                        "On'yomi:",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    onReadings.forEach {
                                        Text(
                                            text = it, fontSize = 15.sp,
                                        )
                                    }
                                }
                                Column(modifier = Modifier.padding(16.dp)) {

                                    Text(
                                        "Meanings:",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    kanjisMeanings.forEach {
                                        Text(
                                            text = it, fontSize = 15.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = { Text("Open dictionary?") },
                            text = { Text("Would you like to search for:${kanjis} in Jisho?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    openCustomTab(context, "https://jisho.org/search/$kanjis")
                                    showDialog = false
                                }) {
                                    Text("Yes")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDialog = false }) {
                                    Text("No")
                                }
                            }
                        )
                    }
                }
            }
        }


    }

    @Composable
    fun BottomNavigationBar() {
        val navController = rememberNavController()
        Scaffold(
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .navigationBarsPadding(),
                    //backgroundColor = Color(0xFF301962),
                    //elevation = 8.dp
                ) {
                    val currentRoute =
                        navController.currentBackStackEntryAsState().value?.destination?.route
                    listOf(Screens.Learned, Screens.Home, Screens.Review).forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    screen.icon, contentDescription = screen.title,

                                    )
                            },
                            label = {
                                Text(
                                    screen.title,
                                )
                            },

                            )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screens.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screens.Learned.route) { LearnedScreen() }
                composable(Screens.Home.route) { KanjiListScreen() }
                composable(Screens.Review.route) { ReviewScreen() }
            }
        }
    }

    @Composable
    fun LearnedScreen() {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { Toast.makeText(this, "Add...", Toast.LENGTH_SHORT).show() },
                    containerColor = Color.LightGray,
                    contentColor = Color.Black,
                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()

                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            },
            floatingActionButtonPosition = FabPosition.End,
            content = { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    // Conteúdo da tela...
                }
            }
        )

        Text("to do...")
    }
}

@Composable
fun ReviewScreen() {
    Text("to do...")
}




