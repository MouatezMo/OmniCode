package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainScreen
import com.example.ui.SettingsScreen
import com.example.ui.WorkspaceViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.WorkspaceViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            val viewModelFactory = WorkspaceViewModelFactory(applicationContext)
            val viewModel: WorkspaceViewModel = viewModel(factory = viewModelFactory)
            
            NavHost(navController = navController, startDestination = "main") {
                composable("main") {
                    MainScreen(viewModel, onNavigateSettings = { navController.navigate("settings") })
                }
                composable("settings") {
                    SettingsScreen(viewModel, onBack = { navController.popBackStack() })
                }
            }
        }
      }
    }
  }
}
