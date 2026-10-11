package com.gamebox.os.importer

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId

data class ImportRegistrationRecoveryReport(
    val confirmed: Int = 0,
    val rolledBack: Int = 0,
    val failures: Int = 0,
)

/** Reconciles durable import markers against library registration after process startup. */
class ImportRegistrationRecovery(
    private val journal: ImportRegistrationJournal,
) {
    suspend fun reconcile(
        createdBeforeMillis: Long = Long.MAX_VALUE,
        gameLookup: suspend (GameId) -> Game?,
    ): ImportRegistrationRecoveryReport {
        require(createdBeforeMillis >= 0L) { "Import recovery cutoff is invalid" }
        var confirmed = 0
        var rolledBack = 0
        var failures = 0
        val records = try {
            journal.pending()
        } catch (_: Exception) {
            return ImportRegistrationRecoveryReport(failures = 1)
        }

        records.filter { it.createdAtMillis < createdBeforeMillis }
            .groupBy { it.gameId }
            .toSortedMap()
            .forEach { (gameId, transactions) ->
                if (transactions.size != 1) {
                    failures += transactions.size
                    return@forEach
                }
                val pending = transactions.single()
                runCatching {
                    val game = gameLookup(GameId(gameId))
                    val roomMatches = journal.matchesRegisteredGame(pending, game)
                    when {
                        roomMatches && journal.targetMatchesPending(pending) -> {
                            journal.confirm(pending)
                            confirmed += 1
                        }
                        roomMatches -> throw IllegalStateException(
                            "Registered import bytes do not match the durable journal; keep recovery state for repair"
                        )
                        else -> {
                            journal.rollback(pending)
                            rolledBack += 1
                        }
                    }
                }.onFailure {
                    failures += 1
                }
            }
        return ImportRegistrationRecoveryReport(confirmed, rolledBack, failures)
    }
}
