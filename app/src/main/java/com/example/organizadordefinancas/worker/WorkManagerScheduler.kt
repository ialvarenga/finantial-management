package com.example.organizadordefinancas.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Scheduler for setting up WorkManager periodic tasks.
 *
 * This class handles scheduling of:
 * 1. Bill Generation Worker - runs daily to auto-generate credit card bills
 * 2. Overdue Check Worker - runs daily to check for overdue bills
 *
 * Call [scheduleAllWork] from your Application class onCreate() to set up
 * all background tasks.
 */
object WorkManagerScheduler {

    /**
     * Schedule all background workers
     * Should be called from Application.onCreate()
     */
    fun scheduleAllWork(context: Context) {
        scheduleBillGenerationWork(context)
        scheduleOverdueCheckWork(context)
    }

    /**
     * Schedule the bill generation worker to run daily.
     *
     * This worker checks all credit cards with auto_generate_bills = true
     * and generates bills when the closing date is reached.
     */
    fun scheduleBillGenerationWork(context: Context) {
        // Define constraints - we don't need network, but let's make sure
        // the device isn't low on battery
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        // Calculate initial delay to run at midnight
        val initialDelay = calculateDelayUntilMidnight()

        // Create periodic work request - runs every 24 hours
        val billGenerationRequest = PeriodicWorkRequestBuilder<BillGenerationWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(BillGenerationWorker.TAG)
            .build()

        // Enqueue with KEEP policy - don't replace existing work
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BillGenerationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            billGenerationRequest
        )
    }

    /**
     * Schedule the overdue check worker to run daily.
     *
     * This worker checks for bills that have passed their due date
     * and updates their status to 'overdue'.
     */
    fun scheduleOverdueCheckWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        // Calculate initial delay to run at 8 AM (good time for notification)
        val initialDelay = calculateDelayUntil8AM()

        val overdueCheckRequest = PeriodicWorkRequestBuilder<OverdueCheckWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(OverdueCheckWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            OverdueCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            overdueCheckRequest
        )
    }

    /**
     * Cancel all scheduled work
     */
    fun cancelAllWork(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(BillGenerationWorker.WORK_NAME)
        workManager.cancelUniqueWork(OverdueCheckWorker.WORK_NAME)
    }

    /**
     * Cancel specific work by tag
     */
    fun cancelWorkByTag(context: Context, tag: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tag)
    }

    /**
     * Run bill generation immediately (for testing or manual trigger)
     */
    fun runBillGenerationNow(context: Context) {
        val request = androidx.work.OneTimeWorkRequestBuilder<BillGenerationWorker>()
            .addTag(BillGenerationWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    /**
     * Run overdue check immediately (for testing or manual trigger)
     */
    fun runOverdueCheckNow(context: Context) {
        val request = androidx.work.OneTimeWorkRequestBuilder<OverdueCheckWorker>()
            .addTag(OverdueCheckWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    /**
     * Calculate delay in milliseconds until midnight (00:00)
     */
    private fun calculateDelayUntilMidnight(): Long {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return midnight.timeInMillis - now.timeInMillis
    }

    /**
     * Calculate delay in milliseconds until 8 AM
     */
    private fun calculateDelayUntil8AM(): Long {
        val now = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If it's already past 8 AM, schedule for tomorrow
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return targetTime.timeInMillis - now.timeInMillis
    }

    /**
     * Get work info for debugging/status display
     */
    fun getWorkStatus(context: Context) = WorkManager.getInstance(context).getWorkInfosForUniqueWork(BillGenerationWorker.WORK_NAME)
}

