package com.example.busmap

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.busmap.firebase.FirebaseManager
import com.example.busmap.ui.theme.BusMapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Test Firebase initialization
        try {
            val isFirebaseReady = FirebaseManager.isInitialized()
            Log.d("MainActivity", "Firebase initialization status: $isFirebaseReady")
            
            if (isFirebaseReady) {
                Log.d("MainActivity", "Firebase Database references created successfully")
                Log.d("MainActivity", "Bus routes reference: ${FirebaseManager.getBusRoutesReference()}")
                Log.d("MainActivity", "Bus stations reference: ${FirebaseManager.getBusStationsReference()}")
                Log.d("MainActivity", "Bus locations reference: ${FirebaseManager.getBusLocationsReference()}")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase initialization failed", e)
        }
        
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