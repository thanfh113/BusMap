package com.example.busmap.model

import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint

class StationRepository {
    private val database = FirebaseDatabase.getInstance()
    private val stationsRef = database.getReference("stations")

    suspend fun getAllStations(): List<Station> {
        return try {
            val snapshot = stationsRef.get().await()
            val stations = mutableListOf<Station>()

            snapshot.children.forEach { child ->
                val stationData = child.getValue(StationFirebase::class.java)
                stationData?.let { firebaseStation ->
                    stations.add(
                        Station(
                            id = firebaseStation.id,
                            name = firebaseStation.name,
                            address = firebaseStation.address,
                            position = GeoPoint(
                                firebaseStation.position.latitude,
                                firebaseStation.position.longitude
                            ),
                            routes = firebaseStation.routes
                        )
                    )
                }
            }
            stations
        } catch (e: Exception) {
            println("Error loading stations: ${e.message}")
            // Fallback to test data
            getTestStations()
        }
    }

    suspend fun getStationById(id: String): Station? {
        return try {
            val snapshot = stationsRef.child(id).get().await()
            val stationData = snapshot.getValue(StationFirebase::class.java)
            stationData?.let { firebaseStation ->
                Station(
                    id = firebaseStation.id,
                    name = firebaseStation.name,
                    address = firebaseStation.address,
                    position = GeoPoint(
                        firebaseStation.position.latitude,
                        firebaseStation.position.longitude
                    ),
                    routes = firebaseStation.routes
                )
            }
        } catch (e: Exception) {
            println("Error loading station: ${e.message}")
            null
        }
    }
}

data class StationFirebase(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val position: LocationPoint = LocationPoint(),
    val routes: List<String> = emptyList()
)


// Test data fallback
fun getTestStations(): List<Station> {
    return listOf(
        Station(
            id = "station_01",
            name = "Bến xe Mỹ Đình",
            address = "Phạm Hùng, Mỹ Đình, Nam Từ Liêm, Hà Nội",
            position = GeoPoint(21.028511, 105.854223),
            routes = listOf("01", "07", "14")
        ),
        Station(
            id = "station_02",
            name = "Hồ Gươm",
            address = "Đinh Tiên Hoàng, Hoàn Kiếm, Hà Nội",
            position = GeoPoint(21.028800, 105.857100),
            routes = listOf("03", "05", "09")
        ),
        Station(
            id = "station_03",
            name = "Bến xe Hà Đông",
            address = "Quang Trung, Hà Đông, Hà Nội",
            position = GeoPoint(20.970000, 105.775000),
            routes = listOf("02", "06", "12")
        ),
        Station(
            id = "station_04",
            name = "Cầu Giấy",
            address = "Cầu Giấy, Hà Nội",
            position = GeoPoint(21.031000, 105.800000),
            routes = listOf("01", "04", "08")
        ),
        Station(
            id = "station_05",
            name = "Đại học Bách Khoa",
            address = "Hai Bà Trưng, Hà Nội",
            position = GeoPoint(21.005000, 105.843000),
            routes = listOf("05", "11", "13")
        ),
        Station(
            id = "station_06",
            name = "Bưu điện Hà Nội",
            address = "Hoàn Kiếm, Hà Nội",
            position = GeoPoint(21.024000, 105.852000),
            routes = listOf("06", "09", "15")
        ),
        Station(
            id = "station_07",
            name = "Chợ Đồng Xuân",
            address = "Hoàn Kiếm, Hà Nội",
            position = GeoPoint(21.035000, 105.850000),
            routes = listOf("07", "10", "16")
        ),
        Station(
            id = "station_08",
            name = "Cầu Long Biên",
            address = "Long Biên, Hà Nội",
            position = GeoPoint(21.045000, 105.880000),
            routes = listOf("08", "12", "17")
        ),
        Station(
            id = "station_09",
            name = "Kim Mã",
            address = "Ba Đình, Hà Nội",
            position = GeoPoint(21.032000, 105.825000),
            routes = listOf("01", "09", "18")
        ),
        Station(
            id = "station_10",
            name = "Lăng Chủ tịch Hồ Chí Minh",
            address = "Ba Đình, Hà Nội",
            position = GeoPoint(21.036000, 105.834000),
            routes = listOf("10", "14", "19")
        )
    )
}