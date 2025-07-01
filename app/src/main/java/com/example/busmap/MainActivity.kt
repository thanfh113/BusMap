package com.example.busmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.busmap.ui.theme.BusMapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BusMapTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DismissKeyboard {
                        val navController = rememberNavController()
                        AppNavHost(navController, modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}