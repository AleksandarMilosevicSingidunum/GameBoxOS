package com.gamebox.os.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal val SAFE_AREA = floatPreferencesKey("safe_area_percent")
internal val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
internal val UI_SOUND_EFFECTS = booleanPreferencesKey("ui_sound_effects")
internal val FOCUS_DEBUG_OVERLAY = booleanPreferencesKey("focus_debug_overlay")
internal val DEVELOPER_LAYOUT_MODE = stringPreferencesKey("developer_layout_mode")
internal val CATALOG_FAILURE_SIMULATION = stringPreferencesKey("catalog_failure_simulation")
internal val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
internal val SHOW_UNAVAILABLE = booleanPreferencesKey("show_unavailable_games")
internal val SHOW_UNAVAILABLE_SHORTCUTS = booleanPreferencesKey("show_unavailable_shortcuts")
internal val DOWNLOADS_UNMETERED_ONLY = booleanPreferencesKey("downloads_unmetered_only")
internal val CATALOG_SEEDED_AT = longPreferencesKey("catalog_seeded_at_epoch_ms")
internal val CATALOG_REFRESHED_AT = longPreferencesKey("catalog_refreshed_at_epoch_ms")
internal val CATALOG_URL = stringPreferencesKey("catalog_url")
internal val CATALOG_TRANSPORT = stringPreferencesKey("catalog_transport")
internal val CATALOG_BUCKET = stringPreferencesKey("catalog_bucket")
internal val CATALOG_PREFIX = stringPreferencesKey("catalog_prefix")
internal val CATALOG_REGION = stringPreferencesKey("catalog_region")
internal val GAME_SOURCES = stringPreferencesKey("game_sources")
internal val EXTERNAL_LIBRARY_URI = stringPreferencesKey("external_library_uri")
internal val EMULATOR_SAVE_ROOT_URI = stringPreferencesKey("emulator_save_root_uri")
internal val CLOUD_SAVE_PROVIDER = stringPreferencesKey("cloud_save_provider")
internal val CLOUD_SAVE_ENDPOINT = stringPreferencesKey("cloud_save_endpoint")
internal val CLOUD_SAVE_REGION = stringPreferencesKey("cloud_save_region")
internal val COMPANION_ENABLED = booleanPreferencesKey("companion_enabled")
internal val COMPANION_PORT = intPreferencesKey("companion_port")
internal val MOONLIGHT_HOST = stringPreferencesKey("moonlight_host")
internal val MOONLIGHT_PORT = intPreferencesKey("moonlight_port")
internal val PLATFORM_EMULATOR_DEFAULTS = stringPreferencesKey("platform_emulator_defaults")
internal val RECENT_SHORTCUT_LAUNCHES = stringPreferencesKey("recent_shortcut_launches")
internal val HIDDEN_SHORTCUT_PACKAGES = stringPreferencesKey("hidden_shortcut_packages")
