package com.example.orbanadrive.repo

import android.util.Log
import com.example.orbanadrive.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber


class DriverRepository(
    private val authApi: AuthApi,
    private val driverApi: DriverApi
) {
    // === Ofertas ===

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var offersJob: Job? = null
    private val _offers = MutableStateFlow<List<OfferItem>>(emptyList())
    val offersFlow: StateFlow<List<OfferItem>> = _offers.asStateFlow()

    fun startOffersPolling(pollMs: Long = 3000L) {
        if (offersJob?.isActive == true) return
        offersJob = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val newList = getFreshOffers()
                    if (newList != _offers.value) _offers.value = newList
                } catch (ce: CancellationException) {
                    // MUY IMPORTANTE: re-lanzar la cancelación
                    throw ce
                } catch (t: Throwable) {
                    // log opcional
                    // Timber.w(t, "poll offers failed")
                }
                delay(pollMs)
            }
        }
    }

    fun stopOffersPolling() {
        offersJob?.cancel()
        offersJob = null
    }
    suspend fun getOffers(status: String? = null) = driverApi.listOffers(status)

    suspend fun getOffer(offerId: Long) = driverApi.getOffer(offerId)

    suspend fun getFreshOffers(): List<OfferItem> =
        driverApi.listOffers(status = "offered").items
            .asSequence()
            .filter { it.isLive() }
            .distinctBy { it.offer_id }
            .sortedByDescending { it.offer_id }
            .toList()

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

    // === Ride activo / finish / cancelar / stop ===
    suspend fun activeRide() = driverApi.activeRide()
    suspend fun finishRide(rideId: Long) = driverApi.finishRide(rideId)
    suspend fun cancelRide(rideId: Long, reason: String?) =
        driverApi.cancelRide(rideId, DriverApi.CancelReq(reason))
    suspend fun completeStop(rideId: Long) = driverApi.completeStop(rideId)

    // === Auth / vehículos / turno ===
    suspend fun me(): MeRes = authApi.me()
    suspend fun getVehicles(): List<VehicleItem> = driverApi.vehicles().items
    suspend fun startShift(vehicleId: Long?): Long? =
        driverApi.startShift(DriverApi.ShiftStartReq(vehicleId)).shift_id
    suspend fun finishShift(shiftId: Long? = null): OkRes =
        driverApi.finishShift(DriverApi.ShiftFinishReq(shiftId))

    // === Ubicación / busy ===
    suspend fun sendLocation(
        lat: Double,
        lng: Double,
        busy: Boolean? = null,
        speedKmh: Double? = null
    ): Boolean {
        return runCatching {
            // Validar coordenadas primero
            if (!lat.isFinite() || !lng.isFinite() ||
                lat !in -90.0..90.0 || lng !in -180.0..180.0) {
                return false
            }

            driverApi.updateLocation(DriverApi.LocationReq(lat, lng, busy, speedKmh))
            true
        }.getOrElse { e ->
            Timber.w("sendLocation failed: ${e.message}")
            false
        }
    }
    suspend fun setBusy(
        busy: Boolean,
        lat: Double? = null,
        lng: Double? = null
    ): Boolean {
        val body = mutableMapOf<String, Any>("busy" to busy)

        fun valid(a: Double?, b: Double?) =
            a != null && b != null &&
                    a.isFinite() && b.isFinite() &&
                    a in -90.0..90.0 && b in -180.0..180.0 &&
                    !(a == 0.0 && b == 0.0)

        if (valid(lat, lng)) {
            body["lat"] = lat!!
            body["lng"] = lng!!
        } else {
            // si no hay coords, NO pegues al backend para evitar 422
            return false
        }

        return runCatching {
            driverApi.postLocation(body)   // tu endpoint existente
            true
        }.getOrElse { e ->
            android.util.Log.e("DriverRepository", "postLocation fail", e)
            false
        }
    }


    // === GEO / SETTINGS / CANCEL REASONS ===
    suspend fun route(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double) =
        driverApi.geoRoute(
            DriverApi.RouteReq(
                from = DriverApi.LatLng(fromLat, fromLng),
                to   = DriverApi.LatLng(toLat, toLng)
            )
        )

    suspend fun dispatchSettings(): DispatchSettingsRes = driverApi.dispatchSettings()

    suspend fun cancelReasons(): List<String> =
        runCatching { driverApi.cancelReasons().items }.getOrDefault(emptyList())
}
