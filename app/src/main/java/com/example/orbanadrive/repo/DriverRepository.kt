// repo/DriverRepository.kt
package com.example.orbanadrive.repo

import android.util.Log
import com.example.orbanadrive.network.*

class DriverRepository(
    private val authApi: AuthApi,
    private val driverApi: DriverApi
) {
    // === Ofertas ===
    suspend fun getOffers(status: String? = null) = driverApi.listOffers(status)

    // Si tienes el endpoint show (GET /driver/offers/{id})
    suspend fun getOffer(offerId: Long) = driverApi.getOffer(offerId)

    // Nuevo: SOLO “offered” y no-expiradas, orden desc
    suspend fun getFreshOffers(): List<OfferItem> =
        driverApi.listOffers(status = "offered").items
            .asSequence()
            .filter { it.isLive() }
            .distinctBy { it.offer_id }
            .sortedByDescending { it.offer_id }
            .toList()

    // === Aceptar / Rechazar ===
    suspend fun accept(offerId: Long, bid: Double?): AcceptRes =
        driverApi.acceptOffer(
            offerId,
            if (bid != null) mapOf("bid_amount" to bid) else emptyMap()
        )


    suspend fun reject(offerId: Long): OkRes = driverApi.rejectOffer(offerId)

    // === Cola ===
    suspend fun getQueue() = driverApi.queue()
    suspend fun promote(offerId: Long) = driverApi.queuePromote(mapOf("offer_id" to offerId))
    suspend fun drop(offerId: Long) = driverApi.queueDrop(offerId)
    suspend fun clearQueue() = driverApi.queueClear()

    // === Ride activo / finish ===
    suspend fun activeRide() = driverApi.activeRide()
    suspend fun finishRide(rideId: Long) = driverApi.finishRide(rideId)

    // === Auth / vehículos / turno ===
    suspend fun me(): MeRes = authApi.me()
    suspend fun getVehicles(): List<VehicleItem> = driverApi.vehicles().items
    suspend fun startShift(vehicleId: Long?): Long? =
        driverApi.startShift(ShiftStartReq(vehicleId)).shift_id

    // === Ubicación / busy ===
    suspend fun sendLocation(
        lat: Double,
        lng: Double,
        busy: Boolean? = null,
        speedKmh: Double? = null
    ) {
        driverApi.updateLocation(LocationReq(lat, lng, busy, speedKmh))
    }

    suspend fun setBusy(
        busy: Boolean,
        lat: Double? = null,
        lng: Double? = null
    ): Boolean {
        val body = mutableMapOf<String, Any?>("busy" to busy)

        fun valid(a: Double?, b: Double?) =
            a != null && b != null &&
                    a.isFinite() && b.isFinite() &&
                    a in -90.0..90.0 && b in -180.0..180.0 &&
                    !(a == 0.0 && b == 0.0)

        if (valid(lat, lng)) {
            body["lat"] = lat
            body["lng"] = lng
        }

        return runCatching {
            driverApi.postLocation(body)
            true
        }.getOrElse { e ->
            Log.e("DriverRepository", "Error en postLocation", e)
            false
        }
    }

    // === Settings dispatch ===
    suspend fun dispatchSettings(): DispatchSettingsRes = driverApi.dispatchSettings()
}
