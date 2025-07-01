package com.example.busmap.Page

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.busmap.R
import com.example.busmap.model.getTestBusRoutes
import com.example.busmap.rememberLocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import kotlin.math.atan2

suspend fun getRouteFromOSRM(start: GeoPoint, end: GeoPoint): List<GeoPoint> {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val url = "http://router.project-osrm.org/route/v1/driving/" +
                "${start.longitude},${start.latitude};${end.longitude},${end.latitude}" +
                "?overview=full&geometries=geojson"
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val coordinates = json.getJSONArray("routes")
                        .getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates")
                    val points = mutableListOf<GeoPoint>()
                    for (i in 0 until coordinates.length()) {
                        val coord = coordinates.getJSONArray(i)
                        points.add(GeoPoint(coord.getDouble(1), coord.getDouble(0)))
                    }
                    points
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

fun calculateDirectionRotation(start: GeoPoint, end: GeoPoint): Float {
    val deltaY = end.latitude - start.latitude
    val deltaX = end.longitude - start.longitude
    return Math.toDegrees(atan2(deltaY, deltaX).toDouble()).toFloat()
}

fun addDirectionCircles(
    mapView: MapView?,
    points: List<GeoPoint>,
    overlays: MutableList<org.osmdroid.views.overlay.Overlay>,
    isForward: Boolean
) {
    if (mapView == null || points.size < 2) {
        println("Cannot add direction circles, mapView is null or points insufficient")
        return
    }

    try {
        val step = (points.size / 10).coerceAtLeast(1)
        val maxCircles = 5
        val circleIndices = (step until points.size step step).take(maxCircles)

        for (i in circleIndices) {
            try {
                val startPoint = points[i - 1]
                val endPoint = points[i]
                val rotation = if (isForward) calculateDirectionRotation(startPoint, endPoint)
                else calculateDirectionRotation(endPoint, startPoint)
                val circlePoint = points[i]

                val circleIcon = mapView.context.getDrawable(R.drawable.ic_circle_direction)
                if (circleIcon != null) {
                    val marker = Marker(mapView).apply {
                        position = circlePoint
                        icon = circleIcon
                        setRotation(rotation)
                        setAnchor(0.5f, 0.5f)
                    }
                    overlays.add(marker)
                } else {
                    println("Failed to load circle direction icon")
                }
            } catch (e: Exception) {
                println("Failed to create direction circle: ${e.message}")
            }
        }
    } catch (e: Exception) {
        println("Error in addDirectionCircles: ${e.message}")
    }
}

suspend fun createRouteOverlays(mapView: MapView?, busRoute: com.example.busmap.model.BusRoute, locationManager: com.example.busmap.LocationManagerState): List<org.osmdroid.views.overlay.Overlay> {
    return withContext(Dispatchers.Default) {
        val overlays = mutableListOf<org.osmdroid.views.overlay.Overlay>()
        if (mapView == null) {
            println("MapView is null in createRouteOverlays")
            return@withContext overlays
        }

        try {
            // Add user location marker if available
            withContext(Dispatchers.Main) {
                locationManager.location?.let { location ->
                    try {
                        val userIcon = mapView.context.getDrawable(R.drawable.ic_location32)
                            ?: mapView.context.getDrawable(android.R.drawable.ic_menu_mylocation)

                        val userMarker = Marker(mapView).apply {
                            position = location
                            title = "Vị trí của bạn"
                            icon = userIcon
                            id = "user_location_marker"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        overlays.add(userMarker)
                        println("Added user marker to route overlays at: $location")
                    } catch (e: Exception) {
                        println("Failed to create user marker: ${e.message}")
                    }
                }
            }

            if (busRoute.points.isNotEmpty()) {
                // Create route using OSRM to follow actual roads
                val allPoints = mutableListOf<GeoPoint>()
                for (i in 0 until busRoute.points.size - 1) {
                    val routePoints = getRouteFromOSRM(busRoute.points[i], busRoute.points[i + 1])
                    if (routePoints.isNotEmpty()) {
                        allPoints.addAll(routePoints)
                    } else {
                        // Fallback to direct line if OSRM fails
                        allPoints.add(busRoute.points[i])
                    }
                }
                // Ensure the last point is included
                if (allPoints.isNotEmpty() && allPoints.last() != busRoute.points.last()) {
                    allPoints.add(busRoute.points.last())
                }

                val routePoints = if (allPoints.isNotEmpty()) allPoints else busRoute.points

                // Create single polyline for the route
                val routePolyline = Polyline().apply {
                    setPoints(routePoints)
                    color = android.graphics.Color.parseColor("#2E8B57") // Green color
                    width = 12f
                }
                overlays.add(routePolyline)

                // Add direction arrows along the route
                addDirectionArrows(mapView, routePoints, overlays)

                // Add bus stop markers
                withContext(Dispatchers.Main) {
                    busRoute.points.forEachIndexed { index, geoPoint ->
                        try {
                            val stopIcon = mapView.context.getDrawable(R.drawable.ic_location32)
                            if (stopIcon != null) {
                                val marker = Marker(mapView).apply {
                                    position = geoPoint
                                    title = busRoute.stops.getOrNull(index) ?: "Điểm dừng $index"
                                    icon = stopIcon
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                }
                                overlays.add(marker)
                            }
                        } catch (e: Exception) {
                            println("Failed to create marker for point $index: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error in createRouteOverlays: ${e.message}")
            e.printStackTrace()
        }

        overlays
    }
}

fun addDirectionArrows(
    mapView: MapView?,
    points: List<GeoPoint>,
    overlays: MutableList<org.osmdroid.views.overlay.Overlay>
) {
    if (mapView == null || points.size < 2) {
        println("Cannot add direction arrows, mapView is null or points insufficient")
        return
    }

    try {
        // Add arrows at regular intervals along the route
        val totalPoints = points.size
        val arrowInterval = (totalPoints / 8).coerceAtLeast(2) // Show about 8 arrows

        for (i in arrowInterval until totalPoints step arrowInterval) {
            if (i < points.size && i > 0) {
                try {
                    val startPoint = points[i - 1]
                    val endPoint = points[i]
                    val rotation = calculateDirectionRotation(startPoint, endPoint)
                    val arrowPoint = points[i]

                    // Use a custom arrow drawable or create one
                    val arrowIcon = mapView.context.getDrawable(R.drawable.ic_arrow_forward_32)
                        ?: mapView.context.getDrawable(R.drawable.ic_circle_direction)

                    if (arrowIcon != null) {
                        val marker = Marker(mapView).apply {
                            position = arrowPoint
                            icon = arrowIcon
                            setRotation(rotation)
                            setAnchor(0.5f, 0.5f)
                            // Make arrows smaller and less prominent
                            alpha = 0.7f
                        }
                        overlays.add(marker)
                    }
                } catch (e: Exception) {
                    println("Failed to create direction arrow: ${e.message}")
                }
            }
        }
    } catch (e: Exception) {
        println("Error in addDirectionArrows: ${e.message}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusRouteScreen(routeId: String?, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val locationManager = rememberLocationManager(snackbarHostState)

    var mapCenter by remember { mutableStateOf(GeoPoint(21.0285, 105.8542)) }
    var routeOverlays by remember {
        mutableStateOf<List<org.osmdroid.views.overlay.Overlay>>(
            emptyList()
        )
    }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var mapViewState by remember { mutableStateOf<MapView?>(null) }
    var isFavorite by remember { mutableStateOf(false) }

    // Lấy dữ liệu tuyến theo routeId
    val busRoute = remember(routeId) {
        getTestBusRoutes().find { it.id == routeId } ?: getTestBusRoutes().first()
    }

    // Check favorite status
    LaunchedEffect(busRoute.id) {
        isFavorite = isBusRouteFavorite(context, busRoute.id)
    }

    LaunchedEffect(busRoute) {
        isLoading = true
        errorMessage = null
        try {
            routeOverlays = emptyList()
            mapViewState?.let { mapView ->
                routeOverlays = createRouteOverlays(mapView, busRoute, locationManager)
                if (busRoute.points.isNotEmpty()) {
                    mapCenter = busRoute.points.first()
                } else {
                    mapCenter = GeoPoint(21.0285, 105.8542)
                }
            }
        } catch (e: Exception) {
            errorMessage = "Lỗi: ${e.message}"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    // UI Layout
    Box(modifier = Modifier.fillMaxSize()) {
        // App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Tuyến xe buýt ${busRoute.id}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // Favorite button
                IconButton(
                    onClick = {
                        toggleFavoriteBusRoute(context, busRoute)
                        isFavorite = isBusRouteFavorite(context, busRoute.id)

                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                if (isFavorite) "Đã thêm vào yêu thích" else "Đã xóa khỏi yêu thích"
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) Color.Red else Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            ),
            modifier = Modifier.zIndex(1f)
        )

        // Map View
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp)
        ) {
            OsmMapView(
                modifier = Modifier.fillMaxSize(),
                center = mapCenter,
                userLocation = locationManager.location,
                onMapRotation = 0f,
                onMapMoved = { /* No action needed */ },
                overlays = routeOverlays,
                showZoomControls = true, // Enable zoom controls
                onMapViewReady = { mapView ->
                    mapViewState = mapView
                    coroutineScope.launch {
                        routeOverlays = createRouteOverlays(mapView, busRoute, locationManager)
                    }
                }
            )

            // My Location Button - adjusted position to avoid zoom buttons
            FloatingActionButton(
                onClick = {
                    if (locationManager.location != null) {
                        mapCenter = locationManager.location!!
                    } else {
                        locationManager.requestLocation()
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(56.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (locationManager.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.center),
                        contentDescription = "My Location",
                        tint = Color.White
                    )
                }
            }

            // Location error indicator
            locationManager.error?.let { error ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(error, color = Color.White, fontSize = 14.sp)
                }
            }

            // Loading & Error Indicators
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            errorMessage?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.clickable { errorMessage = null },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Red.copy(alpha = 0.8f)
                        )
                    ) {
                        Text(
                            text = error,
                            color = Color.White,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Route Info Card - Updated to show single route info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Tuyến ${busRoute.id}: ${busRoute.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color(0xFF2E8B57), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tuyến: ${busRoute.stops.firstOrNull() ?: ""} ↔ ${busRoute.stops.lastOrNull() ?: ""}")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Thời gian: ${busRoute.operatingHours}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Tần suất: ${busRoute.frequency}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Giá vé: ${busRoute.ticketPrice}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            // Snackbar for location errors
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}