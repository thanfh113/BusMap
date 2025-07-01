package com.example.busmap.Page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.busmap.R
import com.example.busmap.model.getTestBusRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun selectBus(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var filteredRoutes by remember { mutableStateOf(getTestBusRoutes()) }

    // Filter routes based on search query
    LaunchedEffect(searchQuery) {
        filteredRoutes = if (searchQuery.isBlank()) {
            getTestBusRoutes()
        } else {
            getTestBusRoutes().filter { route ->
                route.id.contains(searchQuery, ignoreCase = true) ||
                route.name.contains(searchQuery, ignoreCase = true) ||
                route.stops.any { it.contains(searchQuery, ignoreCase = true) }
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
            Column {
                // Enhanced search bar
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
                        placeholder = { Text("Tìm tuyến xe, trạm dừng...", fontSize = 14.sp) },
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
                                contentDescription = "Search Icon",
                                tint = Color(0xFF2E8B57),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
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
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )
                }

                // Results counter
                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = "Tìm thấy ${filteredRoutes.size} tuyến xe",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                // Route list
                if (filteredRoutes.isEmpty()) {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.empty1),
                            contentDescription = "No results",
                            modifier = Modifier.size(120.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Không có tuyến xe nào"
                                  else "Không tìm thấy tuyến xe phù hợp",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Thử tìm kiếm với từ khóa khác",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredRoutes.size) { index ->
                            RouteItem(
                                route = filteredRoutes[index],
                                searchQuery = searchQuery,
                                onClick = { navController.navigate("busroute/${filteredRoutes[index].id}") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RouteItem(
    route: com.example.busmap.model.BusRoute,
    searchQuery: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tuyến [${route.id}]",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E8B57),
                    modifier = Modifier.width(100.dp)
                )
                Text(
                    text = route.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Route info
            Text(
                text = "Lộ trình: ${route.stops.firstOrNull() ?: ""} ↔ ${route.stops.lastOrNull() ?: ""}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            if (searchQuery.isNotEmpty() && route.stops.any { it.contains(searchQuery, ignoreCase = true) }) {
                val matchingStops = route.stops.filter { it.contains(searchQuery, ignoreCase = true) }
                Text(
                    text = "Trạm phù hợp: ${matchingStops.joinToString(", ")}",
                    fontSize = 12.sp,
                    color = Color(0xFF2E8B57),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Giá vé: 10.000đ/lượt",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "5h00 - 21h00",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}