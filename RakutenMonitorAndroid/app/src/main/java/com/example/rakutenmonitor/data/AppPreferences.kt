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
