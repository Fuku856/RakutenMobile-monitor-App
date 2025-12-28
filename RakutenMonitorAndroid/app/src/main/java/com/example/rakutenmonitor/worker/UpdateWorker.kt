package com.example.rakutenmonitor.worker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.rakutenmonitor.data.AppPreferences
import com.example.rakutenmonitor.data.RakutenRepository
import com.example.rakutenmonitor.data.SecureStorage
import com.example.rakutenmonitor.ui.RakutenWidget
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Removed secureStorage credential check since using cookies

        val repository = RakutenRepository(context) 
        
        return try {
            val result = repository.fetchData()
            
            if (result.isSuccess) {
                val usage = result.getOrNull() ?: 0.0f
                
                // Save to AppPreferences for Main App UI
                val appPreferences = AppPreferences(context)
                val currentTime = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date())
                appPreferences.saveUsage(usage.toFloat())
                appPreferences.saveLastUpdated(currentTime)

                // Update Widget State
                val glanceId = GlanceAppWidgetManager(context).getGlanceIds(RakutenWidget::class.java).firstOrNull()
                if (glanceId != null) {
                    updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[RakutenWidget.usageKey] = usage.toFloat()
                        prefs[RakutenWidget.lastUpdatedKey] = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    }
                    RakutenWidget().update(context, glanceId)
                }
                Result.success()
            } else {
                // Check for LoginRequiredException in result failure (if exception wasn't thrown directly but encapsulated)
                val ex = result.exceptionOrNull()
                if (ex is com.example.rakutenmonitor.data.LoginRequiredException) {
                     showLoginNotification()
                     Result.failure()
                } else {
                     Result.retry()
                }
            }
        } catch (e: Exception) {
            if (e is com.example.rakutenmonitor.data.LoginRequiredException) {
                showLoginNotification()
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    private fun showLoginNotification() {
        val channelId = "login_required_channel"
        val notificationId = 1001
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Login Required", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Use app icon
            .setContentTitle("楽天モバイル: 再ログインが必要です")
            .setContentText("情報の取得に失敗しました。タップして再ログインしてください。")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
