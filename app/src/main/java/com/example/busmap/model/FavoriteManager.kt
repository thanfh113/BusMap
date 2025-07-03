package com.example.busmap.model

import android.content.Context

// Bus Route Favorites
fun isBusRouteFavorite(context: Context, routeId: String): Boolean {
    val sharedPref = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    return sharedPref.getBoolean("route_$routeId", false)
}

fun toggleFavoriteBusRoute(context: Context, route: BusRoute) {
    val sharedPref = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    val editor = sharedPref.edit()
    val key = "route_${route.id}"
    val isCurrentlyFavorite = sharedPref.getBoolean(key, false)

    if (isCurrentlyFavorite) {
        editor.remove(key)
        editor.remove("route_data_${route.id}")
    } else {
        editor.putBoolean(key, true)
        // Store route data for retrieval
        editor.putString("route_data_${route.id}", route.name)
    }
    editor.apply()
}

fun getFavoriteBusRoutes(context: Context): List<BusRoute> {
    val sharedPref = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    val allPrefs = sharedPref.all
    val favoriteRoutes = mutableListOf<BusRoute>()

    for ((key, value) in allPrefs) {
        if (key.startsWith("route_") && !key.startsWith("route_data_") && value is Boolean && value) {
            val routeId = key.removePrefix("route_")
            val routeName = sharedPref.getString("route_data_$routeId", "Tuyến $routeId") ?: "Tuyến $routeId"
            favoriteRoutes.add(
                BusRoute(
                    id = routeId,
                    name = routeName,
                    stops = emptyList(),
                    points = emptyList(),
                    operatingHours = "5:00 - 22:00",
                    frequency = "10-15 phút",
                    ticketPrice = "7,000 VNĐ"
                )
            )
        }
    }

    return favoriteRoutes
}

// Station Favorites
fun isStationFavorite(context: Context, station: Station): Boolean {
    val sharedPref = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    return sharedPref.getBoolean("station_${station.id}", false)
}

fun toggleFavoriteStation(context: Context, station: Station) {
    val sharedPref = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    val editor = sharedPref.edit()
    val key = "station_${station.id}"
    val isCurrentlyFavorite = sharedPref.getBoolean(key, false)

    if (isCurrentlyFavorite) {
        editor.remove(key)
        editor.remove("station_data_${station.id}")
    } else {
        editor.putBoolean(key, true)
        editor.putString("station_data_${station.id}", station.name)
    }
    editor.apply()
}

fun getFavoriteStations(context: Context): List<Station> {
    val sharedPref = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    val allPrefs = sharedPref.all
    val favoriteStations = mutableListOf<Station>()

    for ((key, value) in allPrefs) {
        if (key.startsWith("station_") && !key.startsWith("station_data_") && value is Boolean && value) {
            val stationId = key.removePrefix("station_")
            val stationName = sharedPref.getString("station_data_$stationId", "Trạm $stationId")
                ?: "Trạm $stationId"
            favoriteStations.add(
                Station(
                    id = stationId,
                    name = stationName,
                    position = org.osmdroid.util.GeoPoint(21.0285, 105.8542),
                    routes = emptyList()
                )
            )
        }
    }

    return favoriteStations
}