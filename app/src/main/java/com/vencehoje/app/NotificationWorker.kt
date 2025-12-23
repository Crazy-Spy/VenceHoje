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

        val targetTime = try { LocalTime.parse(selectedTimeStr) } catch (e: Exception) { LocalTime.of(8, 0) }

        // --- TRAVA DE SEGURANÇA: TOQUE DE RECOLHER ---
        // Se for antes do horário inicial ou depois das 22h, reagendamos para o dia seguinte e paramos.
        if (now.isBefore(targetTime) || now.hour >= 22) {
            recalculateNextLoop(insistence, isSleeping = true)
            return Result.success()
        }

        val bills = database.billDao().getAllBillsSync()
        val pendingBills = bills.filter { bill ->
            if (bill.isPaid || bill.isAutomatic) return@filter false
            try {
                val dueDate = LocalDate.parse(bill.dueDate, formatter)
                dueDate.isBefore(today) || dueDate.isEqual(today)
            } catch (e: Exception) { false }
        }

        if (pendingBills.isEmpty()) {
            recalculateNextLoop(insistence, isSleeping = true) // Ninguém deve nada, volta amanhã
            return Result.success()
        }

        // --- LÓGICA DE DISPARO BASEADA NA INSISTÊNCIA ---
        val shouldNotify = when (insistence) {
            // Padrão: Só nos primeiros 30 min do horário escolhido
            "Padrão" -> now.isAfter(targetTime) && now.isBefore(targetTime.plusMinutes(30))

            // Alto e Crítico: Sempre que estiver no horário permitido (o delay do loop controla a frequência)
            "Alto", "Crítico" -> true
            else -> false
        }

        if (shouldNotify) {
            val firstBill = pendingBills.first().name
            val others = pendingBills.size - 1
            val message = if (others > 0) "Pagar hoje: $firstBill (+ $others contas)" else "Pagar hoje: $firstBill"

            // Usamos ID 1 fixo para a notificação ser atualizada e não criar uma pilha infinita
            sendNotification(message)
        }

        recalculateNextLoop(insistence, isSleeping = false)
        return Result.success()
    }

    private fun recalculateNextLoop(insistence: String, isSleeping: Boolean) {
        val nextInterval = when {
            isSleeping -> 4L to TimeUnit.HOURS // Se está fora do horário, checa de tempos em tempos até amanhecer
            insistence == "Alto" -> 4L to TimeUnit.HOURS // 3 vezes ao dia (aproximadamente)
            insistence == "Crítico" -> 2L to TimeUnit.HOURS // A cada 2 horas conforme o texto
            else -> 1L to TimeUnit.HOURS // Padrão checa menos vezes
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
        val channel = NotificationChannel(channelId, "Alertas Financeiros", NotificationManager.IMPORTANCE_HIGH)
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
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        manager.notify(1, notification)
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

            manager.notify(99, notification)
        }
    }
}