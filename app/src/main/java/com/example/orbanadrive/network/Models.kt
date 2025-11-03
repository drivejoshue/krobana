
package com.example.orbanadrive.network

// ===== Auth / Login =====
data class LoginReq(val email: String, val password: String)
data class LoginRes(
    val token: String,
    val tenant: Long?,       // opcional si tu backend lo devuelve
    val driver: MeDriver?    // opcional
)


data class DispatchSettingsRes(
    val auto_dispatch_enabled: Boolean? = null,
    val auto_dispatch_delay_s: Int? = null,
    val auto_dispatch_preview_radius_km: Double? = null,
    val auto_dispatch_preview_n: Int? = null,
    val offer_expires_sec: Int? = null,
    val auto_assign_if_single: Boolean? = null,
    val allow_fare_bidding: Boolean? = null
)
// ===== /auth/me =====
data class MeUser(
    val id: Long,
    val name: String?,
    val email: String?,
    val tenant_id: Long?     // 👈 AQUÍ VIENE EL TENANT
)
data class MeDriver(
    val id: Long,
    val name: String?,
    val phone: String?,
    val status: String? = null
)
data class MeShift(
    val id: Long,
    val status: String?,
    val started_at: String?,
    val vehicle_id: Long?
    // (no trae tenant_id ni driver_id aquí según tu comentario)
)
data class MeRes(
    val ok: Boolean,
    val user: MeUser,
    val driver: MeDriver?,
    val current_shift: MeShift?,
    val vehicle: VehicleItem?
)

// ===== Vehicles / Shift =====
data class VehicleItem(
    val id: Long,
    val economico: String? = null,
    val plate: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val type: String? = null
)
data class VehiclesRes(val ok: Boolean, val items: List<VehicleItem>)
data class ShiftStartReq(val vehicle_id: Long?)
data class ShiftStartRes(val ok: Boolean, val shift_id: Long?)

// ===== Location =====
data class LocationReq(
    val lat: Double,
    val lng: Double,
    val busy: Boolean? = null,
    val speed_kmh: Double? = null
)

// ===== Ofertas / Cola / Ride activo =====
data class OffersRes(
    val ok: Boolean,
    val count: Int,
    val items: List<OfferItem>
)
data class StopDto(
    val lat: Double?,
    val lng: Double?,
    val label: String?
)

/** Campos reales del SELECT en OfferController@index */
data class OfferItem(
    // ---- offer ----
    val offer_id: Long,
    val offer_status: String,        // "offered" | "accepted" | ...
    val sent_at: String?,            // texto, puede ser "YYYY-MM-DD HH:mm:ss"
    val responded_at: String?,
    val expires_at: String?,
    val eta_seconds: Int?,           // o null si no viene
    val distance_m: Int?,            // o null
    val round_no: Int?,
    val is_direct: Int?,             // 0/1 (si viene raro, normalizamos abajo)

    // ---- ride ----
    val ride_id: Long,
    val ride_status: String?,        // opcional
    val origin_label: String?,
    val origin_lat: Double?, val origin_lng: Double?,
    val dest_label: String?,
    val dest_lat: Double?,   val dest_lng: Double?,
    val quoted_amount: Double?,
    val ride_distance_m: Int?,
    val ride_duration_s: Int?,
    val passenger_name: String?,
    val passenger_phone: String?,
    val requested_channel: String?,  // "dispatch" -> ocultar bidding
    val pax: Int?,

    // ---- stops ----
    val stops_count: Int?, val stop_index: Int?,
    val stops: List<StopDto>?,

    // ---- extras ----
    val notes: String?
)

data class AcceptRes(val ok: Boolean, val mode: String, val ride_id: Long?)
data class OkRes(val ok: Boolean)
data class PromoteRes(val ok: Boolean? = true, val offer_id: Long? = null, val position: Int? = null)

data class QueueItem(
    val offer_id: Long,
    val ride_id: Long,
    val queued_at: String?,
    val queued_position: Int?,
    val queued_reason: String?
)

data class ActiveRideRes(val ok: Boolean, val item: ActiveRide?)
data class ActiveRide(
    val ride_id: Long,
    val ride_status: String,
    val offer_id: Long?,
    val origin_lat: Double?, val origin_lng: Double?,
    val dest_lat: Double?,   val dest_lng: Double?,
    val route_polyline: String?,
    val stops_count: Int?, val stop_index: Int?, val stops: List<StopDto>?
)
data class FinishRes(val ok: Boolean, val ride_id: Long, val status: String, val promoted: Long?)
