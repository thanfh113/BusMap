package com.example.busmap.model

import org.osmdroid.util.GeoPoint

data class Station(
    val id: String = "",
    val name: String = "",
    val position: GeoPoint = GeoPoint(0.0, 0.0),
    val routes: List<String> = emptyList(),
    val address: String = ""
)
