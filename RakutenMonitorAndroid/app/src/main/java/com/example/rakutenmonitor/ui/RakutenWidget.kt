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
        val context = androidx.glance.LocalContext.current
        
        // Glassmorphism effect: Semi-transparent background
        // Note: Real blur is limited in AppWidgets, so we use transparency and white/black tint.
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xE61E1E1E))) // ~90% opacity dark background
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
                        color = ColorProvider(Color.LightGray)
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
                            color = ColorProvider(Color.White)
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Pie Chart
                val limit = 20.0f // Default limit, could be configurable
                val bitmap = createPieChartBitmap(context, usage, limit)
                
                androidx.glance.Image(
                    provider = androidx.glance.ImageProvider(bitmap),
                    contentDescription = "Data Usage Pie Chart",
                    modifier = GlanceModifier.size(120.dp)
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Last Updated
                Text(
                    text = "Updated: $lastUpdated",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = ColorProvider(Color.LightGray)
                    )
                )
            }
        }
    }

    private fun createPieChartBitmap(context: Context, usage: Float, limit: Float): android.graphics.Bitmap {
        val size = 300 // Bitmap size (px)
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val rect = android.graphics.RectF(0f, 0f, size.toFloat(), size.toFloat())

        // Calculate angles
        val usageRatio = (usage / limit).coerceIn(0.0f, 1.0f)
        val sweepAngle = 360f * usageRatio
        
        // Draw background circle (Remaining)
        paint.color = android.graphics.Color.DKGRAY
        canvas.drawArc(rect, 0f, 360f, true, paint)

        // Draw usage arc
        paint.color = context.getColor(R.color.rakuten_crimson)
        canvas.drawArc(rect, -90f, sweepAngle, true, paint) // Start from top

        // Draw inner circle to make it a donut chart (Optional, but looks nice)
        val innerRadiusRatio = 0.6f
        val innerSize = size * innerRadiusRatio
        val innerOffset = (size - innerSize) / 2
        val innerRect = android.graphics.RectF(innerOffset, innerOffset, size.toFloat() - innerOffset, size.toFloat() - innerOffset)
        
        paint.color = android.graphics.Color.parseColor("#1E1E1E") // Match background roughly
        canvas.drawOval(innerRect, paint)

        return bitmap
    }
}
