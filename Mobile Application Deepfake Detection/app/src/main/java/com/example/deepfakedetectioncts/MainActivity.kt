package com.example.deepfakedetectioncts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.deepfakedetectioncts.ui.theme.DeepfakeDetectionCTSTheme
import com.example.deepfakedetectioncts.ui.theme.bubble.BubbleService
import com.example.deepfakedetectioncts.ui.theme.screens.HistoryScreen
import com.example.deepfakedetectioncts.ui.theme.screens.HistoryViewModel
import com.example.deepfakedetectioncts.ui.theme.screens.SettingsPanel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            DeepfakeDetectionCTSTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var captureEnabled by remember { mutableStateOf(BubbleService.isRunning) }
    val historyViewModel: HistoryViewModel = viewModel()
    val selectedScreenshots by historyViewModel.selectedScreenshots.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                historyViewModel.loadScreenshots()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
                SettingsPanel { captureEnabled = it }
            }
        },
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(if (selectedScreenshots.isEmpty()) "Recents" else "${selectedScreenshots.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (selectedScreenshots.isNotEmpty()) {
                                historyViewModel.clearSelection()
                            } else {
                                scope.launch { drawerState.open() }
                            }
                        }) {
                            if (selectedScreenshots.isNotEmpty()) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Clear Selection")
                            } else {
                                Icon(Icons.Default.Menu, contentDescription = "Settings")
                            }
                        }
                    },
                    actions = {
                        if (selectedScreenshots.isNotEmpty()) {
                            IconButton(onClick = { historyViewModel.deleteSelected() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Box(modifier = Modifier.weight(1f)) {
                    HistoryScreen(captureEnabled, viewModel = historyViewModel)
                }
                val screenshots by historyViewModel.screenshots.collectAsState()
                if (screenshots.isNotEmpty() && selectedScreenshots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(onClick = { historyViewModel.clearRecents() }) {
                            Text("Clear Recents", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}