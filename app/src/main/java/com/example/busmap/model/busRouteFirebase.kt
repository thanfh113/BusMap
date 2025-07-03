package com.example.busmap.model

data class BusRouteFirebase(
    val id: String = "",
    val name: String = "",
    val stops: List<String> = emptyList(),
    val operatingHours: String = "",
    val frequency: String = "",
    val ticketPrice: String = "",
    val points: List<LocationPoint> = emptyList()
)

data class LocationPoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)