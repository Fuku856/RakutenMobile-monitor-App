package com.example.rakutenmonitor.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.rakutenmonitor.R

class RakutenWidget : GlanceAppWidget() {

    companion object {
        val usageKey = floatPreferencesKey("usage")
        val lastUpdatedKey = stringPreferencesKey("last_updated")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val usage = currentState(usageKey) ?: 0.0f
            val lastUpdated = currentState(lastUpdatedKey) ?: "--:--"

            RakutenWidgetContent(usage, lastUpdated)
        }
    }

    @Composable
    fun RakutenWidgetContent(usage: Float, lastUpdated: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(R.color.white))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${usage}GB",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.rakuten_crimson)
                )
            )
            Text(
                text = "/ 20GB",
                style = TextStyle(fontSize = 12.sp, color = ColorProvider(R.color.black))
            )
            Text(
                text = "Updated: $lastUpdated",
                style = TextStyle(fontSize = 10.sp, color = ColorProvider(R.color.teal_700))
            )
        }
    }
}
