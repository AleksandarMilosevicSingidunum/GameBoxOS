package com.gamebox.os.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.gamebox.os.launch.LaunchSessionJournal
import com.gamebox.os.launch.RecoveredLaunchSession
import com.gamebox.os.launch.sessionMinutesAway
import java.time.Instant
import java.util.UUID

@Entity(tableName = "pending_launch_session")
data class PendingLaunchSessionEntity(
    @PrimaryKey val slot: Int = 1,
    val ticket: String,
    val gameId: String,
    val startedAt: Long,
    val confirmed: Boolean = false,
)

@Dao
abstract class LaunchSessionDao {
    // ABORT is intentional: never overwrite an unreconciled session.
    @Insert abstract suspend fun begin(session: PendingLaunchSessionEntity)

    @Query("SELECT * FROM pending_launch_session WHERE slot = 1")
    abstract suspend fun pending(): PendingLaunchSessionEntity?

    @Query("UPDATE pending_launch_session SET confirmed = 1 WHERE ticket = :ticket")
    abstract suspend fun confirm(ticket: String): Int

    @Query("DELETE FROM pending_launch_session WHERE ticket = :ticket")
    abstract suspend fun remove(ticket: String)

    @Query("""
        UPDATE games SET lastPlayed = :lastPlayed,
        minutesPlayed = MIN(2147483647, minutesPlayed + :minutes)
        WHERE id = :gameId
    """)
    abstract suspend fun record(gameId: String, lastPlayed: String, minutes: Int): Int

    @Transaction
    open suspend fun finish(endedAt: Long): RecoveredLaunchSession? {
        val session = pending() ?: return null
        val minutes = if (session.confirmed) sessionMinutesAway(session.startedAt, endedAt) else 0
        val recorded = session.confirmed && record(
            session.gameId, Instant.ofEpochMilli(endedAt.coerceAtLeast(session.startedAt)).toString(), minutes
        ) == 1
        remove(session.ticket)
        return RecoveredLaunchSession(session.gameId, recorded, if (recorded) minutes else 0)
    }
}

class RoomLaunchSessionJournal(
    private val dao: LaunchSessionDao,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : LaunchSessionJournal {
    override suspend fun begin(gameId: String): String {
        val ticket = UUID.randomUUID().toString()
        dao.begin(PendingLaunchSessionEntity(ticket = ticket, gameId = gameId, startedAt = nowMillis()))
        return ticket
    }

    override suspend fun confirm(ticket: String) {
        check(dao.confirm(ticket) == 1) { "Pending launch session is missing" }
    }

    override suspend fun abandon(ticket: String) = dao.remove(ticket)
    override suspend fun finish(): RecoveredLaunchSession? = dao.finish(nowMillis())
}
