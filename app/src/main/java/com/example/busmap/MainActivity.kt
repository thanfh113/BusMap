package com.example.busmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.busmap.ui.theme.BusMapTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Khởi tạo Firebase
        FirebaseApp.initializeApp(this)
        // Cấu hình App Check
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        Log.d("BusMap", "Firebase App Check initialized")
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