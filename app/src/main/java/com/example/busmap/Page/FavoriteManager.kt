package com.example.busmap.Page

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.busmap.model.BusRoute
import com.example.busmap.model.getTestBusRoutes
import org.osmdroid.util.GeoPoint

// Favorite Bus Route functions
fun toggleFavoriteBusRoute(context: Context, busRoute: BusRoute) {
    val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    val favoriteRoutes = prefs.getStringSet("favorite_routes", emptySet())?.toMutableSet() ?: mutableSetOf()

    if (favoriteRoutes.contains(busRoute.id)) {
        favoriteRoutes.remove(busRoute.id)
    } else {
        favoriteRoutes.add(busRoute.id)
    }

    prefs.edit().putStringSet("favorite_routes", favoriteRoutes).apply()
    println("Toggled favorite for route ${busRoute.id}, now favorite: ${favoriteRoutes.contains(busRoute.id)}")
}

fun getFavoriteBusRoutes(context: Context): List<BusRoute> {
    val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    val favoriteRouteIds = prefs.getStringSet("favorite_routes", emptySet()) ?: emptySet()

    return getTestBusRoutes().filter { route ->
        favoriteRouteIds.contains(route.id)
    }
}

fun isBusRouteFavorite(context: Context, routeId: String): Boolean {
    val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    val favoriteRoutes = prefs.getStringSet("favorite_routes", emptySet()) ?: emptySet()
    return favoriteRoutes.contains(routeId)
}

// Enhanced Station functions
fun toggleFavoriteStation(context: Context, station: Station) {
    val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    val favoriteStations = prefs.getStringSet("favorite_stations", emptySet())?.toMutableSet() ?: mutableSetOf()
    val stationKey = "${station.name}_${station.position.latitude}_${station.position.longitude}"

    if (favoriteStations.contains(stationKey)) {
        favoriteStations.remove(stationKey)
    } else {
        favoriteStations.add(stationKey)
    }

    prefs.edit().putStringSet("favorite_stations", favoriteStations).apply()
    println("Toggled favorite for station ${station.name}, now favorite: ${favoriteStations.contains(stationKey)}")
}

fun getFavoriteStations(context: Context): List<Station> {
    val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    val favoriteStations = prefs.getStringSet("favorite_stations", emptySet()) ?: emptySet()

    return favoriteStations.mapNotNull { key ->
        val parts = key.split("_")
        if (parts.size >= 3) {
            try {
                val name = parts.dropLast(2).joinToString("_") // Handle station names with underscores
                val lat = parts[parts.size - 2].toDouble()
                val lon = parts[parts.size - 1].toDouble()

                // Find the station with routes from all stations
                val allStations = getAllStations()
                val originalStation = allStations.find {
                    it.name == name &&
                    Math.abs(it.position.latitude - lat) < 0.0001 &&
                    Math.abs(it.position.longitude - lon) < 0.0001
                }

                originalStation ?: Station(
                    name = name,
                    position = GeoPoint(lat, lon),
                    routes = emptyList()
                )
            } catch (e: Exception) {
                println("Error parsing favorite station: $key - ${e.message}")
                null
            }
        } else null
    }
}

fun isStationFavorite(context: Context, station: Station): Boolean {
    val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    val favoriteStations = prefs.getStringSet("favorite_stations", emptySet()) ?: emptySet()
    val stationKey = "${station.name}_${station.position.latitude}_${station.position.longitude}"
    return favoriteStations.contains(stationKey)
}

// Composable helpers for reactive favorite states
@Composable
fun rememberIsBusRouteFavorite(context: Context, routeId: String): Boolean {
    return remember(routeId) {
        derivedStateOf {
            isBusRouteFavorite(context, routeId)
        }
    }.value
}

@Composable
fun rememberIsStationFavorite(context: Context, station: Station): Boolean {
    return remember(station.name, station.position) {
        derivedStateOf {
            isStationFavorite(context, station)
        }
    }.value
}

// Clear all favorites
fun clearAllFavorites(context: Context) {
    val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    prefs.edit().clear().apply()
}

// Get favorites count
fun getFavoritesCount(context: Context): Pair<Int, Int> {
    val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    val routesCount = prefs.getStringSet("favorite_routes", emptySet())?.size ?: 0
    val stationsCount = prefs.getStringSet("favorite_stations", emptySet())?.size ?: 0
    return Pair(routesCount, stationsCount)
}