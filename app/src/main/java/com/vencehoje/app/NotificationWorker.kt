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

        // Fatos: Pegamos as configs do usuário ou usamos o padrão seguro
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

        // --- TRAVA DE SEGURANÇA: TOQUE DE RECOLHER ---
        // Respeita o sono da Cris e dos meninos: antes do horário ou depois das 22h, o vigia dorme.
        if (now.isBefore(targetTime) || now.hour >= 22) {
            recalculateNextLoop(insistence, isSleeping = true)
            return Result.success()
        }

        // Buscamos as contas e categorias de forma assíncrona/segura
        val bills = database.billDao().getAllBillsGlobalSync()
        val allCategories = database.categoryDao().getAllCategoriesGlobalSync()

        // Filtro inteligente: Só o que não está pago, não é automático e venceu/vence hoje.
        val pendingBills = bills.filter { bill ->
            if (bill.isPaid || bill.isAutomatic) return@filter false
            try {
                val dueDate = LocalDate.parse(bill.dueDate, formatter)
                dueDate.isBefore(today) || dueDate.isEqual(today)
            } catch (e: Exception) { false }
        }

        // Se a missão foi cumprida e não há boletos, volta a checar amanhã.
        if (pendingBills.isEmpty()) {
            recalculateNextLoop(insistence, isSleeping = true)
            return Result.success()
        }

        // --- LÓGICA DE DISPARO ---
        val shouldNotify = when (insistence) {
            "Padrão" -> now.isAfter(targetTime) && now.isBefore(targetTime.plusMinutes(45))
            "Alto", "Crítico" -> true
            else -> false
        }

        if (shouldNotify) {
            val firstBill = pendingBills.first()
            val category = allCategories.find { it.id == firstBill.categoryId }

            // Ciência: Se for emoji, usamos ele. Se for ícone de sistema (texto longo), usamos a sirene 🚨.
            val isEmoji = category?.iconName?.let {
                it.length <= 2 || it.any { c -> Character.isSurrogate(c) }
            } ?: false

            val emoji = if (isEmoji) category?.iconName else "🚨"

            val others = pendingBills.size - 1
            val message = if (others > 0) {
                "$emoji ${firstBill.name} (+ $others contas)"
            } else {
                "$emoji ${firstBill.name}"
            }

            sendNotification(message)
        }

        recalculateNextLoop(insistence, isSleeping = false)
        return Result.success()
    }

    private fun recalculateNextLoop(insistence: String, isSleeping: Boolean) {
        val nextInterval = when {
            isSleeping -> 4L to TimeUnit.HOURS
            insistence == "Alto" -> 4L to TimeUnit.HOURS // ~3 vezes ao dia
            insistence == "Crítico" -> 2L to TimeUnit.HOURS // A cada 2 horas conforme prometido na UI
            else -> 1L to TimeUnit.HOURS // Padrão checa menos para poupar bateria
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

    private fun sendNotification(message: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "vencehoje_notifications"

        val channel = NotificationChannel(
            channelId,
            "Alertas Financeiros",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificações de vencimento do VenceHoje"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification_vencehoje)
            .setContentTitle("VenceHoje 🚨")
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        // ID 1 fixo garante que uma notificação nova substitua a anterior (sem empilhar lixo)
        manager.notify(1, notification)
    }

    companion object {
        fun sendTestNotification(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "vencehoje_notifications"

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification_vencehoje)
                .setContentTitle("VenceHoje 🚨")
                .setContentText("O vigia está online e de olho nos boletos! 🫡")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            manager.notify(99, notification)
        }
    }
}