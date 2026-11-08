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
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Opt-in LAN listener for a paired Windows companion. */
class CompanionEndpointService : Service() {
    private val executor = Executors.newSingleThreadExecutor()
    private val remoteExecutor = Executors.newSingleThreadExecutor()
    private val connectionExecutor = ThreadPoolExecutor(
        4, 4, 0L, TimeUnit.MILLISECONDS, ArrayBlockingQueue(8),
    )
    private val discoveryExecutor = Executors.newSingleThreadExecutor()
    private val authorizationReplayGuard = CompanionAuthorizationReplayGuard()
    @Volatile private var server: ServerSocket? = null
    @Volatile private var remoteServer: ServerSocket? = null
    @Volatile private var discoverySocket: DatagramSocket? = null
    @Volatile private var restartRequested = false
    private var listenersStarted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action != ACTION_STOP && !runBlocking {
                SettingsRepository(applicationContext).settings.first().companionEnabled
            }
        ) {
            stopListener()
            stopSelf()
            return START_NOT_STICKY
        }
        when (action) {
            ACTION_STOP -> { listenersStarted = false; stopListener(); stopSelf(); return START_NOT_STICKY }
            ACTION_RESTART -> {
                restartRequested = true
                stopListener()
                listenersStarted = true
                executor.execute { restartRequested = false; serve() }
                remoteExecutor.execute(::serveRemote)
                discoveryExecutor.execute(::advertise)
            }
            ACTION_START -> {
                if (!listenersStarted) {
                    listenersStarted = true
                    startForeground(NOTIFICATION_ID, notification())
                    executor.execute(::serve)
                    remoteExecutor.execute(::serveRemote)
                    discoveryExecutor.execute(::advertise)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopListener()
        executor.shutdown()
        remoteExecutor.shutdown()
        connectionExecutor.shutdown()
        discoveryExecutor.shutdown()
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
            CompanionTlsIdentity.createServerSocket().use { listener ->
                server = listener
                listener.reuseAddress = true
                listener.bind(InetSocketAddress(port))
                val staging = applicationContext.cacheDir.resolve("companion-requests")
                val connection = CompanionHttpConnection(bodyDirectory = staging)
                while (!Thread.currentThread().isInterrupted) {
                    val socket = listener.accept()
                    if (!CompanionPeerPolicy.isLocalNetwork(socket.inetAddress)) {
                        runCatching { socket.close() }
                        continue
                    }
                    try {
                        connectionExecutor.execute {
                            socket.use {
                                val localPeer = CompanionPeerPolicy.isLocalNetwork(it.inetAddress)
                                connection.handle(
                                    socket = it,
                                    authorizeHead = { head ->
                                        if (!localPeer) false else when (head.path) {
                                            CompanionRemotePairRoute.PATH -> if (head.method == "PUT") {
                                                head.contentLength in 1..128
                                            } else head.method == "DELETE" && head.contentLength == 0L
                                            CompanionRemoteRoute.PATH -> validRemoteControlRequest(head)
                                            else -> authorized(
                                                secret, head.method, head.path,
                                                head.authorization, head.declaredBodySha256,
                                            )
                                        }
                                    },
                                ) { request -> route(request, secret, repository) }
                            }
                        }
                    } catch (_: RejectedExecutionException) {
                        runCatching {
                            socket.getOutputStream().write(
                                "HTTP/1.1 503 Service Unavailable\r\nConnection: close\r\nContent-Length: 0\r\n\r\n"
                                    .toByteArray(Charsets.US_ASCII)
                            )
                        }
                        runCatching { socket.close() }
                    }
                }
            }
        }
        server = null
        if (!restartRequested) stopSelf()
    }

    private fun serveRemote() {
        val repository = SettingsRepository(applicationContext)
        val (enabled, port) = runBlocking {
            val settings = repository.settings.first()
            settings.companionEnabled to settings.companionPort
        }
        if (!enabled) return
        runCatching {
            ServerSocket().use { listener ->
                remoteServer = listener
                listener.reuseAddress = true
                listener.bind(InetSocketAddress(remoteControlPort(port)))
                val connection = CompanionHttpConnection()
                while (!Thread.currentThread().isInterrupted) {
                    val socket = listener.accept()
                    if (!CompanionPeerPolicy.isLocalNetwork(socket.inetAddress)) {
                        runCatching { socket.close() }
                        continue
                    }
                    try {
                        connectionExecutor.execute {
                            socket.use {
                                connection.handle(
                                    socket = it,
                                    authorizeHead = { head ->
                                        if (!CompanionPeerPolicy.isLocalNetwork(it.inetAddress)) false else when {
                                            head.path == CompanionRemotePairRoute.PATH && head.method == "PUT" ->
                                                head.contentLength in 1..128
                                            head.path == CompanionRemotePairRoute.PATH && head.method == "DELETE" ->
                                                head.contentLength == 0L
                                            head.path == CompanionRemoteRoute.PATH -> validRemoteControlRequest(head)
                                            else -> false
                                        }
                                    },
                                ) { request -> route(request, "", repository) }
                            }
                        }
                    } catch (_: RejectedExecutionException) {
                        runCatching { socket.close() }
                    }
                }
            }
        }
        remoteServer = null
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
                    if (!CompanionPeerPolicy.isLocalNetwork(request.address)) continue
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

    private fun route(
        request: CompanionHttpRequest,
        secret: String,
        repository: SettingsRepository,
    ): CompanionHttpResponse {
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
            request.path == CompanionRemotePairRoute.PATH -> CompanionRemoteControlRouteLock.withLock {
                if (request.method == "DELETE" && !authorized(
                        runBlocking { repository.remoteControlToken() }, request.method, request.path,
                        request.authorization, request.bodySha256,
                    )) CompanionHttpResponse(401, """{"error":"unauthorized"}""")
                else CompanionRemotePairRoute.handle(
                    request = request,
                    createToken = { runBlocking { repository.createRemoteControlToken() } },
                    revokeToken = { runBlocking { repository.revokeRemoteControlToken() } },
                )
            }
            request.path == CompanionRemoteRoute.PATH -> CompanionRemoteControlRouteLock.withLock {
                CompanionRemoteRoute.handle(
                    request = request,
                    pairingSecret = runBlocking { repository.remoteControlToken() },
                    nowUnixTimeSeconds = now,
                    acceptNonce = { nonce, timestamp ->
                        authorizationReplayGuard.accept(nonce, timestamp, System.currentTimeMillis() / 1_000L)
                    },
                    textEntryAvailable = CompanionRemoteCommandBus::isTextEntryAvailable,
                    publish = CompanionRemoteCommandBus::publish,
                )
            }
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

    private fun authorized(
        signingSecret: String?,
        method: String,
        path: String,
        authorization: String?,
        bodySha256: String?,
    ): Boolean = CompanionProtocol.verifyAuthorization(
        secret = signingSecret.orEmpty(),
        method = method,
        requestPath = path,
        authorization = authorization,
        nowUnixTimeSeconds = System.currentTimeMillis() / 1_000L,
        bodySha256 = bodySha256,
        acceptNonce = { nonce, timestamp ->
            authorizationReplayGuard.accept(nonce, timestamp, System.currentTimeMillis() / 1_000L)
        },
    )

    private fun validRemoteControlRequest(head: CompanionHttpRequestHead): Boolean = when (head.method) {
        "GET" -> head.contentLength == 0L
        "PUT" -> head.contentLength in 1..512
        else -> false
    }

    private fun stopListener() {
        runCatching { server?.close() }
        runCatching { remoteServer?.close() }
        runCatching { discoverySocket?.close() }
        ActiveCompanionRemotePairing.close()
        server = null
        remoteServer = null
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
        fun remoteControlPort(companionPort: Int): Int = if (companionPort == 65_535) 65_534 else companionPort + 1
        const val ACTION_START = "com.gamebox.os.action.START_COMPANION"
        const val ACTION_STOP = "com.gamebox.os.action.STOP_COMPANION"
        private const val ACTION_RESTART = "com.gamebox.os.action.RESTART_COMPANION"
        private const val CHANNEL_ID = "gamebox_companion"
        private const val NOTIFICATION_ID = 2_401
        fun start(context: Context) = context.startForegroundService(Intent(context, CompanionEndpointService::class.java).setAction(ACTION_START))
        fun stop(context: Context) = context.startService(Intent(context, CompanionEndpointService::class.java).setAction(ACTION_STOP))
        fun restart(context: Context) = context.startService(Intent(context, CompanionEndpointService::class.java).setAction(ACTION_RESTART))
    }
}
