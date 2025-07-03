package com.example.busmap.Page

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.busmap.model.isStationFavorite
import com.example.busmap.model.toggleFavoriteStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import com.example.busmap.model.Station
import com.example.busmap.model.StationRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun surroundingStation(navController: NavController) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val locationManager = rememberLocationManager(snackbarHostState)

    var mapCenter by remember { mutableStateOf<GeoPoint?>(null) }
    var mapRotation by remember { mutableStateOf(0f) }
    var stationOverlays by remember { mutableStateOf<List<Overlay>>(emptyList()) }
    var nearbyStations by remember { mutableStateOf<List<Station>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var showSearchResults by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showStationList by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Search function for stations and places - Enhanced for better results
    fun performSearch(query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            showSearchResults = false
            return
        }

        // Show results immediately when user starts typing
        showSearchResults = true

        coroutineScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    val locationResults = searchLocations(query, context, mapCenter)
                    val stationResults = searchStations(query)

                    // Combine and prioritize results
                    val combinedResults = (locationResults + stationResults)
                        .distinctBy { searchResult -> searchResult.displayName }
                        .sortedWith(compareBy<SearchResult> { result ->
                            // Prioritize exact matches
                            when {
                                result.displayName.equals(query, ignoreCase = true) -> 0
                                result.displayName.startsWith(query, ignoreCase = true) -> 1
                                result.displayName.contains(query, ignoreCase = true) -> 2
                                else -> 3
                            }
                        }.thenBy { it.displayName })
                        .take(8) // Limit to 8 results for better performance

                    combinedResults
                }

                searchResults = results
                showSearchResults = results.isNotEmpty()
                println("Search completed: ${results.size} results for '$query'")
            } catch (e: Exception) {
                println("Search error: ${e.message}")
                showSearchResults = false
            }
        }
    }

    // Trong surroundingStation() Composable, sửa LaunchedEffect
    LaunchedEffect(locationManager.location) {
        println("🗺️ Location changed in surroundingStation: ${locationManager.location}")

        if (locationManager.location != null) {
            println("✅ Setting map center to user location: ${locationManager.location}")
            mapCenter = locationManager.location

            nearbyStations = getNearbyStations(locationManager.location!!, radiusMeters = 5000.0)
            println("📍 Map center set to user location: ${locationManager.location}, Nearby stations: ${nearbyStations.size}")
        } else if (mapCenter == null) {
            println("⚠️ No user location, using default center")
            mapCenter = GeoPoint(21.0285, 105.8542)
            nearbyStations = getAllStations()
            println("📍 User location unavailable, using default: $mapCenter, All stations: ${nearbyStations.size}")
        }
    }

    if (mapCenter == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text(
                text = "Đang tải bản đồ...",
                modifier = Modifier.padding(top = 60.dp)
            )
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Map background - lowest z-index
            com.example.busmap.Page.OsmMapView(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f),
                center = mapCenter!!,
                userLocation = locationManager.location,
                onMapRotation = mapRotation,
                onMapMoved = { newCenter ->
                    println("🗺️ Map moved to: $newCenter")
                    mapCenter = newCenter
                },
                overlays = stationOverlays,
                showZoomControls = true, // Enable zoom controls
                onMapViewReady = { mapView: MapView? ->
                    println("🗺️ OsmMapView initialized in surroundingStation")
                    if (mapView != null) {
                        coroutineScope.launch {
                            try {
                                val overlays = withContext(Dispatchers.Main) {
                                    createStationOverlays(mapView, context, locationManager.location)
                                }
                                stationOverlays = overlays
                                mapView.invalidate()
                                println("✅ MapView ready, overlays count: ${overlays.size}")
                            } catch (e: Exception) {
                                errorMessage = "Lỗi khi tạo overlays: ${e.message}"
                                println("❌ Error creating overlays: ${e.message}")
                            }
                        }
                    } else {
                        errorMessage = "Không thể khởi tạo bản đồ"
                        println("❌ MapView is null in onMapViewReady")
                    }
                }
            )

            // Enhanced search bar with autocomplete - highest z-index
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .zIndex(100f) // Very high z-index to float above everything
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp) // Higher elevation
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            performSearch(query)
                        },
                        placeholder = { Text("Tìm kiếm trạm, địa điểm...", fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.search),
                                contentDescription = "Search",
                                tint = Color(0xFF2E8B57),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        searchResults = emptyList()
                                        showSearchResults = false
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
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
                            onSearch = {
                                if (searchResults.isNotEmpty()) {
                                    val result = searchResults.first()
                                    mapCenter = result.location
                                    searchQuery = result.displayName
                                    showSearchResults = false
                                }
                            }
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )
                }

                // Search results dropdown - appears immediately below search field with very high z-index
                if (showSearchResults && searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp) // Increased max height
                            .zIndex(101f), // Even higher z-index than search bar
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp) // Maximum elevation
                    ) {
                        Column {
                            // Header with results count
                            Surface(
                                color = Color(0xFF2E8B57).copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Gợi ý địa điểm (${searchResults.size})",
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E8B57),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            LazyColumn(
                                modifier = Modifier.heightIn(max = 250.dp) // Ensure scrollable content
                            ) {
                                items(searchResults.size) { index ->
                                    val result = searchResults[index]
                                    SearchResultItem(
                                        result = result,
                                        onClick = {
                                            mapCenter = result.location
                                            searchQuery = result.displayName
                                            showSearchResults = false

                                            // Update nearby stations based on selected location
                                            coroutineScope.launch {
                                                nearbyStations = getNearbyStations(result.location, radiusMeters = 5000.0)
                                            }
                                        }
                                    )
                                    if (index < searchResults.size - 1) {
                                        HorizontalDivider(
                                            color = Color.Gray.copy(alpha = 0.2f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Control buttons - medium z-index
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .padding(top = 64.dp)
                    .zIndex(5f)
            ) {
                IconButton(
                    onClick = {
                        println("🎯 Center button clicked")
                        if (locationManager.location != null) {
                            println("✅ Centering to user location: ${locationManager.location}")
                            mapCenter = locationManager.location
                        } else {
                            println("⚠️ No location available, requesting...")
                            locationManager.requestLocation()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
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
                            contentDescription = "Center Icon",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                    }
                }
                IconButton(
                    onClick = { mapRotation = 0f },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.compass),
                        contentDescription = "Compass Icon",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                }
            }

            // Station list toggle button - left side - adjusted position to avoid zoom buttons
            IconButton(
                onClick = { showStationList = !showStationList },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .padding(top = 140.dp) // Moved down to avoid zoom buttons
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .zIndex(5f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.find),
                    contentDescription = "Station List",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF2E8B57)
                )
            }

            // Back button - medium z-index
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .zIndex(5f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.back),
                    contentDescription = "Back Icon",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF2E8B57)
                )
            }

            // Bottom section with action buttons and station list
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(4f)
            ) {
                Column {
                    // Station list card - shows when button is pressed
                    if (showStationList) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column {
                                // Header with close button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Trạm xung quanh (${nearbyStations.size})",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E8B57)
                                    )
                                    IconButton(
                                        onClick = { showStationList = false }
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.back),
                                            contentDescription = "Close",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

                                // Station list content
                                if (nearbyStations.isEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.empty1),
                                            contentDescription = "Empty",
                                            modifier = Modifier.size(80.dp),
                                            tint = Color.Unspecified
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Không tìm thấy trạm nào",
                                            fontSize = 16.sp,
                                            color = Color(0xFF757575)
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        items(nearbyStations.size) { index ->
                                            StationItem(
                                                station = nearbyStations[index],
                                                onClick = {
                                                    mapCenter = nearbyStations[index].position // Sử dụng .position
                                                    showStationList = false
                                                }
                                            )
                                            if (index < nearbyStations.size - 1) {
                                                HorizontalDivider(
                                                    color = Color.Gray.copy(alpha = 0.2f),
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White)
                                .clickable { navController.navigate("selectbus") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.logotracuu),
                                    contentDescription = "Tra cứu Icon",
                                    modifier = Modifier.size(96.dp),
                                    tint = Color.Unspecified
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Tra cứu")
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White)
                                .clickable { navController.navigate("findtheway") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.logotimduong),
                                    contentDescription = "Tìm đường Icon",
                                    modifier = Modifier.size(96.dp),
                                    tint = Color.Unspecified
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Tìm đường")
                            }
                        }
                    }
                }
            }

            // Error message - high z-index
            errorMessage?.let { message ->
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .zIndex(50f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = message,
                        color = Color.Red,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { navController.navigate("surroundingstation") },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Thử lại")
                    }
                }
            }

            // Location status indicators with better visibility - high z-index
            if (locationManager.isLoading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 100.dp)
                        .background(Color(0xFF2E8B57).copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                        .zIndex(50f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang lấy vị trí...", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }

            locationManager.error?.let { error ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 100.dp)
                        .background(Color.Red.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                        .zIndex(50f)
                ) {
                    Column {
                        Text(error, color = Color.White, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { locationManager.requestLocation() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("Thử lại", color = Color.Red)
                        }
                    }
                }
            }

            // Current location indicator - high z-index
            locationManager.location?.let { location ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 70.dp)
                        .background(Color.Green.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .zIndex(50f)
                ) {
                    Text(
                        text = "📍 ${String.format("%.6f, %.6f", location.latitude, location.longitude)}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

suspend fun getAllStations(): List<com.example.busmap.model.Station> {
    return withContext(Dispatchers.IO) {
        val repository = StationRepository()
        repository.getAllStations()
    }
}

fun GeoPoint.distanceTo(other: GeoPoint): Double {
    return this.distanceToAsDouble(other)
}

suspend fun getNearbyStations(userLocation: GeoPoint, radiusMeters: Double = 5000.0): List<com.example.busmap.model.Station> {
    return withContext(Dispatchers.IO) {
        val repository = StationRepository()
        val allStations = repository.getAllStations()
        allStations.filter { station ->
            station.position.distanceTo(userLocation) <= radiusMeters
        }
    }
}

suspend fun createStationOverlays(mapView: MapView?, context: Context, userLocation: GeoPoint?): List<Overlay> {
    return withContext(Dispatchers.Main) {
        val overlays = mutableListOf<Overlay>()
        if (mapView == null) return@withContext overlays

        try {
            userLocation?.let { location ->
                val userMarker = Marker(mapView).apply {
                    position = location
                    title = "Vị trí của bạn"
                    val drawable = context.getDrawable(R.drawable.ic_location32)
                    icon = drawable ?: context.getDrawable(android.R.drawable.ic_menu_mylocation)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
                overlays.add(userMarker)

                val nearbyStations = getNearbyStations(location)
                nearbyStations.forEach { station ->
                    val marker = Marker(mapView).apply {
                        position = station.position // Sử dụng station.position
                        title = station.name
                        subDescription = "Tuyến: ${station.routes.joinToString(", ")}"
                        val drawable = context.getDrawable(R.drawable.ic_location32)
                        icon = drawable ?: context.getDrawable(android.R.drawable.ic_menu_mylocation)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    overlays.add(marker)
                }
            } ?: run {
                val allStations = getAllStations()
                allStations.forEach { station ->
                    val marker = Marker(mapView).apply {
                        position = station.position // Sử dụng station.position
                        title = station.name
                        subDescription = "Tuyến: ${station.routes.joinToString(", ")}"
                        val drawable = context.getDrawable(R.drawable.ic_location32)
                        icon = drawable ?: context.getDrawable(android.R.drawable.ic_menu_mylocation)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    overlays.add(marker)
                }
            }
        } catch (e: Exception) {
            println("Failed to create marker: ${e.message}")
        }
        overlays
    }
}

@Composable
fun StationItem(station: com.example.busmap.model.Station, onClick: () -> Unit) {
    val context = LocalContext.current
    var isFavorite by remember { mutableStateOf(isStationFavorite(context, station)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_location32),
                contentDescription = "Station Icon",
                tint = Color(0xFF2E8B57),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tuyến: ${station.routes.joinToString(", ")}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            IconButton(
                onClick = {
                    toggleFavoriteStation(context, station)
                    isFavorite = isStationFavorite(context, station)
                }
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color.Red else Color.Gray
                )
            }
        }
    }
}