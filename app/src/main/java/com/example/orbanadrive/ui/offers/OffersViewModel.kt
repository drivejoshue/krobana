package com.example.orbanadrive.ui.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbanadrive.network.*
import com.example.orbanadrive.repo.DriverRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OfferPopupModel(
    val offerId: Long,
    val rideId: Long,
    val direct: Boolean,
    val requestedChannel: String?,
    val quotedAmount: Double?,
    val showBidding: Boolean
)

class OffersViewModel(
    private val repo: DriverRepository
) : ViewModel() {

    private val moshi = Moshi.Builder().build()

    // === Estado UI ===
    private val _offers = MutableStateFlow<List<OfferItem>>(emptyList())
    val offers: StateFlow<List<OfferItem>> = _offers.asStateFlow()

    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()

    private val _popup = MutableStateFlow<OfferPopupModel?>(null)
    val popup: StateFlow<OfferPopupModel?> = _popup.asStateFlow()

    // Disponible (true) / Ocupado (false)
    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val _allowFareBidding = MutableStateFlow(true) // fallback true
    val allowFareBidding: StateFlow<Boolean> = _allowFareBidding.asStateFlow()

    fun refreshAll() {
        viewModelScope.launch {
            runCatching { repo.getOffers(null) }.onSuccess { _offers.emit(it.items) }
            runCatching { repo.getQueue()       }.onSuccess { _queue.emit(it) }
        }
    }

    fun loadSettingsOnce() {
        if (_allowFareBidding.value != true) return
        viewModelScope.launch {
            runCatching { repo.dispatchSettings() }.onSuccess {
                _allowFareBidding.value = it.allowFareBidding ?: true
            }
        }
    }
    fun onSocketEvent(name: String, json: String) {
        viewModelScope.launch {
            when (name) {
                "offers.new" -> handleOfferNew(json)
                "offers.update","queue.add","queue.remove","queue.clear",
                "ride.active","ride.update","ride.promoted","ride.finished" -> refreshAll()
            }
        }
    }

    private fun handleOfferNew(json: String) {
        val map = moshi.adapter(Map::class.java).fromJson(json) as? Map<*, *> ?: return
        val offerId = (map["offer_id"] as? Number)?.toLong() ?: return
        val rideId  = (map["ride_id"]  as? Number)?.toLong() ?: return
        val isDirect  = ((map["is_direct"] as? Number)?.toInt() ?: 0) == 1
        val reqChan   = map["requested_channel"] as? String
        val showBid   = (map["ui"] as? Map<*, *>)?.get("show_bidding") as? Boolean
            ?: (reqChan == "passenger_app")
        val quoted    = (map["quote_amount"] as? Number)?.toDouble()
        val popupOk   = (map["ui"] as? Map<*, *>)?.get("popup_allowed") as? Boolean ?: isDirect

        // refresca listas
        refreshAll()
        if (popupOk) {
            _popup.value = OfferPopupModel(
                offerId = offerId,
                rideId  = rideId,
                direct  = isDirect,
                requestedChannel = reqChan,
                quotedAmount     = quoted,
                showBidding      = showBid
            )
        }
    }

    fun dismissPopup() { _popup.value = null }

    suspend fun accept(offerId: Long, bid: Double?) {
        runCatching { repo.accept(offerId, bid) }
        refreshAll()
        _popup.value = null
    }
    suspend fun reject(offerId: Long) {
        runCatching { repo.reject(offerId) }
        refreshAll()
        _popup.value = null
    }


    private fun handleOfferUpdate(json: String) {
        val map = moshi.adapter(Map::class.java).fromJson(json) as? Map<*, *> ?: return
        val offerId = (map["offer_id"] as? Number)?.toLong() ?: return
        val status  = map["status"] as? String ?: return

        // Si ya no está "offered", sáquela de la lista
        if (status != "offered") {
            _offers.value = _offers.value.filterNot { it.offer_id == offerId }
        } else {
            refreshAll()
        }
    }

    // === ARREGLA TU ERROR: que retorne Boolean ===
    suspend fun setBusy(busy: Boolean, lat: Double?, lng: Double?): Boolean {
        val ok = repo.setBusy(busy, lat, lng)
        if (ok) _available.value = !busy
        return ok
    }
}
