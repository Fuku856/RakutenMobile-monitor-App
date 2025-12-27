package com.example.rakutenmonitor.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Keys
    fun getLastUpdated(): String {
        return prefs.getString(KEY_LAST_UPDATED, "") ?: ""
    }

    // Theme Logic
    enum class ThemeMode {
        SYSTEM, LIGHT, DARK
    }

    companion object {
        private const val KEY_USAGE = "key_usage"
        private const val KEY_LAST_UPDATED = "key_last_updated"
        private const val KEY_THEME_MODE = "key_theme_mode"
    }

    // Usage State
    private val _usageFlow = MutableStateFlow(getUsage())
    val usageFlow: StateFlow<Float> = _usageFlow.asStateFlow()

    private val _lastUpdatedFlow = MutableStateFlow(getLastUpdated())
    val lastUpdatedFlow: StateFlow<String> = _lastUpdatedFlow.asStateFlow()

    fun saveUsage(usage: Float) {
        prefs.edit { putFloat(KEY_USAGE, usage) }
        _usageFlow.value = usage
    }

    fun getUsage(): Float {
        return prefs.getFloat(KEY_USAGE, 0.0f)
    }

    fun saveLastUpdated(timestamp: String) {
        prefs.edit { putString(KEY_LAST_UPDATED, timestamp) }
        _lastUpdatedFlow.value = timestamp
    }

    private val _themeModeFlow = MutableStateFlow(getThemeMode())
    val themeModeFlow: StateFlow<ThemeMode> = _themeModeFlow.asStateFlow()

    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
        _themeModeFlow.value = mode
    }

    fun getThemeMode(): ThemeMode {
        val modeName = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(modeName ?: ThemeMode.SYSTEM.name)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }
}
