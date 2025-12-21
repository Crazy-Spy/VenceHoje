package com.vencehoje.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("configs", Context.MODE_PRIVATE)
        val selectedTimeStr = prefs.getString("notify_time", "08:00") ?: "08:00"
        val insistence = prefs.getString("insistence", "Padrão") ?: "Padrão"

        val database = AppDatabase.getDatabase(applicationContext)
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val today = LocalDate.now()
        val now = LocalTime.now()
        val targetTime = LocalTime.parse(selectedTimeStr)

        val bills = database.billDao().getAllBillsSync()
        val hasPending = bills.any { !it.isPaid && (LocalDate.parse(it.dueDate, formatter).isBefore(today) || LocalDate.parse(it.dueDate, formatter).isEqual(today)) }

        if (!hasPending) return Result.success()

        // Lógica de disparo baseada no horário e nível
        val shouldNotify = when (insistence) {
            "Padrão" -> now.hour == targetTime.hour && now.minute < 15
            "Alto" -> now.isAfter(targetTime) || now.hour == targetTime.hour
            "Crítico" -> now.isAfter(targetTime) || now.hour == targetTime.hour
            else -> false
        }

        if (shouldNotify) {
            sendNotification()
        }

        // Define o próximo ciclo de checagem
        val nextInterval = when (insistence) {
            "Alto" -> 4L to TimeUnit.HOURS
            "Crítico" -> 1L to TimeUnit.HOURS
            else -> 15L to TimeUnit.MINUTES
        }

        val nextCheck = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(nextInterval.first, nextInterval.second)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "vencehoje_loop",
            ExistingWorkPolicy.REPLACE,
            nextCheck
        )

        return Result.success()
    }

    private fun sendNotification() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "vencehoje_notifications"
        val channel = NotificationChannel(channelId, "Alertas Financeiros", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("VenceHoje 🚨")
            .setContentText("Existem obrigações financeiras pendentes para hoje.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(1, notification)
    }
}