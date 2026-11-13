package com.englishquest.app.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.englishquest.app.MainActivity
import com.englishquest.app.R
import com.englishquest.app.data.ProgressStore
import com.englishquest.app.data.Settings
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Planifie le rappel quotidien avec WorkManager (survit aux redémarrages du téléphone). */
object ReminderScheduler {
    const val CHANNEL_ID = "daily_reminder"
    private const val WORK_NAME = "daily_reminder_work"

    fun createChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(CHANNEL_ID, "Rappel quotidien", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "Un petit rappel pour garder ta série."
        nm.createNotificationChannel(channel)
    }

    fun schedule(context: Context, s: Settings) {
        val wm = WorkManager.getInstance(context.applicationContext)
        if (!s.reminderOn) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(s.reminderHour.coerceIn(0, 23), s.reminderMinute.coerceIn(0, 59))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMillis()
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val p = ProgressStore(ctx).load()
        if (!p.settings.reminderOn) return Result.success()
        val today = LocalDate.now().toEpochDay()
        if (p.lastActiveDay == today) return Result.success() // déjà entraîné aujourd'hui
        if (!ReminderScheduler.canNotify(ctx)) return Result.success()

        ReminderScheduler.createChannel(ctx)
        val messages = if (p.streak > 0) listOf(
            "🔥 Ta série de ${p.streak} jour(s) t'attend ! 5 minutes suffisent.",
            "❄️ Ne laisse pas ta série de ${p.streak} jour(s) geler !",
            "⚡ Un petit combo avant ce soir ? Ta série : ${p.streak} jour(s).",
        ) else listOf(
            "🎯 Une leçon rapide pour commencer une nouvelle série ?",
            "👑 Le boss de ton unité t'attend… Prêt(e) ?",
            "🧠 Quelques mots révisés aujourd'hui = des points en plus au bac !",
        )
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(ctx, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("English Quest")
            .setContentText(messages.random())
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(1001, notification)
        } catch (e: SecurityException) {
            // Autorisation retirée entre-temps
        }
        return Result.success()
    }
}
