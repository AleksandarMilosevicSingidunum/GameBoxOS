package com.gamebox.os.companion

import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal sealed interface CompanionRemoteCommand {
    data class Key(val keyCode: Int) : CompanionRemoteCommand
    data object Back : CompanionRemoteCommand
    data class Text(val value: String) : CompanionRemoteCommand
    data object Home : CompanionRemoteCommand
    data object GameBox : CompanionRemoteCommand
    data object Backspace : CompanionRemoteCommand
}

internal object CompanionRemoteCommandBus {
    private data class Target(
        val externalDisplay: Boolean,
        val priority: Int,
        val mutableCommands: MutableSharedFlow<CompanionRemoteCommand>,
        var resumed: Boolean = false,
        var textEntryAvailable: Boolean = false,
    )

    private val lock = Any()
    private val targets = mutableMapOf<Any, Target>()

    fun register(
        owner: Any,
        externalDisplay: Boolean,
        commands: MutableSharedFlow<CompanionRemoteCommand>,
        priority: Int = 0,
    ) {
        synchronized(lock) { targets[owner] = Target(externalDisplay, priority, commands) }
    }

    fun unregister(owner: Any) {
        synchronized(lock) { targets.remove(owner) }
    }

    fun setHostState(owner: Any, resumed: Boolean, textEntryAvailable: Boolean) {
        synchronized(lock) {
            targets[owner]?.let {
                it.resumed = resumed
                it.textEntryAvailable = textEntryAvailable
            }
        }
    }

    fun isTextEntryAvailable(): Boolean = synchronized(lock) {
        activeTarget()?.textEntryAvailable == true
    }

    fun publish(command: CompanionRemoteCommand): Boolean = synchronized(lock) {
        val target = activeTarget() ?: return false
        if ((command is CompanionRemoteCommand.Text || command == CompanionRemoteCommand.Backspace) &&
            !target.textEntryAvailable
        ) return false
        target.mutableCommands.subscriptionCount.value > 0 && target.mutableCommands.tryEmit(command)
    }

    private fun activeTarget(): Target? = targets.values
        .asSequence()
        .filter(Target::resumed)
        .maxWithOrNull(compareBy<Target>({ it.externalDisplay }, Target::priority))
}

internal object CompanionRemoteRoute {
    const val PATH = "/v1/remote"
    private const val MAX_TEXT_LENGTH = 128
    private val json = Json { ignoreUnknownKeys = false }
    private val keys = mapOf(
        "up" to KeyEvent.KEYCODE_DPAD_UP,
        "down" to KeyEvent.KEYCODE_DPAD_DOWN,
        "left" to KeyEvent.KEYCODE_DPAD_LEFT,
        "right" to KeyEvent.KEYCODE_DPAD_RIGHT,
        "select" to KeyEvent.KEYCODE_DPAD_CENTER,
        "play_pause" to KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
    )

    fun handle(
        request: CompanionHttpRequest,
        pairingSecret: String?,
        nowUnixTimeSeconds: Long,
        acceptNonce: (String, Long) -> Boolean,
        textEntryAvailable: () -> Boolean = { true },
        publish: (CompanionRemoteCommand) -> Boolean,
    ): CompanionHttpResponse {
        if (request.path != PATH) {
            return CompanionHttpResponse(404, """{"error":"not_found"}""")
        }
        if (request.method == "GET" && request.bodyLength == 0L) {
            if (pairingSecret.isNullOrBlank() || !CompanionProtocol.verifyAuthorization(
                    pairingSecret, request.method, request.path, request.authorization, nowUnixTimeSeconds,
                    acceptNonce = acceptNonce,
                )) return CompanionHttpResponse(401, """{"error":"unauthorized"}""")
            return CompanionHttpResponse(200, """{"status":"ready"}""")
        }
        if (request.method != "PUT" || request.bodyLength !in 1..512) {
            return CompanionHttpResponse(404, """{"error":"not_found"}""")
        }
        if (pairingSecret.isNullOrBlank() || !CompanionProtocol.verifyAuthorization(
                pairingSecret, request.method, request.path, request.authorization,
                nowUnixTimeSeconds, bodySha256 = request.bodySha256, acceptNonce = acceptNonce,
            )) {
            return CompanionHttpResponse(401, """{"error":"unauthorized"}""")
        }
        val command = runCatching {
            val obj = json.parseToJsonElement(request.body.toString(Charsets.UTF_8)).jsonObject
            require(obj.keys.all { it in setOf("action", "text") })
            val action = obj["action"]?.jsonPrimitive?.contentOrNull ?: error("action required")
            when (action) {
                "back" -> CompanionRemoteCommand.Back
                "home" -> CompanionRemoteCommand.Home
                "gamebox" -> CompanionRemoteCommand.GameBox
                "backspace" -> CompanionRemoteCommand.Backspace
                "text" -> {
                    val value = obj["text"]?.jsonPrimitive?.contentOrNull
                        ?: error("text required")
                    require(value.isNotEmpty() && value.length <= MAX_TEXT_LENGTH && value.none(Char::isISOControl))
                    CompanionRemoteCommand.Text(value)
                }
                else -> CompanionRemoteCommand.Key(keys[action] ?: error("unsupported action"))
            }
        }.getOrNull() ?: return CompanionHttpResponse(400, """{"error":"invalid_command"}""")
        if ((command is CompanionRemoteCommand.Text || command == CompanionRemoteCommand.Backspace) && !textEntryAvailable()) {
            return CompanionHttpResponse(409, """{"error":"search_not_active","message":"Open a GameBox search field before editing text."}""")
        }
        return if (publish(command)) CompanionHttpResponse(200, """{"accepted":true}""")
        else CompanionHttpResponse(503, """{"error":"gamebox_unavailable"}""")
    }
}
