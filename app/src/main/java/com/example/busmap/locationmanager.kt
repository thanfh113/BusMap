package com.example.busmap

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import android.content.pm.PackageManager

@Composable
fun rememberLocationManager(snackbarHostState: SnackbarHostState? = null): LocationManagerState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationState = remember { mutableStateOf<GeoPoint?>(null) }
    val isLoadingState = remember { mutableStateOf(false) }
    val errorState = remember { mutableStateOf<String?>(null) }
    val locationCallback = remember { mutableStateOf<LocationCallback?>(null) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        println("=== PERMISSION RESULT ===")
        permissions.forEach { (permission, granted) ->
            println("Permission $permission: ${if (granted) "GRANTED" else "DENIED"}")
        }

        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            getLocationInternal(context, fusedLocationClient, snackbarHostState, locationState, isLoadingState, errorState, locationCallback, scope)
        } else {
            isLoadingState.value = false
            errorState.value = "Quyền truy cập vị trí bị từ chối"
            scope.launch {
                snackbarHostState?.showSnackbar(
                    message = "Quyền truy cập vị trí bị từ chối. Vui lòng cấp quyền để sử dụng tính năng định vị.",
                    actionLabel = "Cài đặt",
                    duration = SnackbarDuration.Long
                )?.let { result ->
                    if (result == SnackbarResult.ActionPerformed) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.fromParts("package", context.packageName, null)
                        context.startActivity(intent)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        println("=== LOCATION SETUP START ===")

        // Force clear any cached location first
        locationState.value = null

        // Debug location settings
        debugLocationSettings(context)

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            println("⚠️ Location services are disabled")
            errorState.value = "GPS chưa được bật"
            scope.launch {
                snackbarHostState?.showSnackbar(
                    message = "GPS chưa được bật. Vui lòng bật GPS để lấy vị trí hiện tại.",
                    actionLabel = "Bật GPS",
                    duration = SnackbarDuration.Long
                )?.let { result ->
                    if (result == SnackbarResult.ActionPerformed) {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                }
            }
            return@LaunchedEffect
        }

        if (hasLocationPermission(context)) {
            getLocationInternal(context, fusedLocationClient, snackbarHostState, locationState, isLoadingState, errorState, locationCallback, scope)
        } else {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            locationCallback.value?.let { callback ->
                try {
                    fusedLocationClient.removeLocationUpdates(callback)
                    println("Location updates stopped")
                } catch (e: Exception) {
                    println("Error stopping location updates: ${e.message}")
                }
            }
        }
    }

    return LocationManagerState(
        location = locationState.value,
        isLoading = isLoadingState.value,
        error = errorState.value,
        requestLocation = {
            // Clear current location before requesting new one
            locationState.value = null
            if (hasLocationPermission(context)) {
                getLocationInternal(context, fusedLocationClient, snackbarHostState, locationState, isLoadingState, errorState, locationCallback, scope)
            } else {
                permissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    )
}

data class LocationManagerState(
    val location: GeoPoint?,
    val isLoading: Boolean,
    val error: String?,
    val requestLocation: () -> Unit
)

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun debugLocationSettings(context: Context) {
    println("=== LOCATION DEBUG ===")
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    val passiveEnabled = locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)

    println("GPS Provider: ${if (gpsEnabled) "ENABLED" else "DISABLED"}")
    println("Network Provider: ${if (networkEnabled) "ENABLED" else "DISABLED"}")
    println("Passive Provider: ${if (passiveEnabled) "ENABLED" else "DISABLED"}")

    try {
        val locationMode = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.LOCATION_MODE
        )

        val modeString = when (locationMode) {
            Settings.Secure.LOCATION_MODE_OFF -> "OFF"
            Settings.Secure.LOCATION_MODE_SENSORS_ONLY -> "GPS_ONLY"
            Settings.Secure.LOCATION_MODE_BATTERY_SAVING -> "NETWORK_ONLY"
            Settings.Secure.LOCATION_MODE_HIGH_ACCURACY -> "HIGH_ACCURACY"
            else -> "UNKNOWN ($locationMode)"
        }
        println("Location Mode: $modeString")
    } catch (e: Exception) {
        println("Could not get location mode: ${e.message}")
    }

    println("Available providers: ${locationManager.getAllProviders()}")
    println("===================")
}

private fun getLocationInternal(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    snackbarHostState: SnackbarHostState?,
    locationState: MutableState<GeoPoint?>,
    isLoadingState: MutableState<Boolean>,
    errorState: MutableState<String?>,
    locationCallback: MutableState<LocationCallback?>,
    scope: kotlinx.coroutines.CoroutineScope
) {
    println("📡 Starting location request...")
    isLoadingState.value = true
    errorState.value = null

    try {
        if (hasLocationPermission(context)) {
            // Clear callback first if exists
            locationCallback.value?.let { callback ->
                fusedLocationClient.removeLocationUpdates(callback)
            }

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val age = System.currentTimeMillis() - location.time
                    println("📍 Last known location found:")
                    println("  Coordinates: ${location.latitude}, ${location.longitude}")
                    println("  Accuracy: ${location.accuracy}m")
                    println("  Age: ${age / 1000}s")
                    println("  Provider: ${location.provider}")

                    // More strict validation - must be recent and reasonably accurate
                    val isTooOld = age > 2 * 60 * 1000 // 2 minutes
                    val isInaccurate = location.accuracy > 200 // 200m

                    // Validate coordinates are reasonable
                    val isValidCoordinates = location.latitude != 0.0 && location.longitude != 0.0 &&
                            location.latitude >= -90.0 && location.latitude <= 90.0 &&
                            location.longitude >= -180.0 && location.longitude <= 180.0

                    if (!isTooOld && !isInaccurate && isValidCoordinates) {
                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                        locationState.value = geoPoint
                        isLoadingState.value = false
                        println("✅ Using last known location: ${location.latitude},${location.longitude}")
                        return@addOnSuccessListener
                    } else {
                        if (isTooOld) println("⚠️ Location rejected: too old (${age/1000}s)")
                        if (isInaccurate) println("⚠️ Location rejected: inaccurate (${location.accuracy}m)")
                        if (!isValidCoordinates) println("⚠️ Location rejected: invalid coordinates")
                    }
                }

                // Request fresh location immediately
                println("📡 Requesting fresh location updates...")
                requestLocationUpdates(context, fusedLocationClient, snackbarHostState, locationState, isLoadingState, errorState, locationCallback)
            }.addOnFailureListener { exception ->
                println("❌ Failed to get last location: ${exception.message}")
                requestLocationUpdates(context, fusedLocationClient, snackbarHostState, locationState, isLoadingState, errorState, locationCallback)
            }
        } else {
            isLoadingState.value = false
            errorState.value = "Không có quyền truy cập vị trí"
        }
    } catch (e: SecurityException) {
        isLoadingState.value = false
        errorState.value = "Lỗi bảo mật: ${e.message}"
        println("❌ SecurityException: ${e.message}")
    }
}

private fun requestLocationUpdates(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    snackbarHostState: SnackbarHostState?,
    locationState: MutableState<GeoPoint?>,
    isLoadingState: MutableState<Boolean>,
    errorState: MutableState<String?>,
    locationCallbackState: MutableState<LocationCallback?>
) {
    println("📡 Setting up location updates...")

    try {
        // High priority, frequent updates for better accuracy
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMaxUpdateAgeMillis(0L)
            .setWaitForAccurateLocation(true) // Wait for accurate location
            .setMaxUpdates(15) // More attempts
            .build()

        val callback = object : LocationCallback() {
            private var attemptCount = 0
            private var bestLocation: android.location.Location? = null

            override fun onLocationResult(locationResult: LocationResult) {
                attemptCount++
                println("📍 Location update #$attemptCount received:")

                locationResult.locations.forEach { location ->
                    println("  📍 Lat: ${location.latitude}, Lng: ${location.longitude}")
                    println("     Accuracy: ${location.accuracy}m, Provider: ${location.provider}")
                    println("     Time: ${System.currentTimeMillis() - location.time}ms ago")

                    // Validate coordinates
                    val isValidCoordinates = location.latitude != 0.0 && location.longitude != 0.0 &&
                            location.latitude >= -90.0 && location.latitude <= 90.0 &&
                            location.longitude >= -180.0 && location.longitude <= 180.0

                    if (!isValidCoordinates) {
                        println("     ❌ Invalid coordinates")
                        return@forEach
                    }

                    // Update best location if this one is better
                    if (bestLocation == null || location.accuracy < bestLocation!!.accuracy) {
                        bestLocation = location
                        println("     ✅ New best location (accuracy: ${location.accuracy}m)")
                    }

                    // Accept location if it's good enough or we've tried enough times
                    if (location.accuracy <= 50 || attemptCount >= 15) {
                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                        locationState.value = geoPoint
                        isLoadingState.value = false
                        println("✅ Location accepted: ${location.latitude},${location.longitude} (accuracy: ${location.accuracy}m)")

                        fusedLocationClient.removeLocationUpdates(this)
                        locationCallbackState.value = null
                        return
                    }
                }

                // If we've tried enough times, use the best location we have
                if (attemptCount >= 15) {
                    isLoadingState.value = false
                    bestLocation?.let { location ->
                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                        locationState.value = geoPoint
                        println("✅ Using best available location: ${location.latitude},${location.longitude} (accuracy: ${location.accuracy}m)")
                    } ?: run {
                        errorState.value = "Không thể lấy vị trí chính xác"
                        println("❌ No valid location found after 15 attempts")
                    }
                    fusedLocationClient.removeLocationUpdates(this)
                    locationCallbackState.value = null
                }
            }
        }

        locationCallbackState.value = callback

        if (hasLocationPermission(context)) {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, null)
            println("📡 Location updates requested (high accuracy, max 15 attempts)")
        }

    } catch (e: SecurityException) {
        isLoadingState.value = false
        errorState.value = "Lỗi bảo mật: ${e.message}"
        println("❌ SecurityException in requestLocationUpdates: ${e.message}")
    }
}