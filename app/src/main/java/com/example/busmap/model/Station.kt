package com.example.busmap.model

import org.osmdroid.util.GeoPoint

data class Station(
    val id: String = "",
    val name: String = "",
    val position: GeoPoint = GeoPoint(0.0, 0.0),
    val routes: List<String> = emptyList(),
    val address: String = ""
)

// Function để lấy tất cả stations
fun getAllStations(): List<Station> {
    return listOf(
        Station(
            id = "station_01",
            name = "Bến xe Mỹ Đình",
            position = GeoPoint(21.028511, 105.854223),
            routes = listOf("01", "07", "14"),
            address = "Phạm Hùng, Mỹ Đình, Nam Từ Liêm, Hà Nội"
        ),
        Station(
            id = "station_02",
            name = "Hồ Gươm",
            position = GeoPoint(21.028800, 105.857100),
            routes = listOf("03", "05", "09"),
            address = "Đinh Tiên Hoàng, Hoàn Kiếm, Hà Nội"
        ),
        Station(
            id = "station_03",
            name = "Bến xe Hà Đông",
            position = GeoPoint(20.970000, 105.775000),
            routes = listOf("02", "06", "12"),
            address = "Quang Trung, Hà Đông, Hà Nội"
        ),
        Station(
            id = "station_04",
            name = "Cầu Giấy",
            position = GeoPoint(21.031000, 105.800000),
            routes = listOf("01", "04", "08"),
            address = "Cầu Giấy, Hà Nội"
        ),
        Station(
            id = "station_05",
            name = "Đại học Bách Khoa",
            position = GeoPoint(21.005000, 105.843000),
            routes = listOf("05", "11", "13"),
            address = "Hai Bà Trưng, Hà Nội"
        ),
        Station(
            id = "station_06",
            name = "Bưu điện Hà Nội",
            position = GeoPoint(21.024000, 105.852000),
            routes = listOf("06", "09", "15"),
            address = "Hoàn Kiếm, Hà Nội"
        ),
        Station(
            id = "station_07",
            name = "Chợ Đồng Xuân",
            position = GeoPoint(21.035000, 105.850000),
            routes = listOf("07", "10", "16"),
            address = "Hoàn Kiếm, Hà Nội"
        ),
        Station(
            id = "station_08",
            name = "Cầu Long Biên",
            position = GeoPoint(21.045000, 105.880000),
            routes = listOf("08", "12", "17"),
            address = "Long Biên, Hà Nội"
        ),
        Station(
            id = "station_09",
            name = "Kim Mã",
            position = GeoPoint(21.032000, 105.825000),
            routes = listOf("01", "09", "18"),
            address = "Ba Đình, Hà Nội"
        ),
        Station(
            id = "station_10",
            name = "Lăng Chủ tịch Hồ Chí Minh",
            position = GeoPoint(21.036000, 105.834000),
            routes = listOf("10", "14", "19"),
            address = "Ba Đình, Hà Nội"
        )
    )
}