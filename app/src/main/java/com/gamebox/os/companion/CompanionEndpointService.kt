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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.concurrent.Executors

/** Opt-in LAN listener for a paired Windows companion. */
class CompanionEndpointService : Service() {
    private val executor = Executors.newSingleThreadExecutor()
    private val discoveryExecutor = Executors.newSingleThreadExecutor()
    @Volatile private var server: ServerSocket? = null
    @Volatile private var discoverySocket: DatagramSocket? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopListener(); stopSelf() }
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, notification())
                executor.execute(::serve)
                discoveryExecutor.execute(::advertise)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopListener()
        executor.shutdownNow()
        discoveryExecutor.shutdownNow()
        super.onDestroy()
    }
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
                val staging = applicationContext.cacheDir.resolve("companion-requests")
                val connection = CompanionHttpConnection(bodyDirectory = staging)
                while (!Thread.currentThread().isInterrupted) listener.accept().use { socket ->
                    connection.handle(
                        socket = socket,
                        authorizeHead = { head ->
                            CompanionProtocol.verifyAuthorization(
                                secret = secret,
                                method = head.method,
                                requestPath = head.path,
                                authorization = head.authorization,
                                nowUnixTimeSeconds = System.currentTimeMillis() / 1_000L,
                                bodySha256 = head.declaredBodySha256,
                            )
                        },
                    ) { request -> route(request, secret) }
                }
            }
        }
        server = null
        stopSelf()
    }

    private fun advertise() {
        val repository = SettingsRepository(applicationContext)
        val settings = runBlocking { repository.settings.first() }
        val secret = runBlocking { repository.companionPairingSecret() }
        if (!settings.companionEnabled || secret.isNullOrBlank()) return
        runCatching {
            DatagramSocket(null).use { socket ->
                discoverySocket = socket
                socket.reuseAddress = true
                socket.broadcast = true
                socket.soTimeout = 1_000
                socket.bind(InetSocketAddress(CompanionDiscoveryProtocol.PORT))
                val buffer = ByteArray(512)
                while (!Thread.currentThread().isInterrupted) {
                    val request = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(request)
                    } catch (_: SocketTimeoutException) {
                        continue
                    }
                    val nonce = CompanionDiscoveryProtocol.parseRequest(request.data, request.length) ?: continue
                    val response = CompanionDiscoveryProtocol.response(
                        nonce,
                        settings.companionPort,
                        applicationInfo.loadLabel(packageManager).toString(),
                    )
                    socket.send(DatagramPacket(response, response.size, request.address, request.port))
                }
            }
        }
        discoverySocket = null
    }

    private fun route(request: CompanionHttpRequest, secret: String): CompanionHttpResponse {
        val now = System.currentTimeMillis() / 1_000L
        return when {
            request.path == CompanionConfigurationRoute.PATH -> runBlocking {
                CompanionConfigurationRoute.handle(
                    request = request,
                    pairingSecret = secret,
                    store = SettingsCompanionConfigurationStore(SettingsRepository(applicationContext)),
                    nowUnixTimeSeconds = now,
                )
            }
            request.path == CompanionStatusRoute.PATH -> CompanionStatusRoute.handle(
                method = request.method, path = request.path, authorization = request.authorization, pairingSecret = secret,
                deviceName = applicationInfo.loadLabel(packageManager).toString(), nowUnixTimeSeconds = now,
            )
            request.path.startsWith(CompanionLibraryManagementRoute.PREFIX) &&
                request.path != CompanionLibraryRoute.PATH -> CompanionLibraryManagementRoute.handle(
                request = request,
                pairingSecret = secret,
                games = (application as GameBoxApplication).container.gameRepository,
                nowUnixTimeSeconds = now,
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
            request.path.startsWith(CompanionContentRoute.PREFIX) -> runBlocking {
                CompanionContentRoute.handle(
                    request = request,
                    pairingSecret = secret,
                    store = CompanionContentTransferStore(
                        applicationContext.filesDir,
                        (application as GameBoxApplication).container.gameRepository,
                    ),
                    nowUnixTimeSeconds = now,
                )
            }
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

    private fun stopListener() {
        runCatching { server?.close() }
        runCatching { discoverySocket?.close() }
        server = null
        discoverySocket = null
    }

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
