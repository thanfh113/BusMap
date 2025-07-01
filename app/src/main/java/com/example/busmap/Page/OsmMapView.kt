package com.example.busmap.Page

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.busmap.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    center: GeoPoint,
    userLocation: GeoPoint?,
    onMapRotation: Float,
    onMapMoved: (GeoPoint) -> Unit,
    overlays: List<Overlay>,
    onMapViewReady: (MapView?) -> Unit = {},
    onMapTapped: ((GeoPoint) -> Unit)? = null,
    showZoomControls: Boolean = true  // Add parameter to control zoom buttons visibility
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentCenter by rememberUpdatedState(center)
    val currentOverlays by rememberUpdatedState(overlays)
    val currentRotation by rememberUpdatedState(onMapRotation)
    val currentUserLocation by rememberUpdatedState(userLocation)

    // Store MapView in a state variable
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // Store MyLocationOverlay for live tracking
    var myLocationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // Track if we've done initial zoom setup
    var hasInitialZoom by remember { mutableStateOf(false) }

    // Store touch coordinates for tap detection
    var touchDownX by remember { mutableStateOf(0f) }
    var touchDownY by remember { mutableStateOf(0f) }

    // Initialize configuration
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                Configuration.getInstance().apply {
                    userAgentValue = context.packageName
                    osmdroidBasePath = context.cacheDir
                    osmdroidTileCache = File(context.cacheDir, "osmdroid")
                    if (!osmdroidTileCache.exists()) {
                        osmdroidTileCache.mkdirs()
                    }

                    // Set larger cache size for better map performance
                    tileFileSystemCacheMaxBytes = 100L * 1024 * 1024 // 100MB cache
                    tileFileSystemThreads = 8
                    tileDownloadThreads = 4
                }
                println("OSMDroid configured successfully with cache: ${Configuration.getInstance().osmdroidTileCache.absolutePath}")
            }
        } catch (e: Exception) {
            println("Failed to configure OSMDroid: ${e.message}")
            e.printStackTrace()
        }
    }

    // Create MapView with better initial settings
    val mapViewInstance = remember(context) {
        try {
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)

                // Set reasonable default zoom
                controller.setZoom(16.0)
                minZoomLevel = 8.0
                maxZoomLevel = 19.0

                isTilesScaledToDpi = true
                isHorizontalMapRepetitionEnabled = false
                isVerticalMapRepetitionEnabled = false
                setBuiltInZoomControls(false)

                println("MapView created successfully with zoom: ${controller}")
            }
        } catch (e: Exception) {
            println("Error creating MapView: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    // Initialize location tracking overlay
    LaunchedEffect(mapViewInstance) {
        mapViewInstance?.let { mv ->
            try {
                val locationProvider = GpsMyLocationProvider(context)
                val locationOverlay = MyLocationNewOverlay(locationProvider, mv).apply {
                    enableMyLocation()
                    disableFollowLocation()
                    isDrawAccuracyEnabled = true
                }
                myLocationOverlay = locationOverlay
                println("MyLocationOverlay created successfully")
            } catch (e: Exception) {
                println("Error creating location overlay: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Effect to update mapView state when mapViewInstance changes
    LaunchedEffect(mapViewInstance) {
        mapView = mapViewInstance
        println("Map view instance updated: ${mapViewInstance != null}")
        onMapViewReady(mapViewInstance)
    }

    // Update map center with immediate effect
    LaunchedEffect(currentCenter) {
        mapViewInstance?.let { mv ->
            try {
                println("🗺️ Updating map center to: ${currentCenter.latitude}, ${currentCenter.longitude}")

                // Force immediate center update without animation for responsiveness
                mv.controller.setCenter(currentCenter)

                // Set zoom if needed
                if (!hasInitialZoom || mv.zoomLevelDouble < 14.0) {
                    mv.controller.setZoom(16.0)
                    hasInitialZoom = true
                    println("🔍 Set zoom to 16.0")
                }

                // Force redraw
                mv.invalidate()

                println("✅ Map center updated to: ${currentCenter.latitude}, ${currentCenter.longitude}")
            } catch (e: Exception) {
                println("❌ Error updating map center: ${e.message}")
            }
        }
    }

    // Update map center when user location changes (only for initial setup)
    var hasInitialUserLocation by remember { mutableStateOf(false) }
    LaunchedEffect(currentUserLocation) {
        currentUserLocation?.let { location ->
            println("📍 User location received in OsmMapView: $location")

            // Accept any location with basic validation
            val isValidLocation = location.latitude != 0.0 && location.longitude != 0.0 &&
                    location.latitude >= -90.0 && location.latitude <= 90.0 &&
                    location.longitude >= -180.0 && location.longitude <= 180.0

            if (!hasInitialUserLocation && isValidLocation) {
                mapViewInstance?.let { mv ->
                    println("🎯 Centering map to initial user location: $location")
                    mv.controller.setCenter(location)
                    if (mv.zoomLevelDouble < 16.0) {
                        mv.controller.setZoom(16.0)
                    }
                    mv.invalidate()
                }
                hasInitialUserLocation = true
                println("✅ Map centered to initial user location: $location")
            } else if (!isValidLocation) {
                println("❌ User location rejected - invalid coordinates: $location")
            }
        }
    }

    // Handle MapView lifecycle
    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapViewInstance?.onResume()
                    myLocationOverlay?.enableMyLocation()
                    println("MapView resumed")
                }
                Lifecycle.Event.ON_PAUSE -> {
                    myLocationOverlay?.disableMyLocation()
                    mapViewInstance?.onPause()
                    println("MapView paused")
                }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            myLocationOverlay?.disableMyLocation()
            try {
                mapViewInstance?.onDetach()
                println("MapView detached")
            } catch (e: Exception) {
                println("Error detaching MapView: ${e.message}")
            }
        }
    }

    // If MapView failed to initialize, show an empty box
    if (mapViewInstance == null) {
        Box(modifier = modifier.fillMaxSize()) {
            // Could add an error message here if needed
        }
        return
    }

    // Handle MapView updates for overlays
    LaunchedEffect(currentOverlays) {
        val mv = mapViewInstance ?: return@LaunchedEffect

        try {
            // Update overlays - ensure we keep MyLocationOverlay if it exists
            mv.overlays.clear()

            // First add MyLocationOverlay if it exists
            myLocationOverlay?.let {
                mv.overlays.add(it)
            }

            // Then add any custom overlays
            mv.overlays.addAll(currentOverlays)

            // Notify the view to redraw
            mv.invalidate()

            println("Updated ${currentOverlays.size} overlays on map (+ location overlay)")
        } catch (e: Exception) {
            println("Error updating map overlays: ${e.message}")
            e.printStackTrace()
        }
    }

    // Set up map listener with tap handling
    LaunchedEffect(mapViewInstance, onMapTapped) {
        val mv = mapViewInstance ?: return@LaunchedEffect

        try {
            val mapListener = object : MapListener {
                override fun onScroll(event: ScrollEvent): Boolean {
                    val center = mv.mapCenter
                    if (center is GeoPoint) {
                        println("🗺️ Map scrolled to: ${center.latitude}, ${center.longitude}")
                        onMapMoved(center)
                    }
                    return true
                }

                override fun onZoom(event: ZoomEvent): Boolean {
                    println("🔍 Map zoom changed to: ${event.zoomLevel}")
                    return false
                }
            }
            mv.addMapListener(mapListener)

            // Add tap gesture handling if tap callback is provided
            onMapTapped?.let { tapCallback ->
                mv.setOnTouchListener { view, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            // Record the initial touch position using local variables
                            touchDownX = event.x
                            touchDownY = event.y
                            false
                        }
                        android.view.MotionEvent.ACTION_UP -> {
                            // Check if this was a tap (not a drag)
                            val upX = event.x
                            val upY = event.y

                            val deltaX = Math.abs(upX - touchDownX)
                            val deltaY = Math.abs(upY - touchDownY)
                            val maxDelta = 10f // pixels tolerance for tap vs drag

                            if (deltaX < maxDelta && deltaY < maxDelta) {
                                // This is a tap, not a drag
                                val projection = mv.projection
                                val mapPoint = projection.fromPixels(upX.toInt(), upY.toInt())
                                if (mapPoint is GeoPoint) {
                                    tapCallback(mapPoint)
                                    println("🎯 Map tapped at: ${mapPoint.latitude}, ${mapPoint.longitude}")
                                    return@setOnTouchListener true // Consume the event
                                }
                            }
                            false
                        }
                        else -> false
                    }
                }
            }

            println("Map listener added with tap handling: ${onMapTapped != null}")
        } catch (e: Exception) {
            println("Error adding map listener: ${e.message}")
        }
    }

    Box(modifier = modifier) {
        // Render MapView
        AndroidView(
            factory = { mapViewInstance },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                try {
                    // Set rotation if needed
                    if (view.rotation != currentRotation) {
                        view.rotation = currentRotation
                    }

                    // Invalidate to ensure updates are rendered
                    view.invalidate()
                } catch (e: Exception) {
                    println("Error in AndroidView update: ${e.message}")
                }
            }
        )

        // Zoom controls
        if (showZoomControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .zIndex(10f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Zoom In Button
                FloatingActionButton(
                    onClick = {
                        mapViewInstance?.let { mv ->
                            val currentZoom = mv.zoomLevelDouble
                            val newZoom = (currentZoom + 1.0).coerceAtMost(mv.maxZoomLevel)
                            mv.controller.setZoom(newZoom)
                            println("🔍 Zoom in to: $newZoom")
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = Color.White,
                    contentColor = Color(0xFF2E8B57)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add_32),
                        contentDescription = "Zoom In",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Zoom Out Button
                FloatingActionButton(
                    onClick = {
                        mapViewInstance?.let { mv ->
                            val currentZoom = mv.zoomLevelDouble
                            val newZoom = (currentZoom - 1.0).coerceAtLeast(mv.minZoomLevel)
                            mv.controller.setZoom(newZoom)
                            println("🔍 Zoom out to: $newZoom")
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = Color.White,
                    contentColor = Color(0xFF2E8B57)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.remove_32),
                        contentDescription = "Zoom Out",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
