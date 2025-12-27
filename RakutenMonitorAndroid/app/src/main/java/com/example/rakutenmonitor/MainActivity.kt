package com.example.rakutenmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.example.rakutenmonitor.data.AppPreferences
import com.example.rakutenmonitor.ui.RakutenApp
import com.example.rakutenmonitor.ui.theme.RakutenMonitorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = baseContext
            val appPreferences = AppPreferences(context)
            val themeMode = appPreferences.themeModeFlow.collectAsState().value

            val darkTheme = when (themeMode) {
                AppPreferences.ThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppPreferences.ThemeMode.LIGHT -> false
                AppPreferences.ThemeMode.DARK -> true
            }

            RakutenMonitorTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RakutenApp()
                }
            }
        }
    }
}
