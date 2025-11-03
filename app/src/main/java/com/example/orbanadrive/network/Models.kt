
package com.example.orbanadrive.network

// ===== Auth / Login =====
data class LoginReq(val email: String, val password: String)
data class LoginRes(
    val token: String,
    val tenant: Long?,       // opcional si tu backend lo devuelve
    val driver: MeDriver?    // opcional
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


// ===== Vehicles / Shift =====


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



data class PromoteRes(val ok: Boolean? = true, val offer_id: Long? = null, val position: Int? = null)





data class FinishRes(val ok: Boolean, val ride_id: Long, val status: String, val promoted: Long?)
