package com.example.busmap.firebase

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference

/**
 * Firebase Realtime Database manager for BusMap application
 */
object FirebaseManager {
    
    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }
    
    /**
     * Get reference to bus routes data
     */
    fun getBusRoutesReference(): DatabaseReference {
        return database.getReference("bus_routes")
    }
    
    /**
     * Get reference to bus stations data  
     */
    fun getBusStationsReference(): DatabaseReference {
        return database.getReference("bus_stations")
    }
    
    /**
     * Get reference to real-time bus locations
     */
    fun getBusLocationsReference(): DatabaseReference {
        return database.getReference("bus_locations")
    }
    
    /**
     * Check if Firebase is properly initialized
     */
    fun isInitialized(): Boolean {
        return try {
            database.reference
            true
        } catch (e: Exception) {
            false
        }
    }
}