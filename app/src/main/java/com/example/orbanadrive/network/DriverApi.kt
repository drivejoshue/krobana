package com.example.orbanadrive.network

import retrofit2.http.*

interface DriverApi {

    // Vehículos
    @GET("/api/driver/vehicles")
    suspend fun vehicles(): VehiclesRes

    // Turnos
    @POST("/api/driver/shifts/start")
    suspend fun startShift(@Body body: ShiftStartReq): StartShiftRes

    @POST("/api/driver/shifts/finish")
    suspend fun finishShift(@Body body: ShiftFinishReq? = null): OkRes

    data class ShiftStartReq(val vehicle_id: Long?)
    data class StartShiftRes(val ok: Boolean, val shift_id: Long)
    data class ShiftFinishReq(val shift_id: Long?)

    // Ubicación / busy
    @POST("/api/driver/location")
    suspend fun updateLocation(@Body body: LocationReq): OkRes

    @POST("/api/driver/location")
    suspend fun postLocation(@Body body: Map<String, @JvmSuppressWildcards Any?>): OkRes

    data class LocationReq(
        val lat: Double,
        val lng: Double,
        val busy: Boolean? = null,
        val speed_kmh: Double? = null
    )

    // Ofertas
    @GET("/api/driver/offers")
    suspend fun listOffers(@Query("status") status: String? = null): OfferListRes

    // IMPORTANTE: esta ruta debe existir así en backend (ver fixes más abajo)
    @GET("/api/driver/offers/{offer}")
    suspend fun getOffer(@Path("offer") offerId: Long): OfferShowRes

    @POST("/api/driver/offers/{offer}/accept")
    suspend fun acceptOffer(
        @Path("offer") offerId: Long,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): AcceptRes

    @POST("/api/driver/offers/{offer}/reject")
    suspend fun rejectOffer(@Path("offer") offerId: Long): OkRes

    // Ride activo / finish / cancelar / stop
    @GET("/api/driver/rides/active")
    suspend fun activeRide(): ActiveRideRes

    @POST("/api/driver/rides/{ride}/finish")
    suspend fun finishRide(@Path("ride") rideId: Long): OkRes

    @POST("/api/driver/rides/{ride}/cancel")
    suspend fun cancelRide(@Path("ride") rideId: Long, @Body body: CancelReq): OkRes
    data class CancelReq(val reason: String?)

    @POST("/api/driver/rides/{ride}/complete-stop")
    suspend fun completeStop(@Path("ride") rideId: Long): CompleteStopRes

    // Cola
    @GET("/api/driver/queue")
    suspend fun queue(): List<QueueItem>

    @POST("/api/driver/queue/promote")
    suspend fun queuePromote(@Body body: Map<String, @JvmSuppressWildcards Any?>): OkRes

    @DELETE("/api/driver/queue/{offer}")
    suspend fun queueDrop(@Path("offer") offerId: Long): OkRes

    @DELETE("/api/driver/queue")
    suspend fun queueClear(): OkRes

    // Route (driver scope)
    @POST("/api/driver/geo/route")
    suspend fun geoRoute(@Body body: RouteReq): RouteRes
    data class RouteReq(
        val from: LatLng,
        val to: LatLng,
        val mode: String = "driving"
    )
    data class LatLng(val lat: Double, val lng: Double)

    // Settings
    @GET("/api/dispatch/settings")
    suspend fun dispatchSettings(): DispatchSettingsRes

    // Cancel reasons (si la tienes)
    @GET("/api/driver/cancel-reasons")
    suspend fun cancelReasons(): CancelReasonsRes
}
