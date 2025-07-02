# Firebase Realtime Database Integration

## Overview
This project now includes Firebase Realtime Database dependencies for real-time bus tracking and data management.

## Dependencies Added

### Root build.gradle.kts
- Google Services plugin v4.4.2

### app/build.gradle.kts
- Firebase BOM v33.6.0 (manages all Firebase library versions)
- Firebase Realtime Database Kotlin extensions

## Usage

### FirebaseManager
A utility class has been created to manage Firebase database references:

```kotlin
import com.example.busmap.firebase.FirebaseManager

// Get database references
val busRoutesRef = FirebaseManager.getBusRoutesReference()
val busStationsRef = FirebaseManager.getBusStationsReference()
val busLocationsRef = FirebaseManager.getBusLocationsReference()

// Check if Firebase is initialized
val isReady = FirebaseManager.isInitialized()
```

### Database Structure
The database references follow this structure:
- `bus_routes/` - Bus route information
- `bus_stations/` - Bus station data
- `bus_locations/` - Real-time bus location updates

## Configuration
- Firebase project is already configured with `google-services.json`
- Database URL: `https://busapp-b2daa-default-rtdb.asia-southeast1.firebasedatabase.app`

## Next Steps
1. Define data models for bus routes, stations, and locations
2. Implement data reading/writing operations
3. Add real-time listeners for live updates
4. Handle offline capabilities