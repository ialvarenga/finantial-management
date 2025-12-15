package com.example.organizadordefinancas.service

import android.content.Context
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.organizadordefinancas.data.database.AppDatabase
import com.example.organizadordefinancas.data.model.BankAppConfig
import com.example.organizadordefinancas.data.model.CapturedNotification
import com.example.organizadordefinancas.data.model.NotificationStatus
import com.example.organizadordefinancas.data.parser.NotificationParser
import com.example.organizadordefinancas.data.repository.CapturedNotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Service that listens to notifications from banking apps and captures transaction data.
 */
class FinanceNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "FinanceNotificationListener"
        const val PREFS_NAME = "notification_listener_prefs"
        const val PREF_ENABLED_APPS = "enabled_apps"
        const val PREF_LISTENER_ENABLED = "listener_enabled"

        /**
         * Check if the app has notification listener permission.
         */
        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return enabledListeners?.contains(context.packageName) == true
        }

        /**
         * Open the notification access settings.
         */
        fun openNotificationAccessSettings(context: Context) {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            )
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var repository: CapturedNotificationRepository
    private lateinit var parser: NotificationParser
    private lateinit var prefs: SharedPreferences

    private var enabledPackages: Set<String> = emptySet()
    private var isListenerEnabled: Boolean = true

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        val database = AppDatabase.getDatabase(applicationContext)
        repository = CapturedNotificationRepository(database.capturedNotificationDao())
        parser = NotificationParser()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        loadSettings()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "Service destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName

        // Check if listener is enabled and package is monitored
        if (!isListenerEnabled) {
            Log.d(TAG, "Listener is disabled, ignoring notification")
            return
        }

        if (packageName !in enabledPackages) {
            // Not a monitored app
            return
        }

        Log.d(TAG, "Received notification from: $packageName")

        // Extract notification content
        val notification = sbn.notification
        val extras = notification?.extras

        val title = extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val content = extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras?.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString()

        // Use big text if available, otherwise use regular content
        val fullContent = bigText ?: content

        if (title.isBlank() && fullContent.isBlank()) {
            Log.d(TAG, "Empty notification, ignoring")
            return
        }

        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Content: $fullContent")

        // Parse the notification
        val parsed = parser.parse(packageName, title, fullContent)

        // Skip if we couldn't extract any useful information
        if (parsed.amount == null && parsed.confidence < 0.3f) {
            Log.d(TAG, "Could not parse notification, skipping")
            return
        }


        // Create captured notification
        val capturedNotification = CapturedNotification(
            packageName = packageName,
            title = title,
            content = fullContent,
            capturedAt = System.currentTimeMillis(),
            status = if (parsed.amount != null) NotificationStatus.PENDING else NotificationStatus.FAILED,
            extractedAmount = parsed.amount,
            extractedMerchant = parsed.merchant,
            extractedCardLastFour = parsed.cardLastFour,
            transactionType = parsed.transactionType.name,
            parsingConfidence = parsed.confidence
        )

        // Save to database with duplicate check
        serviceScope.launch {
            try {
                val id = repository.insertNotificationWithDuplicateCheck(capturedNotification)
                Log.d(TAG, "Saved notification with id: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // We don't need to do anything when notifications are removed
    }

    /**
     * Load settings from SharedPreferences.
     */
    private fun loadSettings() {
        isListenerEnabled = prefs.getBoolean(PREF_LISTENER_ENABLED, true)

        // Default to all supported apps if no preference is set
        val defaultApps = BankAppConfig.DEFAULT_BANK_APPS
            .map { it.packageName }
            .toSet()

        enabledPackages = prefs.getStringSet(PREF_ENABLED_APPS, defaultApps) ?: defaultApps

        Log.d(TAG, "Loaded settings - Enabled: $isListenerEnabled, Apps: $enabledPackages")
    }

    /**
     * Reload settings (called when preferences change).
     */
    fun reloadSettings() {
        loadSettings()
    }
}

