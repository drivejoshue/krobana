package com.example.orbanadrive.services
import android.util.Log

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.orbanadrive.R
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import okhttp3.MediaType.Companion.toMediaType

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class LocationService : Service() {

    companion object {
        const val ACTION_START = "com.example.orbanadrive.LOC_START"
        const val ACTION_STOP = "com.example.orbanadrive.LOC_STOP"
        const val ACTION_TICK = "com.example.orbanadrive.LOC_TICK"
        const val ACTION_STATUS = "com.example.orbanadrive.LOC_STATUS"
        const val ACTION_FORCE_POST = "com.example.orbanadrive.LOC_FORCE_POST"
        @Volatile var isRunning: Boolean = false
        private const val EXTRA_TOKEN   = "extra_token"
        private const val EXTRA_TENANT  = "extra_tenant_id" // opcional


        fun start(ctx: Context, token: String, tenantId: Long? = null) {
            if (!LocationService().hasLocPermission(ctx)) {
                android.util.Log.e("Orbana.Location", "No location permission → no se inicia FGS")
                return
            }
            val i = Intent(ctx, LocationService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TOKEN, token)
                tenantId?.let { putExtra(EXTRA_TENANT, it) }
            }
            if (android.os.Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }
        fun stop(ctx: Context) {
            // Antes: startService(ACTION_STOP) → creaba el service y explotaba
            try { ctx.stopService(Intent(ctx, LocationService::class.java)) } catch (_: Exception) {}
        }
    }

    private val channelId = "orbana_location"
    private lateinit var fused: FusedLocationProviderClient
    private var callback: LocationCallback? = null

    private var job: Job? = null
    private lateinit var okHttp: OkHttpClient
    private var token: String? = null
    private var tenantId: Long? = null
    private var baseUrl: String = ""

    override fun onCreate() {
        super.onCreate()
        createChannel()
        try {
            startForeground(
                1001,
                NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.location)
                    .setContentTitle("Orbana Driver")
                    .setContentText("Enviando ubicación…")
                    .setOngoing(true)
                    .build()
            )
            isRunning = true
            broadcastStatus(true)
        } catch (se: SecurityException) {
            android.util.Log.e("Orbana.Location", "FGS sin permiso", se)
            stopSelf()    // sal con gracia
            return
        }

        fused = LocationServices.getFusedLocationProviderClient(this)

        okHttp = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .build()

        baseUrl = getString(R.string.api_base_url).trim().trimEnd('/')
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                token    = intent.getStringExtra(EXTRA_TOKEN)
                tenantId = if (intent.hasExtra(EXTRA_TENANT)) intent.getLongExtra(EXTRA_TENANT, 0L) else null
                startLocation()
                isRunning = true
                broadcastStatus(true)          // ← AVISA “Activo”
            }
            ACTION_STOP -> stopSelfSafely()
        }
        return START_STICKY
    }


    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "Ubicación", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    private fun startLocation() {
        if (callback != null) return

        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .setWaitForAccurateLocation(false)
            .setMaxUpdates(Int.MAX_VALUE)
            .build()

        callback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                val loc = res.lastLocation ?: return
                val lat = loc.latitude
                val lng = loc.longitude
                val speedKmh = (loc.speed * 3.6)
                val t = token ?: return

                // Envía al backend y notifica a la UI
                job?.cancel()
                job = CoroutineScope(Dispatchers.IO).launch {
                    postLocationAndNotify(t, lat, lng, speedKmh)
                }
            }
        }

        try {
            fused.requestLocationUpdates(req, callback as LocationCallback, mainLooper)
            // Opcional: primer “tick” con lastKnown para que la UI no quede en blanco
            fused.lastLocation.addOnSuccessListener { l ->
                l?.let { broadcastTick(it.latitude, it.longitude, ok = true) }
            }
        } catch (se: SecurityException) {
            broadcastTick(null, null, ok = false, err = "Sin permisos de ubicación")
        }
    }
    private suspend fun postLocationAndNotify(t: String, lat: Double, lng: Double, speedKmh: Double) {
        val url = "$baseUrl/driver/location"
        val json = JSONObject().apply {
            put("lat", lat); put("lng", lng)
            put("speed_kmh", (speedKmh * 10.0).roundToInt() / 10.0)
        }
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder()
            .url(url).post(body)
            .addHeader("Authorization", "Bearer $t")
            .addHeader("Accept", "application/json")
            .apply { tenantId?.let { addHeader("X-Tenant-ID", it.toString()) } }
            .build()

        try {
            okHttp.newCall(req).execute().use { res ->
                val ok = res.isSuccessful
                broadcastTick(lat, lng, ok = ok, err = if (ok) null else "HTTP ${res.code}")
            }
        } catch (e: Exception) {
            broadcastTick(null, null, ok = false, err = e.message)
        }
    }

    private fun broadcastStatus(isRunningNow: Boolean) {
        sendBroadcast(Intent(ACTION_STATUS).putExtra("running", isRunningNow))
    }

    private fun broadcastTick(lat: Double?, lng: Double?, ok: Boolean, err: String? = null) {
        sendBroadcast(Intent(ACTION_TICK).apply {
            putExtra("ok", ok)
            lat?.let { putExtra("lat", it) }
            lng?.let { putExtra("lng", it) }
            err?.let { putExtra("error", it) }
        })
    }
    private fun stopLocation() {
        callback?.let { fused.removeLocationUpdates(it) }
        callback = null
    }
    private fun hasLocPermission(ctx: Context): Boolean {
        val fine = androidx.core.content.ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = androidx.core.content.ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
    private suspend fun postLocation(lat: Double, lng: Double, speedKmh: Double) {
        val t = token ?: return
        val url = "$baseUrl/driver/location"    // tu ruta: /api/driver/location

        val json = JSONObject().apply {
            put("lat", lat)
            put("lng", lng)
            // Envío de velocidad (redondeada)
            put("speed_kmh", (speedKmh * 10.0).roundToInt() / 10.0)
            // "busy" sólo si quieres forzar estado (si no, backend lo preserva)
            // put("busy", false)
        }

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "Bearer $t")
            .addHeader("Accept", "application/json")
            .apply {
                tenantId?.let { addHeader("X-Tenant-ID", it.toString()) }
            }
            .build()

        runCatching { okHttp.newCall(req).execute().use { } }
        // Ignoramos errores transitorios; el siguiente tick lo reintentará.
    }

    private suspend fun postLocationAndReturn(lat: Double, lng: Double, speedKmh: Double): Boolean {
        val t = token ?: return false
        val url = "$baseUrl/driver/location"

        val json = JSONObject().apply {
            put("lat", lat); put("lng", lng)
            put("speed_kmh", (speedKmh * 10.0).roundToInt() / 10.0)
        }
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder()
            .url(url).post(body)
            .addHeader("Authorization", "Bearer $t")
            .addHeader("Accept", "application/json")
            .apply { tenantId?.let { addHeader("X-Tenant-ID", it.toString()) } }
            .build()

        return runCatching {
            okHttp.newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    private fun stopSelfSafely() {
        stopLocation()
        isRunning = false
        broadcastStatus(false)               // ← AVISA “Detenido”
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopLocation()
        isRunning = false
        broadcastStatus(false)               // ← Por si acaso
        super.onDestroy()
    }


    override fun onBind(intent: Intent?): IBinder? = null
}
