package com.example.orbanadrive.realtime

import android.content.Context
import com.example.orbanadrive.BuildConfig
import com.example.orbanadrive.storage.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.PrivateChannel
import com.pusher.client.channel.PrivateChannelEventListener
import com.pusher.client.channel.PusherEvent
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange



/**
 * Servicio WS (Laravel Reverb compatible con Pusher)
 * Canal: private-tenant.{tenantId}.driver.{driverId}
 */
class OfferSocketService(
    private val context: Context,
    private val tokenStore: TokenStore,
    private val tenantId: Long,
    private val driverId: Long,
    private val onEvent: (String, String) -> Unit,
    private val onState: (String) -> Unit = {}
) {
    private val http = OkHttpClient()

    /** https://host/api/  ->  https://host */
    private val rootUrl: String by lazy {
        val u = BuildConfig.API_BASE_URL.trimEnd('/')
        if (u.endsWith("/api")) u.dropLast(4) else u
    }

    private fun authHeader(): String? =
        runBlocking { tokenStore.getToken() }?.let { "Bearer $it" }

    private fun tenantHeader(): String? =
        runBlocking { tokenStore.getTenantId() }

    private fun authorize(channelName: String, socketId: String): String {
        val form = "channel_name=$channelName&socket_id=$socketId"
            .toRequestBody("application/x-www-form-urlencoded".toMediaType())

        val req = Request.Builder()
            .url("$rootUrl/broadcasting/auth")
            .post(form)
            .apply {
                authHeader()?.let { addHeader("Authorization", it) }
                tenantHeader()?.let { addHeader("X-Tenant-ID", it) }
                addHeader("Accept", "application/json")
            }
            .build()

        http.newCall(req).execute().use { resp ->
            return resp.body?.string().orEmpty()   // {"auth":"key:signature", ...}
        }
    }

    private val options = PusherOptions().apply {
        setHost(BuildConfig.REVERB_HOST)
        setWsPort(BuildConfig.REVERB_PORT)
        setWssPort(BuildConfig.REVERB_PORT)
        setUseTLS(BuildConfig.REVERB_TLS)
        setCluster(null) // Reverb no usa cluster
        setAuthorizer { channel, socketId -> authorize(channel, socketId) }
    }

    private val pusher = Pusher(BuildConfig.REVERB_KEY, options)
    private var channel: PrivateChannel? = null

    fun connect() {
        pusher.connect(object : ConnectionEventListener {
            override fun onConnectionStateChange(change: ConnectionStateChange) {
                onState(change.currentState.name) // CONNECTED / DISCONNECTED / etc.
            }
            override fun onError(message: String?, code: String?, e: Exception?) {
                onState("error:${code ?: ""}:${message ?: e?.message ?: ""}")
            }
        }, ConnectionState.ALL)

        val chName = "private-tenant.$tenantId.driver.$driverId"
        channel = pusher.subscribePrivate(
            chName,
            object : PrivateChannelEventListener {
                override fun onEvent(event: PusherEvent?) {
                    event?.let { onEvent(it.eventName, it.data) }
                }
                override fun onSubscriptionSucceeded(channelName: String?) {
                    onState("subscribed:$channelName")
                }
                override fun onAuthenticationFailure(message: String?, e: Exception?) {
                    onState("auth-failed:${message ?: e?.message ?: ""}")
                }
            },
            // eventos
            "offers.new", "offers.update",
            "queue.add", "queue.remove", "queue.clear",
            "ride.active", "ride.update", "ride.promoted", "ride.finished"
        )
    }

    fun disconnect() {
        try { channel?.let { pusher.unsubscribe(it.name) } } catch (_: Exception) {}
        pusher.disconnect()
    }



}
