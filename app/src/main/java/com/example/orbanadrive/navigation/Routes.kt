package com.example.orbanadrive.navigation

object Routes {

    const val Login   = "login"
    const val Vehicle = "vehicle"
    const val Offers  = "offers"

    const val OfferBid = "offer-bid/{offerId}"
    fun offerBid(offerId: Long) = "offer-bid/$offerId"
    const val Ride    = "ride/{rideId}"
}