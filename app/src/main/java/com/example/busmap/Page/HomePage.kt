package com.example.busmap.Page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.busmap.R
import com.example.busmap.rememberLocationManager
import com.example.busmap.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(navController: NavController) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val locationManager = rememberLocationManager(snackbarHostState)

    // Repositories
    val busRouteRepository = remember { BusRouteRepository() }
    val stationRepository = remember { StationRepository() }

    // State cho data
    var busRoutes by remember { mutableStateOf<List<BusRoute>>(emptyList()) }
    var stations by remember { mutableStateOf<List<Station>>(emptyList()) }
    var isLoadingData by remember { mutableStateOf(true) }
    var mapCenter by remember { mutableStateOf<GeoPoint?>(null) }
    var overlays by remember { mutableStateOf<List<Overlay>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var showSearchResults by remember { mutableStateOf(false) }
    val isRefreshing = remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()

    // Load data from Firebase
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                isLoadingData = true
                Log.d("BusMap", "Loading bus routes and stations...")
                val busRoutesDeferred = async {
                    withTimeoutOrNull(10000) { // Timeout 10s
                        busRouteRepository.getAllBusRoutes()
                    }
                }
                val stationsDeferred = async {
                    withTimeoutOrNull(10000) { // Timeout 10s
                        stationRepository.getAllStations()
                    }
                }

                busRoutes = busRoutesDeferred.await() ?: emptyList()
                stations = stationsDeferred.await() ?: emptyList()

                Log.d("BusMap", "✅ Loaded ${busRoutes.size} bus routes and ${stations.size} stations")
                if (stations.isEmpty()) {
                    errorMessage = "Không tìm thấy trạm xe buýt"
                }

            } catch (e: Exception) {
                Log.e("BusMap", "❌ Error loading data: ${e.message}")
                errorMessage = "Lỗi khi tải dữ liệu: ${e.message}"
            } finally {
                isLoadingData = false
            }
        }
    }

    // Search function
    fun performSearch(query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            showSearchResults = false
            return
        }

        coroutineScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    searchLocations(query, context, mapCenter)
                }
                searchResults = results
                showSearchResults = results.isNotEmpty()
                Log.d("BusMap", "Search results: ${results.size} items")
            } catch (e: Exception) {
                Log.e("BusMap", "Search error: ${e.message}")
                showSearchResults = false
            }
        }
    }

    // Update map center based on location
    LaunchedEffect(locationManager.location) {
        if (locationManager.location != null) {
            mapCenter = locationManager.location
            Log.d("BusMap", "Map center set to user location: ${locationManager.location}")
        } else {
            mapCenter = GeoPoint(21.0285, 105.8542)
            Log.d("BusMap", "User location unavailable, using default: $mapCenter")
        }
    }

    // Refresh function
    val onRefresh = {
        isRefreshing.value = true
        coroutineScope.launch {
            try {
                locationManager.requestLocation()
                delay(1000)

                val busRoutesDeferred = async {
                    withTimeoutOrNull(10000) {
                        busRouteRepository.getAllBusRoutes()
                    }
                }
                val stationsDeferred = async {
                    withTimeoutOrNull(10000) {
                        stationRepository.getAllStations()
                    }
                }

                busRoutes = busRoutesDeferred.await() ?: emptyList()
                stations = stationsDeferred.await() ?: emptyList()

                if (locationManager.location != null) {
                    mapCenter = locationManager.location
                } else {
                    mapCenter = GeoPoint(21.0285, 105.8542)
                }

                Log.d("BusMap", "✅ Data refreshed: ${busRoutes.size} routes, ${stations.size} stations")
                if (stations.isEmpty()) {
                    errorMessage = "Không tìm thấy trạm xe buýt sau khi làm mới"
                }

            } catch (e: Exception) {
                Log.e("BusMap", "❌ Refresh error: ${e.message}")
                errorMessage = "Lỗi khi làm mới: ${e.message}"
            } finally {
                isRefreshing.value = false
            }
        }
    }

    // Loading state
    if (mapCenter == null || isLoadingData) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isLoadingData) "Đang tải dữ liệu..." else "Đang lấy vị trí...",
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    val listState = rememberLazyListState()
    val scrollOffset = listState.firstVisibleItemScrollOffset
    var isMaxAlpha by remember { mutableStateOf(false) }
    val alpha = remember(scrollOffset) {
        if (scrollOffset > 0 && !isMaxAlpha) {
            val calculatedAlpha = (scrollOffset / 80f).coerceIn(0f, 1f)
            if (calculatedAlpha == 1f) isMaxAlpha = true
            calculatedAlpha
        } else if (scrollOffset <= 0) {
            isMaxAlpha = false
            (scrollOffset / 80f).coerceIn(0f, 1f)
        } else {
            1f
        }
    }
    val toolbarColor = Color(0xFF2E8B57).copy(alpha = alpha)
    val isEnable = alpha < 1f

    Box(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sua),
                        contentDescription = "Logo",
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "BusMap",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.White, shape = RoundedCornerShape(12.dp))
                                .clip(CircleShape)
                        ) {
                            Text(
                                " CT060138",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E8B57),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            },
            actions = {
                Image(
                    painter = painterResource(id = R.drawable.thanh),
                    contentDescription = "Logo Right",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.size(160.dp).padding(end = 16.dp)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = toolbarColor),
            modifier = Modifier.fillMaxWidth().zIndex(1f)
        )

        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = isRefreshing.value || locationManager.isLoading,
            onRefresh = { onRefresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isRefreshing.value) {
                    item {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(48.dp)
                                .padding(bottom = 16.dp)
                        )
                    }
                }

                item {
                    CardBox(
                        navController = navController,
                        isEnable = isEnable,
                        mapCenter = mapCenter!!,
                        userLocation = locationManager.location,
                        busRoutes = busRoutes,
                        stations = stations,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { query: String ->
                            searchQuery = query
                            performSearch(query)
                        },
                        searchResults = searchResults,
                        showSearchResults = showSearchResults,
                        onSearchResultSelected = { result: SearchResult ->
                            mapCenter = result.location
                            searchQuery = result.displayName
                            showSearchResults = false
                        },
                        onClearSearch = {
                            searchQuery = ""
                            searchResults = emptyList()
                            showSearchResults = false
                        },
                        onCenterClick = {
                            coroutineScope.launch {
                                if (locationManager.location != null) {
                                    mapCenter = locationManager.location
                                    Log.d("BusMap", "Centered map to user location: ${locationManager.location}")
                                } else {
                                    locationManager.requestLocation()
                                }
                            }
                        },
                        onMapMoved = { newCenter: GeoPoint -> mapCenter = newCenter },
                        overlays = overlays,
                        onOverlaysChanged = { newOverlays: List<Overlay> -> overlays = newOverlays },
                        onError = { error: String? ->
                            errorMessage = error
                            Log.e("BusMap", "Error in CardBox: $error")
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    IconButtons(navController)
                }

                items(40) { index ->
                    Text(
                        "Nội dung thêm $index",
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        // Location status indicator
        if (locationManager.isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đang lấy vị trí...", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        errorMessage?.let { message ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = message,
                        color = Color.Red,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            errorMessage = null
                            isLoadingData = true
                            onRefresh()
                        },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Thử lại")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardBox(
    navController: NavController,
    isEnable: Boolean,
    mapCenter: GeoPoint,
    userLocation: GeoPoint?,
    busRoutes: List<BusRoute>,
    stations: List<Station>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<SearchResult>,
    showSearchResults: Boolean,
    onSearchResultSelected: (SearchResult) -> Unit,
    onClearSearch: () -> Unit,
    onCenterClick: () -> Unit,
    onMapMoved: (GeoPoint) -> Unit,
    overlays: List<Overlay>,
    onOverlaysChanged: (List<Overlay>) -> Unit,
    onError: (String?) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .zIndex(0f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.hoguom),
                contentDescription = "Background Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .padding(top = 160.dp)
                .zIndex(1f)
        ) {
            var mapViewReference by remember { mutableStateOf<MapView?>(null) }

            com.example.busmap.Page.OsmMapView(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f),
                center = mapCenter,
                userLocation = userLocation,
                onMapRotation = 0f,
                onMapMoved = onMapMoved,
                overlays = overlays,
                showZoomControls = true,
                onMapViewReady = { mapView: MapView? ->
                    mapViewReference = mapView
                    if (mapView != null) {
                        Log.d("BusMap", "MapView initialized successfully")
                        coroutineScope.launch {
                            try {
                                val allStops = withContext(Dispatchers.IO) {
                                    stations.map { station ->
                                        Pair(station.position, station.name)
                                    }
                                }

                                val stopMarkers = mutableListOf<Overlay>()
                                if (allStops.isEmpty()) {
                                    Log.w("BusMap", "No stations available to create markers")
                                    onError("Không có trạm xe buýt để hiển thị")
                                } else {
                                    for ((geoPoint, stationName) in allStops) {
                                        try {
                                            val stopIcon = context.getDrawable(R.drawable.ic_location32)
                                            if (stopIcon != null) {
                                                val marker = Marker(mapView).apply {
                                                    position = geoPoint
                                                    title = stationName
                                                    icon = stopIcon
                                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                                }
                                                stopMarkers.add(marker)
                                                Log.d("BusMap", "Created marker for $stationName at $geoPoint")
                                            } else {
                                                Log.w("BusMap", "Stop icon (ic_location32) not found")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("BusMap", "Failed to create marker for $stationName: ${e.message}")
                                        }
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    onOverlaysChanged(stopMarkers)
                                    onError(null)
                                    mapView.invalidate()
                                    Log.d("BusMap", "Map invalidated with ${stopMarkers.size} markers")
                                }

                            } catch (e: Exception) {
                                Log.e("BusMap", "Error creating map markers: ${e.message}")
                                onError("Lỗi khi tạo điểm dừng: ${e.message}")
                            }
                        }
                    } else {
                        Log.e("BusMap", "MapView is null in onMapViewReady callback")
                        onError("Không thể khởi tạo bản đồ")
                    }
                }
            )
        }

        // Enhanced search bar
        if (isEnable) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = 120.dp)
                    .zIndex(100f)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Tìm kiếm địa điểm, trạm xe buýt...", fontSize = 14.sp) },
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
                                    onClick = onClearSearch,
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
                                    onSearchResultSelected(searchResults.first())
                                }
                            }
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )
                }

                // Search results dropdown
                if (showSearchResults && searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .zIndex(101f),
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                    ) {
                        LazyColumn {
                            items(searchResults) { result ->
                                SearchResultItem(
                                    result = result,
                                    onClick = { onSearchResultSelected(result) }
                                )
                                Divider(color = Color.Gray.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }

        MapControlButtons(
            navController = navController,
            onCenterClick = onCenterClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .padding(top = 210.dp)
                .zIndex(10f)
        )
    }
}

@Composable
fun MapControlButtons(
    navController: NavController,
    onCenterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        IconButton(
            onClick = { onCenterClick() },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.center),
                contentDescription = "Center Icon",
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF2E8B57)
            )
        }
        IconButton(
            onClick = { navController.navigate("surroundingstation") },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.find),
                contentDescription = "Find Icon",
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF2E8B57)
            )
        }
    }
}

@Composable
fun IconButtons(navController: NavController) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(0.8f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable { navController.navigate("selectbus") }
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.tracuu),
                    contentDescription = "Tra cứu Icon",
                    modifier = Modifier.size(64.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tra cứu",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(0.8f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable { navController.navigate("findtheway") }
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.timduong),
                    contentDescription = "Tìm đường Icon",
                    modifier = Modifier.size(64.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tìm đường",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(0.8f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable { navController.navigate("surroundingstation") }
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.tramxungquanh),
                    contentDescription = "Trạm xung quanh Icon",
                    modifier = Modifier.size(64.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Trạm",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(0.8f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable { /* TODO: Add action for feedback button */ }
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.gopy),
                    contentDescription = "Góp ý Icon",
                    modifier = Modifier.size(64.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Góp ý",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}