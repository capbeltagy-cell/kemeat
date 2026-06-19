package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.MainGameContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.GameViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val myApp = application as MyApplication
    val gameViewModelFactory = GameViewModelFactory(myApp, myApp.repository)

    // Warm up Google AdMob caches on start
    try {
        com.example.ui.AdManager.loadInterstitial(this)
        com.example.ui.AdManager.loadRewarded(this)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    setContent {
      MyApplicationTheme {
        val viewModel: GameViewModel = viewModel(factory = gameViewModelFactory)
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          MainGameContainer(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}
