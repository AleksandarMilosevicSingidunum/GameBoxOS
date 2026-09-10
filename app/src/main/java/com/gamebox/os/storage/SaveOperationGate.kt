package com.gamebox.os.storage

import java.util.concurrent.ConcurrentHashMap

/** Shared by controllers so revisiting a game cannot overlap an unfinished write. */
internal object SaveOperationGate {
    private val active = ConcurrentHashMap.newKeySet<String>()
    fun acquire(key: String): Boolean = active.add(key)
    fun release(key: String) { active.remove(key) }
}

