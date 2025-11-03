package com.example.orbanadrive.network

import java.time.*
import java.time.format.DateTimeFormatter

fun OfferItem.isLive(nowMs: Long = System.currentTimeMillis()): Boolean {
    if (offer_status?.lowercase() != "offered") return false
    val exp = parseServerTs(expires_at) ?: return true
    return exp > nowMs
}

private fun parseServerTs(s: String?): Long? {
    if (s.isNullOrBlank()) return null
    return try { Instant.parse(s).toEpochMilli() } catch (_: Exception) {
        try {
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            LocalDateTime.parse(s, fmt)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) { null }
    }
}
