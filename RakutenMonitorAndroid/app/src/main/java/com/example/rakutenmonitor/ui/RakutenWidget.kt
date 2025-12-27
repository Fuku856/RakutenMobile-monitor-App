package com.example.rakutenmonitor.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
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
        // Glassmorphism effect: Semi-transparent background
        // Note: Real blur is limited in AppWidgets, so we use transparency and white/black tint.
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xE6FFFFFF), Color(0xE61E1E1E))) // ~90% opacity
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title / Brand
                Text(
                    text = "Rakuten Mobile",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = ColorProvider(Color.Gray, Color.LightGray)
                    )
                )
                
                Spacer(modifier = GlanceModifier.height(4.dp))

                // Usage Text
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format("%.1f", usage),
                        style = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(R.color.rakuten_crimson)
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(2.dp))
                    Text(
                        text = "GB",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorProvider(Color.Black, Color.White)
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Linear Progress Bar (Simulated with Box)
                val limit = 20.0f
                val progress = (usage / limit).coerceIn(0.0f, 1.0f)
                
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(ColorProvider(Color(0xFFE0E0E0), Color(0xFF424242))) // Track
                ) {
                   Box(
                        modifier = GlanceModifier
                            .fillMaxWidth(progress)
                            .height(6.dp)
                            .background(ColorProvider(R.color.rakuten_crimson)) // Indicator
                   ) {}
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Last Updated
                Text(
                    text = "Updated: $lastUpdated",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = ColorProvider(Color.Gray)
                    )
                )
            }
        }
    }
}
