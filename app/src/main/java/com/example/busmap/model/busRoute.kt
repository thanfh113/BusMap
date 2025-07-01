package com.example.busmap.model

import org.osmdroid.util.GeoPoint

data class BusRoute(
    val id: String,
    val name: String,
    val points: List<GeoPoint>, // Các điểm dừng chính của tuyến
    val returnPoints: List<GeoPoint>? = null, // Không sử dụng nữa, giữ lại để tương thích
    val stops: List<String>,
    val operator: String = "XN xe buýt Hà Nội",
    val frequency: String = "11-13-15-20 phút/chuyến",
    val operatingHours: String = "5h00-21h00",
    val ticketPrice: String = "10000đ/lượt"
)

fun getTestBusRoutes(): List<BusRoute> {
    return listOf(
        BusRoute(
            id = "01",
            name = "Bến xe Gia Lâm - Bến xe Yên Nghĩa",
            points = listOf(
                GeoPoint(21.0512, 105.8807), // Bến xe Gia Lâm
                GeoPoint(21.0302, 105.8577), // Cầu Chương Dương
                GeoPoint(21.0012, 105.8147), // Ngã tư Sở
                GeoPoint(20.9756, 105.7504)  // Bến xe Yên Nghĩa
            ),
            returnPoints = listOf(
                GeoPoint(20.9756, 105.7504), // Bến xe Yên Nghĩa
                GeoPoint(21.0012, 105.8147), // Ngã tư Sở
                GeoPoint(21.0302, 105.8577), // Cầu Chương Dương
                GeoPoint(21.0512, 105.8807)  // Bến xe Gia Lâm
            ),
            stops = listOf(
                "Bến xe Gia Lâm",
                "Cầu Chương Dương",
                "Ngã tư Sở",
                "Bến xe Yên Nghĩa"
            )
        ),
        BusRoute(
            id = "02",
            name = "Bến xe Nước Ngầm - Cầu Giấy",
            points = listOf(
                GeoPoint(20.9810, 105.8100),
                GeoPoint(21.0000, 105.8200),
                GeoPoint(21.0200, 105.8300),
                GeoPoint(21.0400, 105.8400)
            ),
            returnPoints = listOf(
                GeoPoint(21.0400, 105.8400),
                GeoPoint(21.0200, 105.8300),
                GeoPoint(21.0000, 105.8200),
                GeoPoint(20.9810, 105.8100)
            ),
            stops = listOf(
                "Bến xe Nước Ngầm",
                "Trần Duy Hưng",
                "Láng Hạ",
                "Cầu Giấy"
            )
        )
    )
}