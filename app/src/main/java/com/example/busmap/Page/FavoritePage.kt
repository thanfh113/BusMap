package com.example.busmap.Page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.busmap.R
import com.example.busmap.model.BusRoute
import com.example.busmap.model.Station  // Thêm import này
import com.example.busmap.model.StationRepository  // Thêm import này
import com.example.busmap.model.BusRouteRepository
import com.example.busmap.model.getFavoriteBusRoutes
import com.example.busmap.model.getFavoriteStations
import com.example.busmap.model.isBusRouteFavorite
import com.example.busmap.model.isStationFavorite
import com.example.busmap.model.toggleFavoriteBusRoute
import com.example.busmap.model.toggleFavoriteStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritePage(navController: NavController) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var favoriteRoutes by remember { mutableStateOf<List<BusRoute>>(emptyList()) }
    var favoriteStations by remember { mutableStateOf<List<Station>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    var reloadTrigger by remember { mutableStateOf(0) }

    // Load favorites when tab changes or reloadTrigger changes
    LaunchedEffect(selectedTab, reloadTrigger) {
        coroutineScope.launch {
            if (selectedTab == 0) {
                favoriteRoutes = getFavoriteBusRoutes(context)
            } else {
                favoriteStations = getFavoriteStations(context)
            }
        }
    }

    // Refresh favorites when returning to this page
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            favoriteRoutes = getFavoriteBusRoutes(context)
            favoriteStations = getFavoriteStations(context)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Danh sách yêu thích",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color(0xFF2E8B57)
            ),
            modifier = Modifier.fillMaxWidth().zIndex(1f)
        )

        // Tab Row
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
                onClick = {
                    selectedTab = 0
                    reloadTrigger++ // reload khi chuyển tab
                },
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "Tuyến (${favoriteRoutes.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if(selectedTab == 0) Color(0xFF2E8B57) else Color(0xFF757575)
                    )
                }
            }

            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    reloadTrigger++ // reload khi chuyển tab
                },
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "Trạm dừng (${favoriteStations.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if(selectedTab == 1) Color(0xFF2E8B57) else Color(0xFF757575)
                    )
                }
            }
        }

        // Content
        when (selectedTab) {
            0 -> BusRoutesFavoriteTab(
                navController = navController,
                favoriteRoutes = favoriteRoutes,
                onRoutesChanged = { reloadTrigger++ }
            )
            1 -> StationsFavoriteTab(
                favoriteStations = favoriteStations,
                onStationsChanged = { reloadTrigger++ }
            )
        }
    }
}

@Composable
fun BusRoutesFavoriteTab(
    navController: NavController,
    favoriteRoutes: List<BusRoute>,
    onRoutesChanged: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showAddRouteDialog by remember { mutableStateOf(false) }
    var localRoutes by remember { mutableStateOf(favoriteRoutes) }

    // Đồng bộ localRoutes với favoriteRoutes khi favoriteRoutes thay đổi
    LaunchedEffect(favoriteRoutes) {
        localRoutes = favoriteRoutes
    }

    if (localRoutes.isEmpty()) {
        EmptyFavoriteContent(
            icon = R.drawable.empty1,
            title = "Chưa có tuyến nào được yêu thích",
            buttonText = "Thêm ngay",
            onButtonClick = { showAddRouteDialog = true }
        )
    } else {
        Column {
            // Add route button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { showAddRouteDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E8B57)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.find),
                        contentDescription = "Add",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thêm tuyến",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(localRoutes.size) { index ->
                    val route = localRoutes[index]
                    BusRouteItem(
                        route = route,
                        isFavorite = true,
                        onFavoriteClick = {
                            coroutineScope.launch {
                                // Xóa khỏi dữ liệu (Firebase) trước
                                toggleFavoriteBusRoute(context, route)
                                // Sau đó reload lại danh sách từ repository để đồng bộ dữ liệu
                                localRoutes = withContext(Dispatchers.IO) { getFavoriteBusRoutes(context) }
                                onRoutesChanged()
                            }
                        },
                        onClick = {
                            navController.navigate("busroute/${route.id}")
                        }
                    )
                }
            }
        }
    }

    // Add Route Dialog
    if (showAddRouteDialog) {
        AddRouteDialog(
            onDismiss = { showAddRouteDialog = false },
            onRouteAdded = {
                onRoutesChanged()
                showAddRouteDialog = false
            }
        )
    }
}

@Composable
fun StationsFavoriteTab(
    favoriteStations: List<Station>,
    onStationsChanged: () -> Unit
) {
    val context = LocalContext.current
    var showAddStationDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var localStations by remember { mutableStateOf(favoriteStations) }

    // Đồng bộ localStations với favoriteStations khi favoriteStations thay đổi
    LaunchedEffect(favoriteStations) {
        localStations = favoriteStations
    }

    if (localStations.isEmpty()) {
        EmptyFavoriteContent(
            icon = R.drawable.empty1,
            title = "Chưa có trạm dừng nào được yêu thích",
            buttonText = "Thêm ngay",
            onButtonClick = { showAddStationDialog = true }
        )
    } else {
        Column {
            // Add station button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { showAddStationDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E8B57)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.find),
                        contentDescription = "Add",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thêm trạm",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(localStations.size) { index ->
                    val station = localStations[index]
                    FavoriteStationItem(
                        station = station,
                        isFavorite = true,
                        onFavoriteClick = {
                            coroutineScope.launch {
                                // Xóa khỏi dữ liệu (Firebase) trước
                                toggleFavoriteStation(context, station)
                                // Sau đó reload lại danh sách từ repository để đồng bộ dữ liệu
                                localStations = withContext(Dispatchers.IO) { getFavoriteStations(context) }
                                onStationsChanged()
                            }
                        },
                        onClick = {
                            // Navigate to map with station centered
                        }
                    )
                }
            }
        }
    }

    // Add Station Dialog
    if (showAddStationDialog) {
        AddStationDialog(
            onDismiss = { showAddStationDialog = false },
            onStationAdded = {
                onStationsChanged()
                showAddStationDialog = false
            }
        )
    }
}

@Composable
fun AddStationDialog(
    onDismiss: () -> Unit,
    onStationAdded: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Station>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun searchStations(query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            return
        }

        isSearching = true
        coroutineScope.launch {
            try {
                val stationRepository = StationRepository()
                val results = withContext(Dispatchers.IO) {
                    stationRepository.getAllStations().filter { station ->
                        station.name.contains(query, ignoreCase = true) ||
                                station.routes.any { route -> route.contains(query, ignoreCase = true) }
                    }.take(20)
                }
                searchResults = results
            } catch (e: Exception) {
                println("Search error: ${e.message}")
            } finally {
                isSearching = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Thêm trạm yêu thích",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                // Search field - thu nhỏ
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        searchStations(query)
                    },
                    label = { Text("Tìm kiếm trạm xe buýt...", fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp), // Đặt chiều cao cố định
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.search),
                            contentDescription = "Search",
                            tint = Color(0xFF2E8B57),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search results
                if (searchQuery.isNotEmpty()) {
                    if (searchResults.isEmpty() && !isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Không tìm thấy trạm nào",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(searchResults.size) { index ->
                                val station = searchResults[index]
                                var isFavorite by remember { mutableStateOf(false) }
                                LaunchedEffect(station.id) {
                                    isFavorite = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                        isStationFavorite(context, station)
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (!isFavorite) {
                                                coroutineScope.launch {
                                                    toggleFavoriteStation(context, station)
                                                    onStationAdded()
                                                }
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isFavorite)
                                            Color.Gray.copy(alpha = 0.1f)
                                        else Color.White
                                    ),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = if (isFavorite) 0.dp else 2.dp
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_location32),
                                            contentDescription = "Station",
                                            tint = if (isFavorite) Color.Gray else Color(0xFF2E8B57),
                                            modifier = Modifier.size(18.dp)
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = station.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isFavorite) Color.Gray else Color.Black,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (station.routes.isNotEmpty()) {
                                                Text(
                                                    text = "Tuyến: ${station.routes.joinToString(", ")}",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        if (isFavorite) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "Already favorite",
                                                tint = Color.Red,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.search),
                                contentDescription = "Search",
                                modifier = Modifier.size(40.dp),
                                tint = Color.Gray.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nhập tên trạm hoặc tuyến để tìm kiếm",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Đóng",
                    color = Color(0xFF2E8B57)
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth(0.9f) // Giảm độ rộng dialog
            .wrapContentHeight()
    )
}

@Composable
fun AddRouteDialog(
    onDismiss: () -> Unit,
    onRouteAdded: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<BusRoute>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun searchRoutes(query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            return
        }

        isSearching = true
        coroutineScope.launch {
            try {
                val routeRepository = BusRouteRepository()
                val results = withContext(Dispatchers.IO) {
                    routeRepository.getAllBusRoutes().filter { route ->
                        route.id.contains(query, ignoreCase = true) ||
                        route.name.contains(query, ignoreCase = true) ||
                        route.stops.any { stop -> stop.contains(query, ignoreCase = true) }
                    }.take(20)
                }
                searchResults = results
            } catch (e: Exception) {
                println("Search error: ${e.message}")
            } finally {
                isSearching = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Thêm tuyến yêu thích",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        searchRoutes(query)
                    },
                    label = { Text("Tìm kiếm tuyến xe buýt...", fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.search),
                            contentDescription = "Search",
                            tint = Color(0xFF2E8B57),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (searchQuery.isNotEmpty()) {
                    if (searchResults.isEmpty() && !isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Không tìm thấy tuyến nào",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(searchResults.size) { index ->
                                val route = searchResults[index]
                                var isFavorite by remember { mutableStateOf(false) }
                                LaunchedEffect(route.id) {
                                    isFavorite = withContext(Dispatchers.IO) {
                                        isBusRouteFavorite(context, route.id)
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (!isFavorite) {
                                                coroutineScope.launch {
                                                    toggleFavoriteBusRoute(context, route)
                                                    onRouteAdded()
                                                }
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isFavorite)
                                            Color.Gray.copy(alpha = 0.1f)
                                        else Color.White
                                    ),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = if (isFavorite) 0.dp else 2.dp
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.tracuu),
                                            contentDescription = "Bus Route",
                                            tint = if (isFavorite) Color.Gray else Color(0xFF2E8B57),
                                            modifier = Modifier.size(18.dp)
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Tuyến ${route.id}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isFavorite) Color.Gray else Color.Black,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = route.name,
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isFavorite) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "Already favorite",
                                                tint = Color.Red,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.search),
                                contentDescription = "Search",
                                modifier = Modifier.size(40.dp),
                                tint = Color.Gray.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nhập số tuyến, tên tuyến hoặc trạm để tìm kiếm",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Đóng",
                    color = Color(0xFF2E8B57)
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight()
    )
}

@Composable
fun EmptyFavoriteContent(
    icon: Int,
    title: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = "empty",
            modifier = Modifier.size(200.dp),
            tint = Color.Unspecified
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF757575),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onButtonClick,
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2E8B57)
            )
        ) {
            Text(
                text = buttonText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun BusRouteItem(
    route: BusRoute,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var favoriteState by remember { mutableStateOf(false) }

    // Luôn lấy trạng thái mới nhất từ repository khi route thay đổi
    LaunchedEffect(route.id) {
        favoriteState = withContext(Dispatchers.IO) {
            isBusRouteFavorite(context, route.id)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Route Icon and Number
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        Color(0xFF2E8B57).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.tracuu),
                        contentDescription = "Bus Route",
                        tint = Color(0xFF2E8B57),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = route.id,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E8B57)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Route Information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Tuyến ${route.id}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = route.name,
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Route Direction
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_location32),
                        contentDescription = "Route",
                        tint = Color(0xFF2E8B57),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${route.stops.firstOrNull() ?: ""} → ${route.stops.lastOrNull() ?: ""}",
                        fontSize = 12.sp,
                        color = Color(0xFF757575),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Favorite Button
            IconButton(
                onClick = {
                    favoriteState = !favoriteState
                    coroutineScope.launch {
                        toggleFavoriteBusRoute(context, route)
                    }
                    onFavoriteClick()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (favoriteState) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (favoriteState) "Remove from favorites" else "Add to favorites",
                    tint = if (favoriteState) Color.Red else Color(0xFF757575),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun FavoriteStationItem(
    station: Station,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var favoriteState by remember { mutableStateOf(false) }

    // Luôn lấy trạng thái mới nhất từ repository khi station thay đổi
    LaunchedEffect(station.id) {
        favoriteState = withContext(Dispatchers.IO) {
            isStationFavorite(context, station)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Station Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        Color.Blue.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_location32),
                    contentDescription = "Bus Station",
                    tint = Color.Blue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Station Information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = station.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (station.routes.isNotEmpty()) {
                    Text(
                        text = "Tuyến: ${station.routes.joinToString(", ")}",
                        fontSize = 14.sp,
                        color = Color(0xFF757575),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Coordinates (for reference)
                Text(
                    text = "📍 ${String.format("%.4f, %.4f", station.position.latitude, station.position.longitude)}",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }

            // Favorite Button
            IconButton(
                onClick = {
                    favoriteState = !favoriteState
                    coroutineScope.launch {
                        toggleFavoriteStation(context, station)
                    }
                    onFavoriteClick()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (favoriteState) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (favoriteState) "Remove from favorites" else "Add to favorites",
                    tint = if (favoriteState) Color.Red else Color(0xFF757575),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}