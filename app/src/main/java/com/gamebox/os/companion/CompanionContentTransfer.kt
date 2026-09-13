package com.gamebox.os.companion

import com.gamebox.os.data.GameRepository
import com.gamebox.os.data.ImportedGameRegistration
import com.gamebox.os.domain.GameId
import com.gamebox.os.importer.RomHasher
import com.gamebox.os.importer.RomImportPolicy
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64

internal data class CompanionContentImportResult(
    val gameId: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
)

internal class CompanionContentTransferStore(
    private val filesDir: File,
    private val games: GameRepository,
) {
    suspend fun import(
        gameIdValue: String,
        encodedFileName: String?,
        stagedBody: File?,
        expectedLength: Long,
        expectedSha256: String?,
    ): CompanionContentImportResult {
        val id = GameId(gameIdValue)
        val game = requireNotNull(games.game(id)) { "Game is not present in the paired library" }
        val fileName = decodeFileName(encodedFileName)
        val safeName = RomImportPolicy.safeFileName(fileName, game.platform)
        val source = requireNotNull(stagedBody) { "Staged content is unavailable" }
        require(expectedLength > 0 && source.isFile && source.length() == expectedLength) {
            "Transferred content length does not match"
        }
        val declared = requireNotNull(expectedSha256)?.lowercase()
        require(declared.matches(Regex("^[a-f0-9]{64}$"))) { "Transferred content checksum is invalid" }

        val storedRelative = RomImportPolicy.relativePath(id, safeName, game.platform)
        val destination = filesDir.resolve(storedRelative)
        require(destination.canonicalPath.startsWith(filesDir.resolve("imports").canonicalPath + File.separator)) {
            "Content destination is unsafe"
        }
        require(destination.parentFile?.mkdirs() != false) { "Content destination is unavailable" }
        val partial = File(destination.parentFile, destination.name + ".companion-partial")
        val backup = File(destination.parentFile, destination.name + ".companion-backup")
        partial.delete()
        backup.delete()
        try {
            val hashes = source.inputStream().buffered().use { input ->
                partial.outputStream().buffered().use { output ->
                    RomHasher.hash(input, onChunk = { buffer, count -> output.write(buffer, 0, count) })
                }
            }
            require(hashes.sizeBytes == expectedLength && hashes.sha256 == declared) {
                "Transferred content checksum does not match"
            }
            if (destination.exists()) move(destination, backup)
            move(partial, destination)
            try {
                games.registerImportedGame(
                    ImportedGameRegistration(
                        id = id,
                        title = game.title,
                        platform = game.platform,
                        year = game.year,
                        sizeBytes = hashes.sizeBytes,
                        relativePath = RomImportPolicy.importRootRelativePath(id, storedRelative),
                        sha256 = hashes.sha256,
                        mimeType = RomImportPolicy.mimeType(safeName),
                        favorite = game.favorite,
                        artworkUrl = game.artworkUrl,
                        description = game.description,
                        players = game.players,
                        region = game.region,
                    )
                )
            } catch (failure: Throwable) {
                destination.delete()
                if (backup.exists()) move(backup, destination)
                throw failure
            }
            backup.delete()
            return CompanionContentImportResult(id.value, safeName, hashes.sizeBytes, hashes.sha256)
        } finally {
            partial.delete()
        }
    }

    private fun decodeFileName(value: String?): String {
        require(!value.isNullOrBlank() && value.length <= 512) { "Content filename is required" }
        val bytes = runCatching { Base64.getDecoder().decode(value) }
            .getOrElse { throw IllegalArgumentException("Content filename is invalid") }
        require(bytes.size in 1..360) { "Content filename is invalid" }
        return bytes.toString(Charsets.UTF_8)
    }

    private fun move(source: File, destination: File) {
        runCatching {
            Files.move(
                source.toPath(), destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING,
            )
        }.getOrElse {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

internal object CompanionContentRoute {
    const val PREFIX = "/v1/content/"

    suspend fun handle(
        request: CompanionHttpRequest,
        pairingSecret: String?,
        store: CompanionContentTransferStore,
        nowUnixTimeSeconds: Long,
    ): CompanionHttpResponse {
        val gameId = request.path.removePrefix(PREFIX)
        if (!request.path.startsWith(PREFIX) || gameId.contains('/') ||
            !gameId.matches(Regex("[a-z0-9][a-z0-9-]{0,95}"))) {
            return CompanionHttpResponse(404, """{"error":"not_found"}""")
        }
        if (request.method != "PUT") {
            return CompanionHttpResponse(400, """{"error":"method_not_supported"}""")
        }
        if (pairingSecret == null || !CompanionProtocol.verifyAuthorization(
                pairingSecret, request.method, request.path, request.authorization,
                nowUnixTimeSeconds, bodySha256 = request.bodySha256,
            )) {
            return CompanionHttpResponse(401, """{"error":"unauthorized"}""")
        }
        return runCatching {
            store.import(
                gameId, request.fileName, request.bodyFile,
                request.bodyLength, request.bodySha256,
            )
        }.fold(
            onSuccess = { result ->
                CompanionHttpResponse(
                    200,
                    "{\"protocolVersion\":" + CompanionProtocol.VERSION +
                        ",\"gameId\":\"" + result.gameId +
                        "\",\"fileName\":\"" + escape(result.fileName) +
                        "\",\"sizeBytes\":" + result.sizeBytes +
                        ",\"sha256\":\"" + result.sha256 + "\"}",
                )
            },
            onFailure = { CompanionHttpResponse(400, """{"error":"content_rejected"}""") },
        )
    }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}
