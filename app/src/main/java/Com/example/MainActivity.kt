package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.ContactScreen
import com.example.ui.screens.DiseaseDetectionScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PredictionHistoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CropViewModel

enum class ScreenRoute {
    Home, Scan, History, About, Contact
}

class MainActivity : ComponentActivity() {
    private val viewModel: CropViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppLayout(viewModel)
            }
        }
    }
}

@Composable
fun MainAppLayout(viewModel: CropViewModel) {
    var currentScreen by remember { mutableStateOf(ScreenRoute.Home) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentScreen == ScreenRoute.Home,
                    onClick = { currentScreen = ScreenRoute.Home },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == ScreenRoute.Home) Icons.Filled.Eco else Icons.Outlined.Eco,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home", style = androidx.compose.material3.MaterialTheme.typography.labelSmall) }
                )

                NavigationBarItem(
                    selected = currentScreen == ScreenRoute.Scan,
                    onClick = { currentScreen = ScreenRoute.Scan },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == ScreenRoute.Scan) Icons.Filled.QrCodeScanner else Icons.Outlined.QrCodeScanner,
                            contentDescription = "Scan"
                        )
                    },
                    label = { Text("Scan", style = androidx.compose.material3.MaterialTheme.typography.labelSmall) }
                )

                NavigationBarItem(
                    selected = currentScreen == ScreenRoute.History,
                    onClick = { currentScreen = ScreenRoute.History },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == ScreenRoute.History) Icons.Filled.Archive else Icons.Outlined.Archive,
                            contentDescription = "Archives"
                        )
                    },
                    label = { Text("Archives", style = androidx.compose.material3.MaterialTheme.typography.labelSmall) }
                )

                NavigationBarItem(
                    selected = currentScreen == ScreenRoute.About,
                    onClick = { currentScreen = ScreenRoute.About },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == ScreenRoute.About) Icons.Filled.Info else Icons.Outlined.Info,
                            contentDescription = "About"
                        )
                    },
                    label = { Text("About", style = androidx.compose.material3.MaterialTheme.typography.labelSmall) }
                )

                NavigationBarItem(
                    selected = currentScreen == ScreenRoute.Contact,
                    onClick = { currentScreen = ScreenRoute.Contact },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == ScreenRoute.Contact) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Support"
                        )
                    },
                    label = { Text("Support", style = androidx.compose.material3.MaterialTheme.typography.labelSmall) }
                )
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentScreen,
            modifier = Modifier.padding(innerPadding),
            label = "screen_transitions"
        ) { screen ->
            when (screen) {
                ScreenRoute.Home -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToScan = { currentScreen = ScreenRoute.Scan }
                )
                ScreenRoute.Scan -> DiseaseDetectionScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = ScreenRoute.Home }
                )
                ScreenRoute.History -> PredictionHistoryScreen(
                    viewModel = viewModel
                )
                ScreenRoute.About -> AboutScreen()
                ScreenRoute.Contact -> ContactScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
