package com.example.orbanadrive.services

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LastKnown {
    @SuppressLint("MissingPermission")
    suspend fun getLatLng(context: Context): Pair<Double, Double>? =
        suspendCancellableCoroutine { cont ->
            val f = LocationServices.getFusedLocationProviderClient(context)
            f.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null && loc.latitude.isFinite() && loc.longitude.isFinite()) {
                        cont.resume(loc.latitude to loc.longitude)
                    } else cont.resume(null)
                }
                .addOnFailureListener { cont.resume(null) }
        }
}
