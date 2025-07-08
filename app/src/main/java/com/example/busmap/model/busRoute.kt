package com.example.busmap.model

import org.osmdroid.util.GeoPoint

data class BusRoute(
    val id: String,
    val name: String,
    val points: List<GeoPoint>, // Các điểm dừng chính của tuyến (từ Firebase)
    val returnPoints: List<GeoPoint>? = null, // Không sử dụng nữa, giữ lại để tương thích
    val stops: List<String>,
    val operator: String = "XN xe buýt Hà Nội",
    val frequency: String = "11-13-15-20 phút/chuyến",
    val operatingHours: String = "5h00-21h00",
    val ticketPrice: String = "10000đ/lượt",
    val detailedPoints: List<GeoPoint> = emptyList() // Detailed route từ OSRM
)