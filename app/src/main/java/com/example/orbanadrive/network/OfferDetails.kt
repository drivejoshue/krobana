package com.example.orbanadrive.network

data class OfferDetails(
    // --- offer ---
    val offer_id: Long,
    val ride_id: Long,
    val offer_status: String? = null,
    val is_direct: Int? = null,
    val round_no: Int? = null,
    val sent_at: String? = null,
    val responded_at: String? = null,
    val expires_at: String? = null,
    val eta_seconds: Int? = null,
    val distance_m: Int? = null,

    // --- ride ---
    val ride_status: String? = null,
    val requested_channel: String? = null,
    val passenger_name: String? = null,
    val passenger_phone: String? = null,
    val origin_label: String? = null,
    val origin_lat: Double? = null,
    val origin_lng: Double? = null,
    val dest_label: String? = null,
    val dest_lat: Double? = null,
    val dest_lng: Double? = null,
    val pax: Int? = null,
    val ride_distance_m: Int? = null,
    val ride_duration_s: Int? = null,
    val notes: String? = null,

    // --- montos / bidding (de rides) ---
    val total_amount: Double? = null,
    val quoted_amount: Double? = null,
    val allow_bidding: Boolean? = null,
    val passenger_offer: Double? = null,
    val driver_offer: Double? = null,
    val agreed_amount: Double? = null,

    // --- derivados para UI (no DB) ---
    val min_bid: Double? = null,
    val max_bid: Double? = null,
    val suggested_bid: Double? = null
)
