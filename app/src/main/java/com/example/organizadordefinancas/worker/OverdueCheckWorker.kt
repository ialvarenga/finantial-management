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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * WorkManager Worker that checks for overdue bills.
 *
 * This worker runs daily and:
 * 1. Finds bills where due_date < today AND status IN ('open', 'partial')
 * 2. Updates their status to 'overdue'
 * 3. Sends notification for overdue bills
 *
 * This helps users stay aware of payments that need attention.
 */
class OverdueCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "OverdueCheckWork"
        const val CHANNEL_ID = "overdue_bills_channel"
        const val NOTIFICATION_ID = 1002

        // Tags for filtering/canceling work
        const val TAG = "overdue_check"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val application = applicationContext as FinanceApplication
            val database = application.database
            val billDao = database.billDao()

            // Get current timestamp
            val currentTime = System.currentTimeMillis()

            // Mark overdue bills (this updates status in the database)
            billDao.markOverdueBills(currentTime)

            // Get overdue bills to send notification
            val overdueBills = billDao.getOverdueBills(currentTime).first()

            if (overdueBills.isNotEmpty()) {
                // Calculate total overdue amount
                val totalOverdueAmount = overdueBills.sumOf { bill ->
                    bill.totalAmount - bill.paidAmount
                }

                sendOverdueNotification(overdueBills.size, totalOverdueAmount)
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Overdue check worker failed", e)
            // Retry on failure
            Result.retry()
        }
    }

    /**
     * Send notification about overdue bills
     */
    private fun sendOverdueNotification(count: Int, totalAmount: Double) {
        createNotificationChannel()

        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        // Create intent to open app when notification is clicked
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "bills")
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = formatCurrency(totalAmount)

        val title = if (count == 1) {
            "Fatura vencida!"
        } else {
            "$count faturas vencidas!"
        }

        val content = if (count == 1) {
            "Você tem uma fatura vencida de $formattedAmount"
        } else {
            "Você tem $count faturas vencidas totalizando $formattedAmount"
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for overdue bills
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Create notification channel (required for Android O+, but minSdk is 33 so always runs)
     */
    private fun createNotificationChannel() {
        val name = "Faturas Vencidas"
        val descriptionText = "Notificações sobre faturas vencidas"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableVibration(true)
        }

        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Format currency in Brazilian Real format
     */
    private fun formatCurrency(amount: Double): String {
        return "R$ %.2f".format(amount).replace(".", ",")
    }
}

