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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun selectBus(navController: NavController) {
    val busRouteRepository = remember { BusRouteRepository() }
    var busRoutes by remember { mutableStateOf<List<BusRoute>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Tính toán filteredRoutes dựa trên searchQuery
    val filteredRoutes = remember(busRoutes, searchQuery) {
        if (searchQuery.isEmpty()) {
            busRoutes
        } else {
            busRoutes.filter { route ->
                route.id.contains(searchQuery, ignoreCase = true) ||
                        route.name.contains(searchQuery, ignoreCase = true) ||
                        route.stops.any { stop -> stop.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                isLoading = true
                busRoutes = busRouteRepository.getAllBusRoutes()
                println("✅ Loaded ${busRoutes.size} bus routes from Firebase")
            } catch (e: Exception) {
                println("❌ Error loading routes: ${e.message}")
                errorMessage = "Lỗi khi tải dữ liệu: ${e.message}"
            } finally {
                isLoading = false
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
                            text = "Chọn tuyến xe bus",
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
                actions = {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2E8B57)),
                modifier = Modifier.fillMaxWidth().zIndex(1f)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF2E8B57)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Đang tải danh sách tuyến xe...",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = errorMessage!!,
                                fontSize = 16.sp,
                                color = Color.Red
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    errorMessage = null
                                    coroutineScope.launch {
                                        try {
                                            isLoading = true
                                            busRoutes = busRouteRepository.getAllBusRoutes()
                                        } catch (e: Exception) {
                                            errorMessage = "Lỗi khi tải dữ liệu: ${e.message}"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E8B57)
                                )
                            ) {
                                Text("Thử lại", color = Color.White)
                            }
                        }
                    }
                }
                else -> {
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
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "Tìm kiếm tuyến xe (số xe, tên tuyến, trạm...)",
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
                                    if (searchQuery.isNotEmpty()) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.remove_32),
                                            contentDescription = "Clear",
                                            tint = Color.Gray,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable { searchQuery = "" }
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
                        if (searchQuery.isNotEmpty()) {
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
                            if (filteredRoutes.isEmpty() && searchQuery.isNotEmpty()) {
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
                                        searchQuery = searchQuery,
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
        }
    }
}

@Composable
fun RouteItem(
    route: BusRoute,
    searchQuery: String,
    onClick: () -> Unit
) {
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

                // Route status or info
                Text(
                    text = "Hoạt động",
                    fontSize = 12.sp,
                    color = Color(0xFF2E8B57),
                    fontWeight = FontWeight.Medium
                )
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