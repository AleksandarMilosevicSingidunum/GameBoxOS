package com.gamebox.os.data

import android.content.Context
import androidx.room.Room
import com.gamebox.os.data.local.GameBoxDatabase
import com.gamebox.os.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface AppContainer {
    val gameRepository: GameRepository
    val settingsRepository: SettingsRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = Room.databaseBuilder(
        context.applicationContext,
        GameBoxDatabase::class.java,
        "gamebox.db"
    ).build()

    override val gameRepository: GameRepository =
        RoomGameRepository(database.gameDao(), applicationScope)

    override val settingsRepository = SettingsRepository(context.applicationContext)
}
