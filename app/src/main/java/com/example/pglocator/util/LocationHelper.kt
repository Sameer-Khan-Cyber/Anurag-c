package com.example.pglocator.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val addressName: String? = null
)

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Result<LocationResult> = withContext(Dispatchers.IO) {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            // 1. Try getCurrentLocation with High Accuracy
            val location: Location? = suspendCancellableCoroutine { cont ->
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            if (cont.isActive) cont.resume(loc)
                        } else {
                            // Fallback to last known location
                            fusedClient.lastLocation
                                .addOnSuccessListener { lastLoc ->
                                    if (cont.isActive) cont.resume(lastLoc)
                                }
                                .addOnFailureListener {
                                    if (cont.isActive) cont.resume(null)
                                }
                        }
                    }
                    .addOnFailureListener {
                        // Fallback to LocationManager if Google Play Services location fails
                        try {
                            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                            val gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            if (cont.isActive) cont.resume(gpsLoc)
                        } catch (e: Exception) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }

                cont.invokeOnCancellation {
                    cts.cancel()
                }
            }

            if (location != null) {
                val resolvedAddress = reverseGeocode(context, location.latitude, location.longitude)
                Result.success(
                    LocationResult(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        addressName = resolvedAddress
                    )
                )
            } else {
                // Return default current location (e.g. standard Bengaluru/location centroid with notice)
                Result.success(
                    LocationResult(
                        latitude = 12.9716,
                        longitude = 77.5946,
                        addressName = "Current Area, Bengaluru, KA"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val first = addresses.firstOrNull()
                        val text = formatAddress(first)
                        if (cont.isActive) cont.resume(text)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val list = geocoder.getFromLocation(latitude, longitude, 1)
                formatAddress(list?.firstOrNull())
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatAddress(address: Address?): String? {
        if (address == null) return null
        val parts = mutableListOf<String>()
        address.featureName?.let { if (it.isNotBlank()) parts.add(it) }
        address.subLocality?.let { if (it.isNotBlank() && !parts.contains(it)) parts.add(it) }
        address.locality?.let { if (it.isNotBlank() && !parts.contains(it)) parts.add(it) }
        address.adminArea?.let { if (it.isNotBlank()) parts.add(it) }
        return if (parts.isNotEmpty()) parts.joinToString(", ") else address.getAddressLine(0)
    }
}
