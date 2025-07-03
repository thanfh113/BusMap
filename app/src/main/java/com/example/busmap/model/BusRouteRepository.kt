package com.example.busmap.model

import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.osmdroid.util.GeoPoint
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BusRouteRepository {
    private val database = FirebaseDatabase.getInstance()
    private val busRoutesRef = database.getReference("bus_routes")

    // Lấy tất cả tuyến xe
    suspend fun getAllBusRoutes(): List<BusRoute> = suspendCancellableCoroutine { continuation ->
        busRoutesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val routes = mutableListOf<BusRoute>()
                    for (routeSnapshot in snapshot.children) {
                        val firebaseRoute = routeSnapshot.getValue(BusRouteFirebase::class.java)
                        firebaseRoute?.let { fbRoute ->
                            val geoPoints = fbRoute.points.map { point ->
                                GeoPoint(point.latitude, point.longitude)
                            }
                            val busRoute = BusRoute(
                                id = fbRoute.id,
                                name = fbRoute.name,
                                stops = fbRoute.stops,
                                operatingHours = fbRoute.operatingHours,
                                frequency = fbRoute.frequency,
                                ticketPrice = fbRoute.ticketPrice,
                                points = geoPoints
                            )
                            routes.add(busRoute)
                        }
                    }
                    continuation.resume(routes)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        })

        continuation.invokeOnCancellation {
            // Cleanup nếu cần
        }
    }

    // Lấy tuyến xe theo ID
    suspend fun getBusRouteById(routeId: String): BusRoute? = suspendCancellableCoroutine { continuation ->
        busRoutesRef.child(routeId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val firebaseRoute = snapshot.getValue(BusRouteFirebase::class.java)
                    if (firebaseRoute != null) {
                        val geoPoints = firebaseRoute.points.map { point ->
                            GeoPoint(point.latitude, point.longitude)
                        }
                        val busRoute = BusRoute(
                            id = firebaseRoute.id,
                            name = firebaseRoute.name,
                            stops = firebaseRoute.stops,
                            operatingHours = firebaseRoute.operatingHours,
                            frequency = firebaseRoute.frequency,
                            ticketPrice = firebaseRoute.ticketPrice,
                            points = geoPoints
                        )
                        continuation.resume(busRoute)
                    } else {
                        continuation.resume(null)
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        })
    }

    // Lắng nghe thay đổi real-time
    fun getBusRoutesFlow(): Flow<List<BusRoute>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val routes = mutableListOf<BusRoute>()
                    for (routeSnapshot in snapshot.children) {
                        val firebaseRoute = routeSnapshot.getValue(BusRouteFirebase::class.java)
                        firebaseRoute?.let { fbRoute ->
                            val geoPoints = fbRoute.points.map { point ->
                                GeoPoint(point.latitude, point.longitude)
                            }
                            val busRoute = BusRoute(
                                id = fbRoute.id,
                                name = fbRoute.name,
                                stops = fbRoute.stops,
                                operatingHours = fbRoute.operatingHours,
                                frequency = fbRoute.frequency,
                                ticketPrice = fbRoute.ticketPrice,
                                points = geoPoints
                            )
                            routes.add(busRoute)
                        }
                    }
                    trySend(routes)
                } catch (e: Exception) {
                    close(e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        busRoutesRef.addValueEventListener(listener)

        awaitClose {
            busRoutesRef.removeEventListener(listener)
        }
    }
}