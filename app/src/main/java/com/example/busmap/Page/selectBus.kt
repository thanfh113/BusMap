package com.example.busmap.Page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.busmap.R
import com.example.busmap.model.BusRoute
import com.example.busmap.model.BusRouteRepository
import com.example.busmap.model.Station
import com.example.busmap.model.StationRepository
import com.example.busmap.model.isBusRouteFavorite
import com.example.busmap.model.isStationFavorite
import com.example.busmap.model.toggleFavoriteBusRoute
import com.example.busmap.model.toggleFavoriteStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun selectBus(navController: NavController) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    // Bus route state
    val busRouteRepository = remember { BusRouteRepository() }
    var busRoutes by remember { mutableStateOf<List<BusRoute>>(emptyList()) }
    var busSearchQuery by remember { mutableStateOf("") }
    var busIsLoading by remember { mutableStateOf(true) }
    var busErrorMessage by remember { mutableStateOf<String?>(null) }

    // Station state
    val stationRepository = remember { StationRepository() }
    var stations by remember { mutableStateOf<List<Station>>(emptyList()) }
    var stationSearchQuery by remember { mutableStateOf("") }
    var stationIsLoading by remember { mutableStateOf(true) }
    var stationErrorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Filtered lists
    val filteredRoutes = remember(busRoutes, busSearchQuery) {
        if (busSearchQuery.isEmpty()) busRoutes
        else busRoutes.filter { route ->
            route.id.contains(busSearchQuery, ignoreCase = true) ||
            route.name.contains(busSearchQuery, ignoreCase = true) ||
            route.stops.any { stop -> stop.contains(busSearchQuery, ignoreCase = true) }
        }
    }
    val filteredStations = remember(stations, stationSearchQuery) {
        if (stationSearchQuery.isEmpty()) stations
        else stations.filter { station ->
            station.name.contains(stationSearchQuery, ignoreCase = true) ||
            station.routes.any { route -> route.contains(stationSearchQuery, ignoreCase = true) }
        }
    }

    // Load bus routes
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                busIsLoading = true
                busRoutes = busRouteRepository.getAllBusRoutes()
            } catch (e: Exception) {
                busErrorMessage = "Lỗi khi tải dữ liệu: ${e.message}"
            } finally {
                busIsLoading = false
            }
        }
    }
    // Load stations
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                stationIsLoading = true
                stations = withContext(Dispatchers.IO) { stationRepository.getAllStations() }
            } catch (e: Exception) {
                stationErrorMessage = "Lỗi khi tải dữ liệu: ${e.message}"
            } finally {
                stationIsLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chọn tuyến/trạm",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.back),
                        contentDescription = "Back",
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { navController.popBackStack() },
                        tint = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2E8B57)),
                modifier = Modifier.fillMaxWidth().zIndex(1f)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab])
                            .height(3.dp),
                        color = Color(0xFF2E8B57)
                    )
                },
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Tuyến xe",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if(selectedTab == 0) Color(0xFF2E8B57) else Color(0xFF757575),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Trạm dừng",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if(selectedTab == 1) Color(0xFF2E8B57) else Color(0xFF757575),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // Tuyến xe
                    if (busIsLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF2E8B57))
                        }
                    } else if (busErrorMessage != null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(busErrorMessage!!, color = Color.Red)
                        }
                    } else {
                        Column {
                            // Search Bar
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                TextField(
                                    value = busSearchQuery,
                                    onValueChange = { busSearchQuery = it },
                                    placeholder = {
                                        Text(
                                            "Tìm tuyến xe (số xe, tên tuyến, trạm...)",
                                            fontSize = 14.sp
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.search),
                                            contentDescription = "Search",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (busSearchQuery.isNotEmpty()) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.remove_32),
                                                contentDescription = "Clear",
                                                tint = Color.Gray,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable { busSearchQuery = "" }
                                            )
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = { /* Handle search if needed */ }
                                    ),
                                    singleLine = true
                                )
                            }

                            // Results count
                            if (busSearchQuery.isNotEmpty()) {
                                Text(
                                    text = "Tìm thấy ${filteredRoutes.size} tuyến xe",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }

                            // Bus Routes List
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                if (filteredRoutes.isEmpty() && busSearchQuery.isNotEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.search),
                                                    contentDescription = "No results",
                                                    modifier = Modifier.size(48.dp),
                                                    tint = Color.Gray
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Không tìm thấy tuyến xe phù hợp",
                                                    fontSize = 16.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    items(filteredRoutes) { route ->
                                        RouteItem(
                                            route = route,
                                            searchQuery = busSearchQuery,
                                            onClick = {
                                                navController.navigate("busroute/${route.id}")
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Trạm dừng
                    if (stationIsLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF2E8B57))
                        }
                    } else if (stationErrorMessage != null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stationErrorMessage!!, color = Color.Red)
                        }
                    } else {
                        Column {
                            // Search Bar
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                TextField(
                                    value = stationSearchQuery,
                                    onValueChange = { stationSearchQuery = it },
                                    placeholder = {
                                        Text(
                                            "Tìm trạm dừng (tên trạm, tuyến...)",
                                            fontSize = 14.sp
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.search),
                                            contentDescription = "Search",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (stationSearchQuery.isNotEmpty()) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.remove_32),
                                                contentDescription = "Clear",
                                                tint = Color.Gray,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable { stationSearchQuery = "" }
                                            )
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = { /* Handle search if needed */ }
                                    ),
                                    singleLine = true
                                )
                            }

                            // Results count
                            if (stationSearchQuery.isNotEmpty()) {
                                Text(
                                    text = "Tìm thấy ${filteredStations.size} trạm dừng",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }

                            // Stations List
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                if (filteredStations.isEmpty() && stationSearchQuery.isNotEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.search),
                                                    contentDescription = "No results",
                                                    modifier = Modifier.size(48.dp),
                                                    tint = Color.Gray
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Không tìm thấy trạm phù hợp",
                                                    fontSize = 16.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    items(filteredStations) { station ->
                                        StationItemSelectable(
                                            station = station,
                                            onClick = {
                                                // Chuyển sang màn hình surroundingstation và truyền vị trí trạm, hiển thị tất cả trạm
                                                navController.navigate(
                                                    "surroundingstation?lat=${station.position.latitude}&lon=${station.position.longitude}&showAllStations=true"
                                                )
                                            }
                                        )
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

@Composable
fun RouteItem(
    route: BusRoute,
    searchQuery: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var favoriteState by remember { mutableStateOf(false) }

    // Khởi tạo trạng thái yêu thích bằng coroutine
    LaunchedEffect(route.id) {
        favoriteState = withContext(Dispatchers.IO) {
            isBusRouteFavorite(context, route.id)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Route number
                Box(
                    modifier = Modifier
                        .background(
                            Color(0xFF2E8B57),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = route.id,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Thêm icon trái tim ở đây
                IconButton(
                    onClick = {
                        favoriteState = !favoriteState
                        coroutineScope.launch {
                            toggleFavoriteBusRoute(context, route)
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (favoriteState) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (favoriteState) "Bỏ yêu thích" else "Thêm yêu thích",
                        tint = if (favoriteState) Color.Red else Color(0xFF757575)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Route name
            Text(
                text = route.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Show matching stops if searching
            if (searchQuery.isNotEmpty() && route.stops.any { it.contains(searchQuery, ignoreCase = true) }) {
                Spacer(modifier = Modifier.height(4.dp))
                val matchingStops = route.stops.filter { it.contains(searchQuery, ignoreCase = true) }
                Text(
                    text = "Trạm phù hợp: ${matchingStops.take(3).joinToString(", ")}",
                    fontSize = 12.sp,
                    color = Color(0xFF2E8B57),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Route details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Điểm đầu: ${route.stops.firstOrNull() ?: "N/A"}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Điểm cuối: ${route.stops.lastOrNull() ?: "N/A"}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.next),
                    contentDescription = "View details",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun StationItemSelectable(
    station: Station,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var favoriteState by remember { mutableStateOf(false) }

    // Khởi tạo trạng thái yêu thích bằng coroutine
    LaunchedEffect(station.id) {
        favoriteState = withContext(Dispatchers.IO) {
            isStationFavorite(context, station)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Station name and routes
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 2
                )
                if (station.routes.isNotEmpty()) {
                    Text(
                        text = "Tuyến: ${station.routes.joinToString(", ")}",
                        fontSize = 14.sp,
                        color = Color(0xFF757575),
                        maxLines = 2
                    )
                }
            }
            // Favorite Button
            IconButton(
                onClick = {
                    favoriteState = !favoriteState
                    coroutineScope.launch {
                        toggleFavoriteStation(context, station)
                    }
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (favoriteState) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (favoriteState) "Bỏ yêu thích" else "Thêm yêu thích",
                    tint = if (favoriteState) Color.Red else Color(0xFF757575)
                )
            }
        }
    }
}
