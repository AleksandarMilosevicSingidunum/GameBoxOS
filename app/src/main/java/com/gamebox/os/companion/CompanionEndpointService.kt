package com.gamebox.os.companion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gamebox.os.GameBoxApplication
import com.gamebox.os.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/** Opt-in LAN listener for a paired Windows companion. */
class CompanionEndpointService : Service() {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var server: ServerSocket? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopListener(); stopSelf() }
            ACTION_START -> { startForeground(NOTIFICATION_ID, notification()); executor.execute(::serve) }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() { stopListener(); executor.shutdownNow(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun serve() {
        val repository = SettingsRepository(applicationContext)
        val (enabled, port, secret) = runBlocking {
            val settings = repository.settings.first()
            Triple(settings.companionEnabled, settings.companionPort, repository.companionPairingSecret())
        }
        if (!enabled || secret.isNullOrBlank()) { stopSelf(); return }
        runCatching {
            ServerSocket().use { listener ->
                server = listener
                listener.reuseAddress = true
                listener.bind(InetSocketAddress(port))
                while (!Thread.currentThread().isInterrupted) listener.accept().use { socket -> handle(socket, secret) }
            }
        }
        server = null
        stopSelf()
    }

    private fun handle(socket: Socket, secret: String) {
        socket.soTimeout = 5_000
        val reader = socket.getInputStream().bufferedReader(Charsets.US_ASCII)
        val parts = reader.readLine()?.trim()?.split(' ') ?: emptyList()
        if (parts.size < 2) return write(socket, 400, """{"error":"bad_request"}""")
        val headers = mutableMapOf<String, String>()
        repeat(32) {
            val line = reader.readLine() ?: return@repeat
            if (line.isBlank()) return@repeat
            val divider = line.indexOf(':')
            if (divider > 0) headers[line.substring(0, divider).trim().lowercase()] = line.substring(divider + 1).trim()
        }
        val authorization = headers[CompanionProtocol.AUTHORIZATION_HEADER.lowercase()]
        val now = System.currentTimeMillis() / 1_000L
        val response = when (parts[1]) {
            CompanionStatusRoute.PATH -> CompanionStatusRoute.handle(
                method = parts[0], path = parts[1], authorization = authorization, pairingSecret = secret,
                deviceName = applicationInfo.loadLabel(packageManager).toString(), nowUnixTimeSeconds = now,
            )
            CompanionLibraryRoute.PATH -> CompanionLibraryRoute.handle(
                method = parts[0], path = parts[1], authorization = authorization, pairingSecret = secret,
                library = (application as GameBoxApplication).container.gameRepository.observeGames().value.map { game ->
                    CompanionLibraryItem(
                        id = game.id.value, title = game.title, platform = game.platform,
                        installState = game.state.name, favorite = game.favorite,
                        minutesPlayed = game.minutesPlayed, savePresent = game.savePresent,
                    )
                },
                nowUnixTimeSeconds = now,
            )
            else -> CompanionHttpResponse(404, """{"error":"not_found"}""")
        }
        write(socket, response.status, response.body)
    }

    private fun write(socket: Socket, status: Int, body: String) {
        val reason = when (status) { 200 -> "OK"; 400 -> "Bad Request"; 401 -> "Unauthorized"; else -> "Not Found" }
        val bytes = body.toByteArray(Charsets.UTF_8)
        socket.getOutputStream().apply {
            write("HTTP/1.1 $status $reason\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray(Charsets.US_ASCII))
            write(bytes)
            flush()
        }
    }

    private fun stopListener() { runCatching { server?.close() }; server = null }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "GameBox companion", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun notification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_upload)
        .setContentTitle("GameBox companion")
        .setContentText("Paired companion is available on your local network.")
        .setOngoing(true)
        .build()

    companion object {
        const val ACTION_START = "com.gamebox.os.action.START_COMPANION"
        const val ACTION_STOP = "com.gamebox.os.action.STOP_COMPANION"
        private const val CHANNEL_ID = "gamebox_companion"
        private const val NOTIFICATION_ID = 2_401
        fun start(context: Context) = context.startForegroundService(Intent(context, CompanionEndpointService::class.java).setAction(ACTION_START))
        fun stop(context: Context) = context.startService(Intent(context, CompanionEndpointService::class.java).setAction(ACTION_STOP))
    }
}
