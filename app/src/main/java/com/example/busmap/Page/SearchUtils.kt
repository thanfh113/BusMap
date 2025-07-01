package com.example.busmap.Page

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.busmap.R
import com.example.busmap.model.getTestBusRoutes
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

// Data classes for search results
data class SearchResult(
    val displayName: String,
    val address: String,
    val location: GeoPoint,
    val type: SearchResultType
)

enum class SearchResultType {
    PLACE, BUS_STATION, BUS_ROUTE, LOCATION
}

data class RouteSearchResult(
    val route: List<GeoPoint>,
    val overlays: List<Overlay>,
    val busRoutes: List<String>,
    val instructions: List<String>
)

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

                        // Extract address components
                        val address = if (item.has("address")) {
                            val addr = item.getJSONObject("address")
                            buildString {
                                val houseNumber = addr.optString("house_number")
                                if (houseNumber.isNotEmpty()) append("$houseNumber ")

                                val road = addr.optString("road")
                                if (road.isNotEmpty()) append("$road, ")

                                val suburb = addr.optString("suburb")
                                if (suburb.isNotEmpty()) append("$suburb, ")

                                val city = addr.optString("city")
                                val town = addr.optString("town")
                                val village = addr.optString("village")

                                when {
                                    city.isNotEmpty() -> append(city)
                                    town.isNotEmpty() -> append(town)
                                    village.isNotEmpty() -> append(village)
                                }
                            }.trim().removeSuffix(",")
                        } else {
                            displayName.split(",").take(3).joinToString(", ")
                        }

                        results.add(SearchResult(
                            displayName = displayName.split(",").first(),
                            address = address,
                            location = GeoPoint(lat, lon),
                            type = SearchResultType.PLACE
                        ))
                    }

                    results
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            println("Search error: ${e.message}")
            emptyList()
        }
    }
}

// Bus route finding function
suspend fun findBusRoute(start: GeoPoint, end: GeoPoint): RouteSearchResult {
    return withContext(Dispatchers.IO) {
        try {
            val busRoutes = getTestBusRoutes()
            val overlays = mutableListOf<Overlay>()
            val routeIds = mutableListOf<String>()
            val instructions = mutableListOf<String>()
            val routePoints = mutableListOf<GeoPoint>()

            // Simple algorithm: find routes that pass near start and end points
            val nearbyRoutes = busRoutes.filter { route ->
                val nearStart = route.points.any { point -> point.distanceToAsDouble(start) <= 500 }
                val nearEnd = route.points.any { point -> point.distanceToAsDouble(end) <= 500 }
                nearStart && nearEnd
            }

            if (nearbyRoutes.isNotEmpty()) {
                val bestRoute = nearbyRoutes.first()
                routeIds.add(bestRoute.id)

                // Find start and end indices in the route
                val startIndex = bestRoute.points.withIndex().minByOrNull { (_, point) ->
                    point.distanceToAsDouble(start)
                }?.index ?: 0

                val endIndex = bestRoute.points.withIndex().minByOrNull { (_, point) ->
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
    return Marker(null).apply {
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
                .background(
                    when (result.type) {
                        SearchResultType.PLACE -> Color(0xFF2E8B57).copy(alpha = 0.15f)
                        SearchResultType.BUS_STATION -> Color.Blue.copy(alpha = 0.15f)
                        SearchResultType.BUS_ROUTE -> Color(0xFF2E8B57).copy(alpha = 0.15f)
                        SearchResultType.LOCATION -> Color(0xFF2E8B57).copy(alpha = 0.15f)
                    },
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    id = when (result.type) {
                        SearchResultType.PLACE -> R.drawable.ic_location32
                        SearchResultType.BUS_STATION -> R.drawable.ic_location32
                        SearchResultType.BUS_ROUTE -> R.drawable.tracuu
                        SearchResultType.LOCATION -> R.drawable.ic_location32
                    }
                ),
                contentDescription = null,
                tint = when (result.type) {
                    SearchResultType.PLACE -> Color(0xFF2E8B57)
                    SearchResultType.BUS_STATION -> Color.Blue
                    SearchResultType.BUS_ROUTE -> Color(0xFF2E8B57)
                    SearchResultType.LOCATION -> Color(0xFF2E8B57)
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
                    SearchResultType.PLACE -> "📍 Địa điểm"
                    SearchResultType.BUS_STATION -> "🚏 Trạm xe buýt"
                    SearchResultType.BUS_ROUTE -> "🚌 Tuyến xe"
                    SearchResultType.LOCATION -> "📍 Vị trí"
                },
                fontSize = 11.sp,
                color = when (result.type) {
                    SearchResultType.PLACE -> Color(0xFF2E8B57)
                    SearchResultType.BUS_STATION -> Color.Blue
                    SearchResultType.BUS_ROUTE -> Color(0xFF2E8B57)
                    SearchResultType.LOCATION -> Color(0xFF2E8B57)
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
        getAllStations()
            .filter { station -> station.name.contains(query, ignoreCase = true) }
            .map { station ->
                SearchResult(
                    displayName = station.name,
                    address = "Trạm xe buýt - Tuyến: ${station.routes.joinToString(", ")}",
                    location = station.position,
                    type = SearchResultType.BUS_STATION
                )
            }
    }
}