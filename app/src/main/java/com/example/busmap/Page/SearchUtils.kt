package com.example.busmap.Page

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.busmap.R
import com.example.busmap.model.BusRoute
import com.example.busmap.model.BusRouteRepository
import com.example.busmap.model.Station
import com.example.busmap.model.StationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.MapView

// Data classes for search results
data class SearchResult(
    val displayName: String,
    val address: String,
    val location: GeoPoint,
    val type: SearchResultType
) {
    val name: String get() = displayName
}

enum class SearchResultType {
    PLACE, BUS_STATION, BUS_ROUTE, LOCATION
}

data class RouteSearchResult(
    val route: List<GeoPoint>,
    val overlays: List<Overlay>,
    val busRoutes: List<String>,
    val instructions: List<String>
)

// Repositories for data access
private val busRouteRepository = BusRouteRepository()
private val stationRepository = StationRepository()

// Nominatim search function
suspend fun searchLocations(
    query: String,
    context: Context,
    currentLocation: GeoPoint?
): List<SearchResult> {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val encodedQuery = URLEncoder.encode(query, "UTF-8")

            // Add Vietnam bias to search
            val bias = currentLocation?.let { location ->
                "&viewbox=${location.longitude-0.1},${location.latitude+0.1},${location.longitude+0.1},${location.latitude-0.1}&bounded=1"
            } ?: "&countrycodes=vn"

            val url = "https://nominatim.openstreetmap.org/search?" +
                    "q=$encodedQuery" +
                    "&format=json" +
                    "&addressdetails=1" +
                    "&limit=10" +
                    "&accept-language=vi,en" +
                    bias

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "BusMap Android App")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonString = response.body?.string() ?: "[]"
                    val jsonArray = org.json.JSONArray(jsonString)
                    val results = mutableListOf<SearchResult>()

                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val lat = item.getDouble("lat")
                        val lon = item.getDouble("lon")
                        val displayName = item.getString("display_name")

                        results.add(
                            SearchResult(
                                displayName = displayName.split(",")[0], // Take first part
                                address = displayName,
                                location = GeoPoint(lat, lon),
                                type = SearchResultType.PLACE
                            )
                        )
                    }

                    // Also search in local bus stations and routes
                    val localResults = searchLocalData(query)
                    results.addAll(localResults)

                    results.take(10) // Limit results
                } else {
                    // Fallback to local search only
                    searchLocalData(query)
                }
            }
        } catch (e: Exception) {
            println("Search error: ${e.message}")
            // Fallback to local search
            searchLocalData(query)
        }
    }
}

// Search in local bus data
private suspend fun searchLocalData(query: String): List<SearchResult> {
    return withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()

        try {
            // Search stations
            val stations = stationRepository.getAllStations()
            val matchingStations = stations.filter { station ->
                station.name.contains(query, ignoreCase = true)
            }

            results.addAll(matchingStations.map { station ->
                SearchResult(
                    displayName = station.name,
                    address = "Trạm xe buýt - Tuyến: ${station.routes.joinToString(", ")}",
                    location = station.position,
                    type = SearchResultType.BUS_STATION
                )
            })

            // Search bus routes
            val busRoutes = busRouteRepository.getAllBusRoutes()
            val matchingRoutes = busRoutes.filter { route ->
                route.name.contains(query, ignoreCase = true) ||
                        route.id.contains(query, ignoreCase = true) ||
                        route.stops.any { stop -> stop.contains(query, ignoreCase = true) }
            }

            results.addAll(matchingRoutes.map { route ->
                SearchResult(
                    displayName = "Tuyến ${route.id} - ${route.name}",
                    address = "Tuyến xe buýt: ${route.stops.take(3).joinToString(" → ")}...",
                    location = route.points.firstOrNull() ?: GeoPoint(21.0285, 105.8542),
                    type = SearchResultType.BUS_ROUTE
                )
            })

        } catch (e: Exception) {
            println("Local search error: ${e.message}")
        }

        results
    }
}

// Bus route finding function
suspend fun findBusRoute(start: GeoPoint, end: GeoPoint): RouteSearchResult {
    return withContext(Dispatchers.IO) {
        try {
            val busRoutes = busRouteRepository.getAllBusRoutes()
            val overlays = mutableListOf<Overlay>()
            val routeIds = mutableListOf<String>()
            val instructions = mutableListOf<String>()
            val routePoints = mutableListOf<GeoPoint>()

            // Simple algorithm: find routes that pass near start and end points
            val nearbyRoutes = busRoutes.filter { route ->
                val nearStart = route.points.any { point ->
                    point.distanceToAsDouble(start) <= 500.0
                }
                val nearEnd = route.points.any { point ->
                    point.distanceToAsDouble(end) <= 500.0
                }
                nearStart && nearEnd
            }

            if (nearbyRoutes.isNotEmpty()) {
                val bestRoute = nearbyRoutes.first()
                routeIds.add(bestRoute.id)

                // Find start and end indices in the route
                val startIndex = bestRoute.points.withIndex().minByOrNull { (_, point: GeoPoint) ->
                    point.distanceToAsDouble(start)
                }?.index ?: 0

                val endIndex = bestRoute.points.withIndex().minByOrNull { (_, point: GeoPoint) ->
                    point.distanceToAsDouble(end)
                }?.index ?: bestRoute.points.size - 1

                // Create route segment
                val routeSegment = if (startIndex <= endIndex) {
                    bestRoute.points.subList(startIndex, endIndex + 1)
                } else {
                    bestRoute.points.subList(endIndex, startIndex + 1).reversed()
                }

                routePoints.addAll(routeSegment)

                // Create polyline overlay
                val polyline = Polyline().apply {
                    setPoints(routeSegment)
                    color = android.graphics.Color.BLUE
                    width = 8f
                }
                overlays.add(polyline)

                // Add start and end markers
                overlays.add(createMarker(start, "Điểm đi", android.graphics.Color.GREEN))
                overlays.add(createMarker(end, "Điểm đến", android.graphics.Color.RED))

                instructions.add("Đi bộ đến trạm ${bestRoute.stops.getOrNull(startIndex) ?: "gần nhất"}")
                instructions.add("Lên xe tuyến ${bestRoute.id}")
                instructions.add("Xuống xe tại trạm ${bestRoute.stops.getOrNull(endIndex) ?: "gần đích"}")
                instructions.add("Đi bộ đến đích")
            } else {
                // No direct route found, suggest walking or multiple transfers
                instructions.add("Không tìm thấy tuyến xe trực tiếp")
                instructions.add("Gợi ý: Sử dụng taxi hoặc xe ôm")
            }

            RouteSearchResult(
                route = routePoints,
                overlays = overlays,
                busRoutes = routeIds,
                instructions = instructions
            )
        } catch (e: Exception) {
            println("Route finding error: ${e.message}")
            RouteSearchResult(
                route = emptyList(),
                overlays = emptyList(),
                busRoutes = emptyList(),
                instructions = listOf("Lỗi tìm đường: ${e.message}")
            )
        }
    }
}

// Helper function to create marker
private fun createMarker(location: GeoPoint, title: String, color: Int): Marker {
    return Marker(null as MapView?).apply {
        position = location
        this.title = title
        // You can customize marker appearance here
    }
}

// Search result item composable with enhanced UI for suggestions
@Composable
fun SearchResultItem(
    result: SearchResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Location type icon with background
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when (result.type) {
                        SearchResultType.BUS_STATION -> Color(0xFF2E8B57).copy(alpha = 0.1f)
                        SearchResultType.BUS_ROUTE -> Color(0xFF1E90FF).copy(alpha = 0.1f)
                        else -> Color.Gray.copy(alpha = 0.1f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    id = when (result.type) {
                        SearchResultType.BUS_STATION -> R.drawable.ic_location32
                        SearchResultType.BUS_ROUTE -> R.drawable.tracuu
                        else -> R.drawable.search
                    }
                ),
                contentDescription = null,
                tint = when (result.type) {
                    SearchResultType.BUS_STATION -> Color(0xFF2E8B57)
                    SearchResultType.BUS_ROUTE -> Color(0xFF1E90FF)
                    else -> Color.Gray
                },
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.Black
            )
            Text(
                text = result.address,
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Type indicator
            Text(
                text = when (result.type) {
                    SearchResultType.BUS_STATION -> "Trạm xe buýt"
                    SearchResultType.BUS_ROUTE -> "Tuyến xe buýt"
                    SearchResultType.PLACE -> "Địa điểm"
                    SearchResultType.LOCATION -> "Vị trí"
                },
                fontSize = 11.sp,
                color = when (result.type) {
                    SearchResultType.BUS_STATION -> Color(0xFF2E8B57)
                    SearchResultType.BUS_ROUTE -> Color(0xFF1E90FF)
                    else -> Color.Gray
                },
                maxLines = 1
            )
        }

        // Distance indicator arrow
        Icon(
            painter = painterResource(id = R.drawable.next),
            contentDescription = "Select",
            tint = Color.Gray.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
    }
}

// Search function for stations
suspend fun searchStations(query: String): List<SearchResult> {
    return withContext(Dispatchers.IO) {
        try {
            val stations = stationRepository.getAllStations()
            stations.filter { station ->
                station.name.contains(query, ignoreCase = true)
            }.map { station ->
                SearchResult(
                    displayName = station.name,
                    address = "Trạm xe buýt - Tuyến: ${station.routes.joinToString(", ")}",
                    location = station.position,
                    type = SearchResultType.BUS_STATION
                )
            }
        } catch (e: Exception) {
            println("Station search error: ${e.message}")
            emptyList()
        }
    }
}

// Search function for bus routes
suspend fun searchBusRoutes(query: String): List<SearchResult> {
    return withContext(Dispatchers.IO) {
        try {
            val busRoutes = busRouteRepository.getAllBusRoutes()
            busRoutes.filter { route ->
                route.name.contains(query, ignoreCase = true) ||
                route.name.contains(query, ignoreCase = true) ||
                        route.id.contains(query, ignoreCase = true)
            }.map { route ->
                SearchResult(
                    displayName = "Tuyến ${route.id} - ${route.name}",
                    address = "Tuyến xe buýt: ${route.stops.take(3).joinToString(" → ")}",
                    location = route.points.firstOrNull() ?: GeoPoint(21.0285, 105.8542),
                    type = SearchResultType.BUS_ROUTE
                )
            }
        } catch (e: Exception) {
            println("Bus route search error: ${e.message}")
            emptyList()
        }
    }
}