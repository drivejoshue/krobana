package com.example.orbanadrive.network

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json

// ---------- GENÉRICOS ----------
data class OkRes(
    val ok: Boolean,
    val message: String? = null
)

data class AcceptRes(
    val ok: Boolean,
    val mode: String? = null,           // "queued" | "activated" | "accepted"
    val ride_id: Long? = null
)

// ---------- AUTH / ME ----------
data class MeRes(
    val ok: Boolean,
    val user: UserDto?,
    val driver: DriverDto?,
    @Json(name = "current_shift")
    @SerializedName("current_shift")
    val currentShift: ShiftDto?,
    val vehicle: VehicleDto?
) {
    data class UserDto(
        val id: Int,
        val name: String?,
        val email: String?,
        @Json(name = "tenant_id")
        @SerializedName("tenant_id")
        val tenant_id: Int
    )
    data class DriverDto(
        val id: Int,
        val status: String?,
        val name: String?,
        val phone: String?,
        val email: String?,
        val foto_path: String?,
        val last_lat: String?,
        val last_lng: String?,
        val last_ping_at: String?
    )
    data class ShiftDto(
        val id: Int,
        val status: String?,          // "abierto"/"cerrado"
        val started_at: String?,
        val ended_at: String?,
        @Json(name = "vehicle_id")
        @SerializedName("vehicle_id")
        val vehicle_id: Int?
    )
    data class VehicleDto(
        val id: Int,
        val economico: String?,
        val plate: String?,
        val brand: String?,
        val model: String?,
        val type: String?
    )
}

// ---------- VEHÍCULOS ----------
data class VehiclesRes(
    val items: List<VehicleItem>
)
data class VehicleItem(
    val id: Int,
    val economico: String?,
    val plate: String?,
    val brand: String?,
    val model: String?,
    val type: String?
)

// ---------- OFFERS ----------
data class OfferListRes(
    val ok: Boolean,
    val driver: DriverRef?,
    val count: Int,
    val items: List<OfferItem>
) {
    data class DriverRef(
        val id: Int,
        @Json(name = "tenant_id")
        @SerializedName("tenant_id")
        val tenant_id: Int
    )
}

data class OfferShowRes(
    val ok: Boolean,
    val item: OfferItem
)

data class StopDto(
    val lat: Double?,
    val lng: Double?,
    val label: String?
)

data class OfferItem(
    // offer (ride_offers)
    val offer_id: Long,
    val offer_status: String?,        // offered/accepted/rejected/...
    val sent_at: String?,
    val responded_at: String?,
    val expires_at: String?,
    val eta_seconds: Int?,
    val distance_m: Int?,
    val round_no: Int?,
    val is_direct: Int?,              // 1 directa, 0 ola
    val wave_reached: Boolean?,

    // ride (rides)
    val ride_id: Long,
    val ride_status: String?,
    val passenger_name: String?,
    val passenger_phone: String?,
    val passenger_photo: String? = null,  // por si lo agregas
    val route_polyline: String?,
    val pax: Int?,
    val origin_label: String?,
    val origin_lat: Double?,
    val origin_lng: Double?,
    val dest_label: String?,
    val dest_lat: Double?,
    val dest_lng: Double?,
    val quoted_amount: Double?,
    val total_amount: Double? = null,
    val ride_distance_m: Int?,
    val ride_duration_s: Int?,
    val allow_bidding: Boolean? = null,
    val passenger_offer: Double? = null,
    val driver_offer: Double? = null,
    val agreed_amount: Double? = null,
    val requested_channel: String?,
    val notes: String?,

    // stops
    val stops: List<StopDto> = emptyList(),
    val stops_count: Int = 0,
    val stop_index: Int = 0,

    // sugerencias (en /show)
    val min_bid: Double? = null,
    val max_bid: Double? = null,
    val suggested_bid: Double? = null
)

// ---------- ACTIVE RIDE ----------
data class ActiveRideRes(
    val ok: Boolean,
    val item: OfferItem?
)

// ---------- QUEUE ----------
data class QueueItem(
    val offer_id: Long,
    val ride_id: Long,
    val queued_at: String?,
    val queued_position: Int?,
    val queued_reason: String?,

    // para UI
    val passenger_name: String?,
    val pax: Int?,
    val origin_lat: Double?,
    val origin_lng: Double?,
    val dest_lat: Double?,
    val dest_lng: Double?,
    val quoted_amount: Double?
)

// ---------- SETTINGS ----------
data class DispatchSettingsRes(
    @SerializedName("auto_dispatch_enabled") val autoDispatchEnabled: Boolean,
    @SerializedName("auto_dispatch_delay_s") val autoDispatchDelayS: Int,
    @SerializedName("auto_dispatch_preview_radius_km") val previewRadiusKm: Double,
    @SerializedName("auto_dispatch_preview_n") val previewN: Int,
    @SerializedName("offer_expires_sec") val offerExpiresSec: Int,
    @SerializedName("auto_assign_if_single") val autoAssignIfSingle: Boolean,
    @SerializedName("allow_fare_bidding") val allowFareBidding: Boolean
)

// ---------- CANCEL REASONS ----------
data class CancelReasonsRes(
    val items: List<String>
)

// ---------- ROUTE ----------
data class RouteRes(
    val ok: Boolean,
    val polyline: String? = null,
    val points: List<List<Double>>? = null   // [[lat,lng],...]
)

// ---------- COMPLETAR STOP ----------
data class CompleteStopRes(
    val ok: Boolean,
    val stop_index: Int?,
    val stops_count: Int?
)
