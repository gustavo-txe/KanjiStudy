package com.app.kanjistudy.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material3.Icon
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.kanjistudy.viewmodel.KanjiViewModel
import com.app.kanjistudy.R
import com.app.kanjistudy.ui.navigation.Screens
import com.app.kanjistudy.ui.scanner.components.CameraPermission
import com.app.kanjistudy.ui.scanner.components.CameraScreen
import com.app.kanjistudy.ui.theme.KanjiStudyTheme
import com.app.kanjistudy.viewmodel.LearnedViewModel
import com.app.kanjistudy.viewmodel.ScanViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.collections.joinToString

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        enableEdgeToEdge()
        setContent {
            KanjiStudyTheme {
                BottomNavigationBar()
            }
        }
    }


    @Composable
    fun KanjiListScreen(
        viewModel: KanjiViewModel = hiltViewModel(),
        learnedViewModel: LearnedViewModel = hiltViewModel()
    ) {

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val kanjisKunMap = uiState.kunReadings
        val kanjisOnMap = uiState.onReadings
        val kanjisMeanings = uiState.meanings
        val kanjis = uiState.joyoKanjis

        var query by remember { mutableStateOf("") }

        val infiniteTransition = rememberInfiniteTransition(label = "infiniteColor")

        val learnedKanjis by learnedViewModel.learnedKanjis.collectAsStateWithLifecycle()

        val learnedKanjiSet = remember(learnedKanjis) {
            learnedKanjis.filter { it.isLearned }.map { it.kanji }.toSet()
        }

        val animatedColor by infiniteTransition.animateColor(
            initialValue = Color(0xFF28D0A1),
            targetValue = Color(0xFF156B18),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "colorCycle"
        )

        val filteredList by remember(query, kanjis, kanjisKunMap, kanjisOnMap, kanjisMeanings) {
            derivedStateOf {
                val lowerQuery = query.trim().lowercase()
                if (lowerQuery.isBlank()) {
                    kanjis
                } else {
                    kanjis.filter { kanji ->
                        val kun = kanjisKunMap[kanji]?.joinToString(" ")?.lowercase() ?: ""
                        val on = kanjisOnMap[kanji]?.joinToString(" ")?.lowercase() ?: ""
                        val mean = kanjisMeanings[kanji]?.joinToString(" ")?.lowercase() ?: ""

                        kanji.lowercase().contains(lowerQuery) ||
                                kun.contains(lowerQuery) ||
                                on.contains(lowerQuery) ||
                                mean.contains(lowerQuery)
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            KanjiLoadingScreen(viewModel)

            SearchBar(onSearch = { query = it })

            LazyColumn {
                items(filteredList) { kanji ->
                    val context = LocalContext.current
                    var showDialog by remember { mutableStateOf(false) }
                    var showDialogAdd by remember { mutableStateOf(false) }

                    val kunReadings = kanjisKunMap[kanji] ?: listOf("loading...")
                    val onReadings = kanjisOnMap[kanji] ?: listOf("loading...")
                    val kanjisMeanings = kanjisMeanings[kanji] ?: listOf("loading...")

                    val isLearned = learnedKanjiSet.contains(kanji)

                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 16.dp, horizontal = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .align(Alignment.End)
                                .clickable {
                                    showDialogAdd = true
                                }) {
                            Icon(
                                painter = if (!isLearned) painterResource(id = R.drawable.baseline_add_24)
                                else painterResource(id = R.drawable.checkicon),
                                contentDescription = "Ícone",
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .size(40.dp),
                                tint = if (isLearned) animatedColor else Color.Gray,
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center

                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDialog = true }
                            ) {
                                Text(
                                    text = kanji,
                                    fontSize = 150.sp,
                                    modifier = Modifier.align(Alignment.Center),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
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
                            title = { Text("Open Google?") },
                            text = { Text("Would you like to search for:${kanji} in Google?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    openCustomTab(
                                        context,
                                        "https://www.google.com/search?q=kanji${kanji}"
                                    )
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

                    if (showDialogAdd) {
                        AlertDialog(
                            onDismissRequest = { showDialogAdd = false },
                            title = { if (isLearned) Text("Remove Kanji?") else Text("Add Kanji?") },
                            text = {
                                if (isLearned) Text("Would you like to remove the kanji:${kanji} as learned?")
                                else Text("Would you like to add the kanji:${kanji} as learned?")
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.markLearnedKanji(kanji)
                                    showDialogAdd = false
                                }) {
                                    Text("Yes")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDialogAdd = false }) {
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
        val currentRoute =
            navController.currentBackStackEntryAsState().value?.destination?.route

        Scaffold(
            topBar = {
                if (currentRoute == Screens.Home.route) {
                    HomeTopBar()
                }
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    listOf(Screens.Learned, Screens.Home, Screens.Scan).forEach { screen ->
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
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) }
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
                composable(Screens.Scan.route) { ScanScreen() }
            }
        }
    }


    @Composable
    fun LearnedScreen(
        learnedViewModel: LearnedViewModel = hiltViewModel(),
        viewModel: KanjiViewModel = hiltViewModel()
    ) {

        val learnedKanjis by learnedViewModel.learnedKanjis.collectAsStateWithLifecycle()
        var showDialog by remember { mutableStateOf(false) }

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val kanjisKunMap = uiState.kunReadings
        val kanjisOnMap = uiState.onReadings
        val kanjisMeanings = uiState.meanings
        val kanjis = uiState.joyoKanjis

        var query by remember { mutableStateOf("") }
        var selectedKanji by remember { mutableStateOf<String?>(null) }

        val filteredList by remember(query, kanjis, kanjisKunMap, kanjisOnMap, kanjisMeanings) {
            derivedStateOf {
                val lowerQuery = query.trim().lowercase()
                if (lowerQuery.isBlank()) {
                    kanjis
                } else {
                    kanjis.filter { kanji ->
                        val kun = kanjisKunMap[kanji]?.joinToString(" ")?.lowercase() ?: ""
                        val on = kanjisOnMap[kanji]?.joinToString(" ")?.lowercase() ?: ""
                        val mean = kanjisMeanings[kanji]?.joinToString(" ")?.lowercase() ?: ""

                        kanji.lowercase().contains(lowerQuery) ||
                                kun.contains(lowerQuery) ||
                                on.contains(lowerQuery) ||
                                mean.contains(lowerQuery)
                    }
                }
            }
        }

        val learnedKanjiSet by remember(learnedKanjis) {
            mutableStateOf(
                learnedKanjis
                    .filter { it.isLearned }
                    .map { it.kanji }
                    .toSet()
            )
        }

        val learnedFilteredList by remember(filteredList, learnedKanjiSet) {
            derivedStateOf {
                filteredList.filter { kanji ->
                    learnedKanjiSet.contains(kanji)
                }
            }
        }

        selectedKanji?.let { kanji ->

            val isLearned = learnedKanjiSet.contains(kanji)

            AlertDialog(
                onDismissRequest = { selectedKanji = null },
                title = { if (isLearned) Text("Remove Kanji?") },
                text = { if (isLearned) Text("Would you like to remove the kanji:${kanji} as learned?") },
                confirmButton = {
                    TextButton(onClick = {
                        learnedViewModel.markLearnedKanji(kanji)
                        selectedKanji = null
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedKanji = null }) {
                        Text("No")
                    }
                }
            )

        }



        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(learnedFilteredList) { kanji ->

                //val learnedStatus = learnedViewModel.learnedKanjis.collectAsStateWithLifecycle()
                /*val isLearned = learnedStatus.value
                    .find { it.kanji == kanji }?.isLearned ?: false*/

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clickable {
                            selectedKanji = kanji

                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = kanji,
                            fontSize = 60.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

            }


        }

    }

}

@Composable
fun KanjiLoadingScreen(viewModel: KanjiViewModel = hiltViewModel()) {
    val progress = viewModel.uiState.collectAsStateWithLifecycle().value.loadingProgress
    val isLoading = viewModel.uiState.collectAsStateWithLifecycle().value.isLoading
    val infiniteTransition = rememberInfiniteTransition(label = "infiniteColor1")
    val shineWidth = 500f

    val animatedColorTitle by infiniteTransition.animateFloat(
        initialValue = -shineWidth,
        targetValue = 500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "colorCycle1"
    )

    val brush = Brush.linearGradient(
        colors = if (isSystemInDarkTheme()) {
            listOf(
                Color.Transparent,
                Color.Black,
                Color.Transparent
            )
        } else {
            listOf(
                Color.Transparent,
                Color.White.copy(0.6f),
                Color.Transparent
            )
        },
        start = Offset(animatedColorTitle, 0f),
        end = Offset(animatedColorTitle + shineWidth, 0f)
    )

    if (isLoading) {
        Column(
            modifier = Modifier
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = ProgressIndicatorDefaults.linearColor,
                trackColor = ProgressIndicatorDefaults.linearTrackColor,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = brush, blendMode = BlendMode.Lighten)
                    }
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Downloading kanji. Please wait...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,

                        )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,

                        )
                }
            }
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
            .padding(12.dp, 4.dp, 12.dp, 4.dp)
    )
}

@Composable
fun AnimatedTitle(brush: Brush) {
    val kouzanBrush = FontFamily(Font(R.font.kouzanbrush))

    Box(
        modifier = Modifier
            .drawWithContent {
                drawContent()
                drawRect(brush = brush, blendMode = BlendMode.Lighten)
            }
            .padding(top = 8.dp)
    ) {
        Text(
            text = "常用漢字",
            fontSize = 30.sp,
            fontFamily = kouzanBrush,
            fontWeight = FontWeight.Bold,
            color = if (isSystemInDarkTheme()) Color.White else Color.Black
        )
    }
}


@SuppressLint("ContextCastToActivity")
@Composable
fun ScanScreen(viewModel: ScanViewModel = hiltViewModel()) {
    CameraPermission {
        CameraScreen(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "infiniteColor")

    val shineWidth = 250f

    val animatedColorTitle by infiniteTransition.animateFloat(
        initialValue = -shineWidth,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "colorCycle"
    )

    val brush = Brush.linearGradient(
        colors = if (isSystemInDarkTheme()) {
            listOf(
                Color.Transparent,
                Color.Black,
                Color.Transparent
            )
        } else {
            listOf(
                Color.Transparent,
                Color.White.copy(0.75f),
                Color.Transparent
            )
        },
        start = Offset(animatedColorTitle, 0f),
        end = Offset(animatedColorTitle + shineWidth, 0f)
    )

    TopAppBar(
        title = {
            AnimatedTitle(brush = brush)
        },
        actions = {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu"
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Privacy Policy") },
                    onClick = {
                        expanded = false
                        openCustomTab(
                            context,
                            "https://sites.google.com/view/kanji-scanner-privacy-policy/home"
                        )
                    }
                )

                DropdownMenuItem(
                    text = { Text("Terms of Service") },
                    onClick = {
                        expanded = false
                        openCustomTab(
                            context,
                            "https://sites.google.com/view/kanji-scanner-terms-of-use/home"
                        )
                    }
                )
            }
        }
    )
}











