package com.example.orbanadrive.network

import retrofit2.http.*

interface DriverApi {

    // === Ofertas ===
    @GET("driver/offers")
    suspend fun listOffers(@Query("status") status: String? = null): OffersRes

    @GET("driver/offers/{id}")
    suspend fun getOffer(@Path("id") offerId: Long): OfferDetails

    @POST("driver/offers/{id}/accept")
    suspend fun acceptOffer(
        @Path("id") offerId: Long,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): AcceptRes

    @POST("driver/offers/{id}/reject")
    suspend fun rejectOffer(@Path("id") offerId: Long): OkRes

    // === Cola ===
    @GET("driver/queue")
    suspend fun queue(): List<QueueItem>

    @POST("driver/queue/promote")
    suspend fun queuePromote(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): PromoteRes

    @DELETE("driver/queue/{offerId}")
    suspend fun queueDrop(@Path("offerId") offerId: Long): OkRes

    @POST("driver/queue/clear")
    suspend fun queueClear(): OkRes

    // === Ride activo / finish ===
    @GET("driver/ride/active")
    suspend fun activeRide(): ActiveRideRes

    @POST("driver/rides/{id}/finish")
    suspend fun finishRide(@Path("id") rideId: Long): FinishRes

    // === Vehículos / Turno ===
    @GET("driver/vehicles")
    suspend fun vehicles(): VehiclesRes

    @POST("driver/shifts/start")
    suspend fun startShift(@Body req: ShiftStartReq): ShiftStartRes

    // === Ubicación / busy ===
    @POST("driver/location")
    suspend fun updateLocation(@Body req: LocationReq): OkRes

    // Variante genérica que usas en setBusy()
    @POST("driver/location")
    suspend fun postLocation(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): OkRes

    // === Settings dispatch ===
    @GET("driver/dispatch/settings")
    suspend fun dispatchSettings(): DispatchSettingsRes
}
