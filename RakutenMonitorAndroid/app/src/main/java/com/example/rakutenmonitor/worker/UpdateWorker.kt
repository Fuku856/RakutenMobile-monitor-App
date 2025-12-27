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
        val secureStorage = SecureStorage(context)
        val userId = secureStorage.getUserId()
        val password = secureStorage.getPassword()

        if (userId == null || password == null) {
            return Result.failure()
        }

        val repository = RakutenRepository()
        val result = repository.fetchData(userId, password)

        return if (result.isSuccess) {
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
            Result.retry()
        }
    }
}
