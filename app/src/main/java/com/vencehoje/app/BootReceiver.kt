package com.vencehoje.app

import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import android.content.BroadcastReceiver

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Fatos: O celular ligou, agora mandamos o Worker trabalhar
            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>().build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "vencehoje_loop",
                ExistingWorkPolicy.KEEP, // KEEP porque se já tiver um loop, não queremos resetar
                workRequest
            )
        }
    }
}