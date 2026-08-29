package com.gamebox.os.launch

enum class ShortcutAvailability { INSTALLED, NOT_INSTALLED, LAUNCH_REJECTED }

data class ShortcutStatus(val packageName: String, val availability: ShortcutAvailability, val message: String? = null)