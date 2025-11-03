package com.example.orbanadrive.network

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale

fun OfferItem.isLive(nowMs: Long = System.currentTimeMillis()): Boolean {
    if (!offer_status.equals("offered", true)) return false
    val exp = expires_at?.let { parseIsoToEpochMs(it) } ?: Long.MAX_VALUE
    return nowMs < exp
}

private fun parseIsoToEpochMs(s: String): Long? = try {
    Instant.parse(s).toEpochMilli()
} catch (_: Exception) {
    try { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(s)?.time } catch (_: Exception) { null }
}
