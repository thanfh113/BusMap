package com.example.busmap.Page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.busmap.R
import com.example.busmap.model.BusRoute
import com.example.busmap.model.BusRouteRepository
import com.example.busmap.rememberLocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import android.util.Log
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.atan2

fun calculateDirectionRotation(start: GeoPoint, end: GeoPoint): Float {
    val deltaY = end.latitude - start.latitude
    val deltaX = end.longitude - start.longitude
    return Math.toDegrees(atan2(deltaY, deltaX).toDouble()).toFloat()
}

fun addDirectionCircles(
    mapView: MapView,
    points: List<GeoPoint>,
    overlays: MutableList<org.osmdroid.views.overlay.Overlay>,
    isForward: Boolean
) {
    if (points.size < 2) return
    try {
        val step = (points.size / 10).coerceAtLeast(1)
        val maxCircles = 5
        val circleIndices = (step until points.size step step).take(maxCircles)
        for (i in circleIndices) {
            val startPoint = points[i - 1]
            val endPoint = points[i]
            val rotation = if (isForward) calculateDirectionRotation(startPoint, endPoint)
            else calculateDirectionRotation(endPoint, startPoint)
            val circlePoint = points[i]
            val marker = Marker(mapView).apply {
                position = circlePoint
                icon = mapView.context.getDrawable(R.drawable.ic_circle_direction)
                    ?: mapView.context.getDrawable(android.R.drawable.ic_menu_directions)
                setRotation(rotation)
                setAnchor(0.5f, 0.5f)
            }
            overlays.add(marker)
        }
    } catch (_: Exception) {}
}

suspend fun getDetailedRoutePoints(points: List<GeoPoint>, mapView: MapView): List<GeoPoint> {
    return withContext(Dispatchers.IO) {
        val allDetailedPoints = mutableListOf<GeoPoint>()
        try {
            val roadManager = OSRMRoadManager(mapView.context, "BusMapApp/1.0")
            roadManager.setService("http://router.project-osrm.org/route/v1/driving/")
            roadManager.addRequestOption("steps=true")
            roadManager.addRequestOption("overview=full")
            roadManager.addRequestOption("geometries=polyline")
            val segmentSize = 5
            val segments = points.chunked(segmentSize)
            for (segmentPoints in segments) {
                Log.d("OSRM", "Requesting segment: $segmentPoints")
                val segmentDetailedPoints = withTimeoutOrNull(20000) {
                    val waypoints = ArrayList(segmentPoints)
                    val road = roadManager.getRoad(waypoints)
                    Log.d("OSRM", "Status: ${road.mStatus}, Points: ${road.mRouteHigh?.size}")
                    if (road.mStatus == Road.STATUS_OK && !road.mRouteHigh.isNullOrEmpty()) {
                        road.mRouteHigh.toList()
                    } else segmentPoints
                }
                if (segmentDetailedPoints != null) {
                    if (allDetailedPoints.isEmpty()) {
                        allDetailedPoints.addAll(segmentDetailedPoints)
                    } else {
                        allDetailedPoints.addAll(segmentDetailedPoints.drop(1))
                    }
                } else {
                    if (allDetailedPoints.isEmpty()) {
                        allDetailedPoints.addAll(segmentPoints)
                    } else {
                        allDetailedPoints.addAll(segmentPoints.drop(1))
                    }
                }
            }
            Log.d("OSRM", "Total detailed points: ${allDetailedPoints.size}")
            allDetailedPoints
        } catch (e: Exception) {
            Log.e("OSRM", "Exception: ${e.message}")
            points
        }
    }
}

suspend fun createRouteOverlays(
    mapView: MapView?,
    busRoute: BusRoute,
    locationManager: com.example.busmap.LocationManagerState
): List<org.osmdroid.views.overlay.Overlay> {
    return withContext(Dispatchers.Default) {
        val overlays = mutableListOf<org.osmdroid.views.overlay.Overlay>()
        if (mapView == null || busRoute.points.isEmpty()) return@withContext overlays
        try {
            locationManager.location?.let { location ->
                withContext(Dispatchers.Main) {
                    val userIcon = mapView.context.getDrawable(android.R.drawable.ic_menu_mylocation)
                    val userMarker = Marker(mapView).apply {
                        position = location
                        title = "Vị trí của bạn"
                        icon = userIcon
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    overlays.add(userMarker)
                }
            }
            val detailedForwardPoints = getDetailedRoutePoints(busRoute.points, mapView)
            val forwardPolyline = Polyline().apply {
                setPoints(detailedForwardPoints)
                color = android.graphics.Color.parseColor("#2E8B57")
                width = 15f
            }
            overlays.add(forwardPolyline)
            addDirectionCircles(mapView, detailedForwardPoints, overlays, isForward = true)
            withContext(Dispatchers.Main) {
                busRoute.points.forEachIndexed { index, geoPoint ->
                    val stopName = busRoute.stops.getOrNull(index) ?: "Điểm dừng ${index + 1}"
                    val stopIcon = mapView.context.getDrawable(R.drawable.ic_location32)
                    if (stopIcon != null) {
                        val marker = Marker(mapView).apply {
                            position = geoPoint
                            title = stopName
                            icon = stopIcon
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        overlays.add(marker)
                    }
                }
            }
        } catch (_: Exception) {
            try {
                val basicPolyline = Polyline().apply {
                    setPoints(busRoute.points)
                    color = android.graphics.Color.parseColor("#2E8B57")
                    width = 15f
                }
                overlays.add(basicPolyline)
            } catch (_: Exception) {}
        }
        overlays
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusRouteScreen(routeId: String?, navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val locationManager = rememberLocationManager(snackbarHostState)
    val busRouteRepository = remember { BusRouteRepository() }

    var mapCenter by remember { mutableStateOf(GeoPoint(21.0285, 105.8542)) }
    var routeOverlays by remember { mutableStateOf<List<org.osmdroid.views.overlay.Overlay>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var mapViewState by remember { mutableStateOf<MapView?>(null) }
    var busRoute by remember { mutableStateOf<BusRoute?>(null) }
    var isTracking by remember { mutableStateOf(false) }
    var currentPointIndex by remember { mutableStateOf(0) }
    var detailedPoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var showRouteInfo by remember { mutableStateOf(false) }

    // Load bus route from Firebase
    LaunchedEffect(routeId) {
        if (routeId != null) {
            isLoading = true
            try {
                val route = withTimeoutOrNull(15000) {
                    busRouteRepository.getBusRouteById(routeId)
                }
                if (route != null && route.points.isNotEmpty()) {
                    busRoute = route
                    mapCenter = route.points.first()
                    mapViewState?.let { mapView ->
                        detailedPoints = getDetailedRoutePoints(route.points, mapView)
                    }
                } else {
                    errorMessage = "Không tìm thấy tuyến xe hoặc không có điểm"
                }
            } catch (e: Exception) {
                errorMessage = "Lỗi khi tải tuyến xe: ${e.message}"
            } finally {
                isLoading = false
            }
        } else {
            errorMessage = "Không có ID tuyến xe"
            isLoading = false
        }
    }

    // Create overlays when route and map are ready
    LaunchedEffect(busRoute, mapViewState) {
        busRoute?.let { route ->
            mapViewState?.let { mapView ->
                try {
                    val newOverlays = createRouteOverlays(mapView, route, locationManager)
                    routeOverlays = newOverlays
                } catch (e: Exception) {
                    errorMessage = "Lỗi khi tạo bản đồ: ${e.message}"
                }
            }
        }
    }

    // Simulate tracking along the route
    LaunchedEffect(isTracking, busRoute, detailedPoints) {
        if (isTracking && busRoute != null && detailedPoints.isNotEmpty()) {
            while (isTracking && currentPointIndex < detailedPoints.size) {
                mapCenter = detailedPoints[currentPointIndex]
                mapViewState?.controller?.setZoom(16.0)
                currentPointIndex = (currentPointIndex + 1) % detailedPoints.size
                delay(1000)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            center = mapCenter,
            userLocation = locationManager.location,
            onMapRotation = 0f,
            onMapMoved = { newCenter ->
                if (!isTracking) mapCenter = newCenter
            },
            overlays = routeOverlays,
            showZoomControls = true,
            onMapViewReady = { mapView ->
                mapViewState = mapView
                if (mapView != null) {
                    busRoute?.let { route ->
                        coroutineScope.launch {
                            detailedPoints = getDetailedRoutePoints(route.points, mapView)
                        }
                    }
                } else {
                    errorMessage = "Không thể khởi tạo bản đồ"
                }
            }
        )

        TopAppBar(
            title = {
                val route = busRoute
                Text(
                    text = if (route != null) "Tuyến ${route.id}: ${route.name}" else "Đang tải...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF2E8B57), // Đổi màu nền sang xanh lá cây
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            ),
            modifier = Modifier
                .zIndex(1f)
                .padding(top = 16.dp) // Đảm bảo cùng top với nút bên phải
        )

        // Đưa 2 nút lên sát góc phải trên cùng, padding 16.dp cho cả top và end
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .zIndex(5f)
        ) {
            FloatingActionButton(
                onClick = {
                    locationManager.location?.let { location ->
                        mapCenter = location
                        isTracking = false
                        currentPointIndex = 0
                        mapViewState?.controller?.setZoom(16.0)
                    } ?: run {
                        locationManager.requestLocation()
                    }
                },
                modifier = Modifier.size(40.dp),
                containerColor = Color.White,
                contentColor = Color(0xFF2E8B57),
                elevation = FloatingActionButtonDefaults.elevation(2.dp)
            ) {
                if (locationManager.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF2E8B57),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.center),
                        contentDescription = "Vị trí của tôi",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            FloatingActionButton(
                onClick = {
                    isTracking = !isTracking
                    if (isTracking) currentPointIndex = 0
                },
                modifier = Modifier.size(40.dp),
                containerColor = Color.White,
                contentColor = if (isTracking) Color.Red else Color(0xFF2E8B57),
                elevation = FloatingActionButtonDefaults.elevation(2.dp)
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Default.LocationOn else Icons.Default.PlayArrow,
                    contentDescription = if (isTracking) "Dừng theo dõi" else "Theo dõi tuyến đường",
                    tint = if (isTracking) Color.Red else Color(0xFF2E8B57),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Nút show thông tin tuyến ở góc dưới phải
        if (busRoute != null) {
            FloatingActionButton(
                onClick = { showRouteInfo = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 20.dp)
                    .size(48.dp),
                containerColor = Color(0xFF2E8B57),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.info),
                    contentDescription = "Thông tin tuyến",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Card thông tin chi tiết tuyến xe
        if (showRouteInfo) {
            val route = busRoute
            if (route != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .zIndex(10f)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Thông tin chi tiết tuyến",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF2E8B57)
                                )
                                IconButton(onClick = { showRouteInfo = false }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.back),
                                        contentDescription = "Đóng",
                                        tint = Color.Gray
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tuyến: ${route.id} - ${route.name}", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Thời gian hoạt động: ${route.operatingHours}")
                            Text("Tần suất: ${route.frequency}")
                            Text("Giá vé: ${route.ticketPrice}")
                            Text("Đơn vị vận hành: ${route.operator}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Các điểm dừng:",
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2E8B57)
                            )
                            route.stops.forEachIndexed { idx, stop ->
                                Text("${idx + 1}. $stop", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF2E8B57))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Đang tạo lộ trình...")
                }
            }
        }

        errorMessage?.let { message ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = message,
                        color = Color.Red,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val route = withTimeoutOrNull(15000) {
                                        busRouteRepository.getBusRouteById(routeId ?: "")
                                    }
                                    if (route != null && route.points.isNotEmpty()) {
                                        busRoute = route
                                        mapCenter = route.points.first()
                                        mapViewState?.let { mapView ->
                                            detailedPoints = getDetailedRoutePoints(route.points, mapView)
                                        }
                                    } else {
                                        errorMessage = "Không tìm thấy tuyến xe hoặc không có điểm"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Lỗi khi tải tuyến xe: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E8B57)
                        )
                    ) {
                        Text("Thử lại")
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(10f)
        )
    }
}