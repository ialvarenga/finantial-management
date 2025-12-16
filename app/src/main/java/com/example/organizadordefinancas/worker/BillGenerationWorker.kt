package com.example.organizadordefinancas.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.organizadordefinancas.FinanceApplication
import com.example.organizadordefinancas.MainActivity
import com.example.organizadordefinancas.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager Worker that automatically generates credit card bills.
 *
 * This worker runs daily and checks all credit cards with auto_generate_bills = true.
 * If today is on or after the card's closing date (fechamento), it generates the bill
 * for the current month.
 *
 * The worker sends a notification when bills are generated.
 */
class BillGenerationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "BillGenerationWork"
        const val CHANNEL_ID = "bill_generation_channel"
        const val NOTIFICATION_ID = 1001

        // Tags for filtering/canceling work
        const val TAG = "bill_generation"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val application = applicationContext as FinanceApplication
            val billGenerationService = application.billGenerationService

            // Run auto-generation
            val summary = billGenerationService.autoGenerateBillsIfNeeded()

            // Send notification if bills were generated
            if (summary.billsGenerated > 0) {
                sendBillGeneratedNotification(summary.billsGenerated)
            }

            // Log errors if any
            if (summary.errors.isNotEmpty()) {
                // In production, you might want to log these to analytics
                summary.errors.forEach { error ->
                    // Log error (could use Timber or other logging framework)
                    android.util.Log.e(TAG, "Bill generation error: $error")
                }
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Bill generation worker failed", e)
            // Retry on failure (up to WorkManager's retry limit)
            Result.retry()
        }
    }

    /**
     * Send notification when bills are generated
     */
    private fun sendBillGeneratedNotification(count: Int) {
        createNotificationChannel()

        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        // Create intent to open app when notification is clicked
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // You could add extras here to navigate to bills screen
            putExtra("navigate_to", "bills")
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (count == 1) "Nova fatura gerada" else "$count novas faturas geradas"
        val content = if (count == 1) {
            "Uma nova fatura de cartão de crédito foi gerada"
        } else {
            "$count novas faturas de cartão de crédito foram geradas"
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app icon
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Create notification channel (required for Android O+, but minSdk is 33 so always runs)
     */
    private fun createNotificationChannel() {
        val name = "Geração de Faturas"
        val descriptionText = "Notificações sobre novas faturas geradas automaticamente"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

