package com.example.busmap.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val fullName: String = "",
    val birthDate: String = "",
    val favoriteStations: List<String> = emptyList(),
    val favoriteBusRoutes: List<String> = emptyList() // Thêm trường này
)
