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
                val connection = CompanionHttpConnection()
                while (!Thread.currentThread().isInterrupted) listener.accept().use { socket ->
                    connection.handle(socket) { request -> route(request, secret) }
                }
            }
        }
        server = null
        stopSelf()
    }

    private fun route(request: CompanionHttpRequest, secret: String): CompanionHttpResponse {
        val now = System.currentTimeMillis() / 1_000L
        return when {
            request.path == CompanionStatusRoute.PATH -> CompanionStatusRoute.handle(
                method = request.method, path = request.path, authorization = request.authorization, pairingSecret = secret,
                deviceName = applicationInfo.loadLabel(packageManager).toString(), nowUnixTimeSeconds = now,
            )
            request.path == CompanionLibraryRoute.PATH -> CompanionLibraryRoute.handle(
                method = request.method, path = request.path, authorization = request.authorization, pairingSecret = secret,
                library = (application as GameBoxApplication).container.gameRepository.observeGames().value.map { game ->
                    CompanionLibraryItem(
                        id = game.id.value, title = game.title, platform = game.platform,
                        installState = game.state.name, favorite = game.favorite,
                        minutesPlayed = game.minutesPlayed, savePresent = game.savePresent,
                    )
                },
                nowUnixTimeSeconds = now,
            )
            request.path.startsWith(CompanionSaveRoute.PREFIX) -> runBlocking {
                CompanionSaveRoute.handle(
                    request = request,
                    pairingSecret = secret,
                    store = CompanionSaveTransferStore(
                        applicationContext.filesDir,
                        (application as GameBoxApplication).container.gameRepository,
                    ),
                    nowUnixTimeSeconds = now,
                )
            }
            else -> CompanionHttpResponse(404, """{"error":"not_found"}""")
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
