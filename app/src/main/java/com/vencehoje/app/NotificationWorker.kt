package com.vencehoje.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.vencehoje.app.data.AppDatabase
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val applicationContext = applicationContext
        val prefs = applicationContext.getSharedPreferences("configs", Context.MODE_PRIVATE)
        val selectedTimeStr = prefs.getString("notify_time", "08:00") ?: "08:00"
        val insistence = prefs.getString("insistence", "Padrão") ?: "Padrão"

        val database = AppDatabase.getDatabase(applicationContext)
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val today = LocalDate.now()
        val now = LocalTime.now()
        val targetTime = try {
            LocalTime.parse(selectedTimeStr)
        } catch (e: Exception) {
            LocalTime.of(8, 0)
        }

        // Pega todas as contas
        val bills = database.billDao().getAllBillsSync()

        // FILTRO INTELIGENTE:
        // 1. Não pode estar paga (!bill.isPaid)
        // 2. Não pode ser débito automático (!bill.isAutomatic)
        // 3. Tem que estar vencida ou vencer hoje
        val pendingBills = bills.filter { bill ->
            if (bill.isPaid || bill.isAutomatic) return@filter false

            try {
                val dueDate = LocalDate.parse(bill.dueDate, formatter)
                dueDate.isBefore(today) || dueDate.isEqual(today)
            } catch (e: Exception) {
                false
            }
        }

        // Se não tem nada "manual" pra pagar, encerra por aqui
        if (pendingBills.isEmpty()) {
            recalculateNextLoop(insistence)
            return Result.success()
        }

        // Lógica de disparo baseada na insistência
        val shouldNotify = when (insistence) {
            "Padrão" -> now.isAfter(targetTime) && now.isBefore(targetTime.plusMinutes(30))
            "Alto" -> now.isAfter(targetTime)
            "Crítico" -> now.isAfter(targetTime)
            else -> false
        }

        if (shouldNotify) {
            val firstBill = pendingBills.first().name
            val others = pendingBills.size - 1
            val message = if (others > 0) {
                "Pagar hoje: $firstBill (+ $others contas)"
            } else {
                "Pagar hoje: $firstBill"
            }
            sendNotification(message)
        }

        recalculateNextLoop(insistence)
        return Result.success()
    }

    private fun sendNotification(message: String) {
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "vencehoje_notifications"
        val channel = NotificationChannel(
            channelId,
            "Alertas Financeiros",
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification_vencehoje)
            .setContentTitle("VenceHoje 🚨")
            .setContentText(message) // Agora a mensagem é dinâmica!
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Garante vibração e som padrão
            .build()

        manager.notify(1, notification)
    }


    private fun recalculateNextLoop(insistence: String) {
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
    }

    companion object {
        fun sendTestNotification(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "vencehoje_notifications"
            val channel = NotificationChannel(channelId, "Alertas Financeiros", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification_vencehoje)
                .setContentTitle("VenceHoje 🚨")
                .setContentText("Teste de Notificação: O vigia está acordado!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            manager.notify(99, notification) // ID 99 para não confundir com as reais
        }
    }
}