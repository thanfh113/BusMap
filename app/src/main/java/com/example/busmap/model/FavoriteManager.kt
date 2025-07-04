package com.example.busmap.model

import android.content.Context

// Station Favorites sử dụng Firebase
suspend fun isStationFavorite(context: Context, station: Station): Boolean {
    val userRepo = UserRepository()
    val user = userRepo.getCurrentUser()
    if (user == null) return false
    val userData = userRepo.getUserData(user.uid)
    return userData?.favoriteStations?.contains(station.id) == true
}

suspend fun toggleFavoriteStation(context: Context, station: Station) {
    val userRepo = UserRepository()
    val user = userRepo.getCurrentUser() ?: return
    val userData = userRepo.getUserData(user.uid)
    if (userData != null && userData.favoriteStations.contains(station.id)) {
        userRepo.removeFavoriteStation(user.uid, station.id)
    } else {
        userRepo.addFavoriteStation(user.uid, station.id)
    }
}

suspend fun getFavoriteStations(context: Context): List<Station> {
    val userRepo = UserRepository()
    val user = userRepo.getCurrentUser() ?: return emptyList()
    val userData = userRepo.getUserData(user.uid)
    val stationRepo = StationRepository()
    val allStations = stationRepo.getAllStations()
    val favoriteIds = userData?.favoriteStations ?: emptyList()
    return allStations.filter { favoriteIds.contains(it.id) }
}

// Bus Route Favorites sử dụng Firebase
suspend fun isBusRouteFavorite(context: Context, routeId: String): Boolean {
    val userRepo = UserRepository()
    val user = userRepo.getCurrentUser()
    if (user == null) return false
    val userData = userRepo.getUserData(user.uid)
    return userData?.favoriteBusRoutes?.contains(routeId) == true
}

suspend fun toggleFavoriteBusRoute(context: Context, route: BusRoute) {
    val userRepo = UserRepository()
    val user = userRepo.getCurrentUser() ?: return
    val userData = userRepo.getUserData(user.uid)
    if (userData != null && userData.favoriteBusRoutes.contains(route.id)) {
        userRepo.removeFavoriteBusRoute(user.uid, route.id)
    } else {
        userRepo.addFavoriteBusRoute(user.uid, route.id)
    }
}

suspend fun getFavoriteBusRoutes(context: Context): List<BusRoute> {
    val userRepo = UserRepository()
    val user = userRepo.getCurrentUser() ?: return emptyList()
    val userData = userRepo.getUserData(user.uid)
    val routeRepo = BusRouteRepository()
    val allRoutes = routeRepo.getAllBusRoutes()
    val favoriteIds = userData?.favoriteBusRoutes ?: emptyList()
    return allRoutes.filter { favoriteIds.contains(it.id) }
}
