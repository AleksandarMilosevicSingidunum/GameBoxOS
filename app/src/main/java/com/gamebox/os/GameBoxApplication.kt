package com.gamebox.os

import android.app.Application
import com.gamebox.os.data.AppContainer
import com.gamebox.os.data.DefaultAppContainer

class GameBoxApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
