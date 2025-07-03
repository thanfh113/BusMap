package com.example.busmap.Page

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.busmap.R
import com.example.busmap.rememberLocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun findTheWay(navController: NavController) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val locationManager = rememberLocationManager(snackbarHostState)

    var mapCenter by remember { mutableStateOf(GeoPoint(21.0285, 105.8542)) }
    var startPoint by remember { mutableStateOf("") }
    var endPoint by remember { mutableStateOf("") }
    var startLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var endLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var routeOverlays by remember { mutableStateOf<List<org.osmdroid.views.overlay.Overlay>>(emptyList()) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // Search states
    var startSearchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var endSearchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var showStartResults by remember { mutableStateOf(false) }
    var showEndResults by remember { mutableStateOf(false) }
    var isSearchingRoute by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Search functions with bus stations included
    fun searchStartPoint(query: String) {
        println("Searching start point for: '$query'")
        if (query.isBlank()) {
            startSearchResults = emptyList()
            showStartResults = false
            return
        }

        coroutineScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    val locationResults = searchLocations(query, context, mapCenter)
                    val stationResults = searchStations(query)
                    (locationResults + stationResults).distinctBy { it.displayName }
                }
                println("Start search results: ${results.size} items")
                startSearchResults = results
                showStartResults = results.isNotEmpty()
            } catch (e: Exception) {
                println("Start search error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun searchEndPoint(query: String) {
        println("Searching end point for: '$query'")
        if (query.isBlank()) {
            endSearchResults = emptyList()
            showEndResults = false
            return
        }

        coroutineScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    val locationResults = searchLocations(query, context, mapCenter)
                    val stationResults = searchStations(query)
                    (locationResults + stationResults).distinctBy { it.displayName }
                }
                println("End search results: ${results.size} items")
                endSearchResults = results
                showEndResults = results.isNotEmpty()
            } catch (e: Exception) {
                println("End search error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Route finding function
    fun findRoute() {
        if (startLocation == null || endLocation == null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Vui lòng chọn điểm đi và điểm đến")
            }
            return
        }

        isSearchingRoute = true
        coroutineScope.launch {
            try {
                val route = withContext(Dispatchers.IO) {
                    findBusRoute(startLocation!!, endLocation!!)
                }

                routeOverlays = route.overlays
                if (route.route.isNotEmpty()) {
                    mapCenter = route.route.first()
                }

                snackbarHostState.showSnackbar(
                    if (route.route.isNotEmpty()) "Đã tìm thấy ${route.busRoutes.size} tuyến xe"
                    else "Không tìm thấy tuyến xe phù hợp"
                )
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Lỗi tìm đường: ${e.message}")
            } finally {
                isSearchingRoute = false
            }
        }
    }

    LaunchedEffect(locationManager.location) {
        if (locationManager.location != null) {
            mapCenter = locationManager.location!!
            if (startPoint.isEmpty()) {
                startPoint = "Vị trí hiện tại"
                startLocation = locationManager.location
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding: PaddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Card 1: Thông tin tìm đường
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.back),
                                    contentDescription = "Back",
                                    tint = Color(0xFF2E8B57)
                                )
                            }
                            Text(
                                text = "Tìm đường",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Start point with search autocomplete
                        Column {
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                TextField(
                                    value = startPoint,
                                    onValueChange = { query ->
                                        startPoint = query
                                        if (query != "Vị trí hiện tại") {
                                            searchStartPoint(query)
                                        } else {
                                            showStartResults = false
                                        }
                                    },
                                    label = { Text("Điểm đi", fontSize = 14.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_location32),
                                            contentDescription = "Start",
                                            tint = Color(0xFF2E8B57),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        Row {
                                            if (startPoint.isNotEmpty() && startPoint != "Vị trí hiện tại") {
                                                IconButton(onClick = {
                                                    startPoint = ""
                                                    startLocation = null
                                                    startSearchResults = emptyList()
                                                    showStartResults = false
                                                }) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.back),
                                                        contentDescription = "Clear",
                                                        tint = Color.Gray,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            IconButton(onClick = {
                                                if (locationManager.location != null) {
                                                    startPoint = "Vị trí hiện tại"
                                                    startLocation = locationManager.location
                                                    mapCenter = locationManager.location!!
                                                    showStartResults = false
                                                } else {
                                                    locationManager.requestLocation()
                                                }
                                            }) {
                                                if (locationManager.isLoading) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        strokeWidth = 2.dp,
                                                        color = Color(0xFF2E8B57)
                                                    )
                                                } else {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.center),
                                                        contentDescription = "Use current location",
                                                        tint = Color(0xFF2E8B57),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // End point with search autocomplete
                        Column {
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                TextField(
                                    value = endPoint,
                                    onValueChange = { query ->
                                        endPoint = query
                                        searchEndPoint(query)
                                    },
                                    label = { Text("Điểm đến", fontSize = 14.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_location32),
                                            contentDescription = "End",
                                            tint = Color.Red,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (endPoint.isNotEmpty()) {
                                            IconButton(onClick = {
                                                endPoint = ""
                                                endLocation = null
                                                endSearchResults = emptyList()
                                                showEndResults = false
                                            }) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.back),
                                                    contentDescription = "Clear",
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = { findRoute() }
                                    ),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                                )
                            }
                        }

                        // Location error indicator
                        locationManager.error?.let { error ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = error,
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Search button
                        Button(
                            onClick = { findRoute() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E8B57)
                            ),
                            enabled = !isSearchingRoute && startLocation != null && endLocation != null
                        ) {
                            if (isSearchingRoute) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đang tìm...", color = Color.White)
                            } else {
                                Text("Tìm đường", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // Card 2: Map
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        OsmMapView(
                            modifier = Modifier.fillMaxSize(),
                            center = mapCenter,
                            userLocation = locationManager.location,
                            onMapRotation = 0f,
                            onMapMoved = { newCenter ->
                                mapCenter = newCenter
                            },
                            overlays = routeOverlays,
                            showZoomControls = true, // Enable zoom controls
                            onMapViewReady = { view ->
                                mapView = view
                                println("MapView ready: ${view != null}")
                            }
                        )

                        // Loading indicator
                        if (locationManager.isLoading || isSearchingRoute) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isSearchingRoute) "Đang tìm đường..." else "Đang lấy vị trí...",
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Start point search results - Floating overlay
            if (showStartResults && startSearchResults.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1000f)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .padding(horizontal = 16.dp)
                            .offset(y = 120.dp), // Vị trí cố định từ đầu màn hình
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "Gợi ý điểm đi (${startSearchResults.size})",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Medium
                            )

                            LazyColumn(
                                modifier = Modifier.heightIn(max = 350.dp)
                            ) {
                                items(startSearchResults.size) { index ->
                                    val result = startSearchResults[index]
                                    SearchResultItem(
                                        result = result,
                                        onClick = {
                                            startPoint = result.displayName
                                            startLocation = result.location
                                            mapCenter = result.location
                                            showStartResults = false
                                        }
                                    )
                                    if (index < startSearchResults.size - 1) {
                                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // End point search results - Floating overlay
            if (showEndResults && endSearchResults.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1000f)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .padding(horizontal = 16.dp)
                            .offset(y = 190.dp), // Vị trí cố định từ đầu màn hình
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "Gợi ý điểm đến (${endSearchResults.size})",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Medium
                            )

                            LazyColumn(
                                modifier = Modifier.heightIn(max = 350.dp)
                            ) {
                                items(endSearchResults.size) { index ->
                                    val result = endSearchResults[index]
                                    SearchResultItem(
                                        result = result,
                                        onClick = {
                                            endPoint = result.displayName
                                            endLocation = result.location
                                            mapCenter = result.location
                                            showEndResults = false
                                        }
                                    )
                                    if (index < endSearchResults.size - 1) {
                                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}