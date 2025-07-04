package com.example.busmap.model

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().getReference("users")

    suspend fun register(
        email: String,
        password: String,
        displayName: String,
        fullName: String,
        birthDate: String
    ): Result<FirebaseUser?> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            user?.let {
                val userData = User(
                    uid = it.uid,
                    email = it.email ?: "",
                    displayName = displayName,
                    fullName = fullName,
                    birthDate = birthDate,
                    favoriteStations = emptyList(),
                    favoriteBusRoutes = emptyList() // Thêm trường này
                )
                db.child(it.uid).setValue(userData).await()
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser?> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun getUserData(uid: String): User? {
        return try {
            val snapshot = db.child(uid).get().await()
            snapshot.getValue(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addFavoriteStation(uid: String, stationId: String) {
        val user = getUserData(uid) ?: return
        if (!user.favoriteStations.contains(stationId)) {
            val updated = user.favoriteStations + stationId
            db.child(uid).child("favoriteStations").setValue(updated).await()
        }
    }

    suspend fun removeFavoriteStation(uid: String, stationId: String) {
        val user = getUserData(uid) ?: return
        val updated = user.favoriteStations.filter { it != stationId }
        db.child(uid).child("favoriteStations").setValue(updated).await()
    }

    // Thêm các hàm đồng bộ tuyến xe bus yêu thích
    suspend fun addFavoriteBusRoute(uid: String, routeId: String) {
        val user = getUserData(uid) ?: return
        if (!user.favoriteBusRoutes.contains(routeId)) {
            val updated = user.favoriteBusRoutes + routeId
            db.child(uid).child("favoriteBusRoutes").setValue(updated).await()
        }
    }

    suspend fun removeFavoriteBusRoute(uid: String, routeId: String) {
        val user = getUserData(uid) ?: return
        val updated = user.favoriteBusRoutes.filter { it != routeId }
        db.child(uid).child("favoriteBusRoutes").setValue(updated).await()
    }
}