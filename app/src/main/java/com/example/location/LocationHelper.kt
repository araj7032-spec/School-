package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.example.data.model.GeofenceStatus
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Pair<Double, Double>? {
        return suspendCancellableCoroutine { continuation ->
            val cts = CancellationTokenSource()
            try {
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc: Location? ->
                        if (loc != null) {
                            continuation.resume(Pair(loc.latitude, loc.longitude))
                        } else {
                            // Fallback to last location
                            fusedClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                                if (lastLoc != null) {
                                    continuation.resume(Pair(lastLoc.latitude, lastLoc.longitude))
                                } else {
                                    continuation.resume(null)
                                }
                            }.addOnFailureListener {
                                continuation.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }
            } catch (e: SecurityException) {
                continuation.resume(null)
            }

            continuation.invokeOnCancellation {
                cts.cancel()
            }
        }
    }

    companion object {
        fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            return results[0]
        }

        fun evaluateGeofence(
            currentLat: Double,
            currentLng: Double,
            schoolLat: Double,
            schoolLng: Double,
            radiusMeters: Double,
            hasPermission: Boolean,
            isSimulation: Boolean = false
        ): GeofenceStatus {
            val distance = calculateDistance(currentLat, currentLng, schoolLat, schoolLng)
            val isInside = distance <= radiusMeters.toFloat()
            return GeofenceStatus(
                isInside = isInside,
                distanceMeters = distance,
                currentLat = currentLat,
                currentLng = currentLng,
                schoolLat = schoolLat,
                schoolLng = schoolLng,
                allowedRadius = radiusMeters,
                hasPermission = hasPermission,
                isSimulation = isSimulation
            )
        }
    }
}
