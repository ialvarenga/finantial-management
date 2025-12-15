package com.example.organizadordefinancas.data.repository

import com.example.organizadordefinancas.data.dao.CapturedNotificationDao
import com.example.organizadordefinancas.data.model.CapturedNotification
import com.example.organizadordefinancas.data.model.NotificationStatus
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class CapturedNotificationRepository(
    private val capturedNotificationDao: CapturedNotificationDao
) {
    fun getAllNotifications(): Flow<List<CapturedNotification>> =
        capturedNotificationDao.getAllNotifications()

    fun getNotificationsByStatus(status: NotificationStatus): Flow<List<CapturedNotification>> =
        capturedNotificationDao.getNotificationsByStatus(status)

    fun getPendingNotifications(): Flow<List<CapturedNotification>> =
        capturedNotificationDao.getPendingNotifications()

    fun getPendingCount(): Flow<Int> =
        capturedNotificationDao.getPendingCount()

    fun getNotificationById(id: Long): Flow<CapturedNotification?> =
        capturedNotificationDao.getNotificationById(id)

    suspend fun getNotificationByIdSync(id: Long): CapturedNotification? =
        capturedNotificationDao.getNotificationByIdSync(id)

    /**
     * Insert notification with duplicate detection.
     * Returns the ID of the inserted notification, or -1 if it was a duplicate.
     */
    suspend fun insertNotificationWithDuplicateCheck(notification: CapturedNotification): Long {
        // Check for duplicates within the last 5 minutes
        val timeWindow = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5)
        val duplicate = capturedNotificationDao.findDuplicate(
            content = notification.content,
            amount = notification.extractedAmount,
            timeWindow = timeWindow
        )

        return if (duplicate != null) {
            // Mark this one as duplicate and still save it for records
            val duplicateNotification = notification.copy(status = NotificationStatus.DUPLICATE)
            capturedNotificationDao.insertNotification(duplicateNotification)
        } else {
            capturedNotificationDao.insertNotification(notification)
        }
    }

    suspend fun insertNotification(notification: CapturedNotification): Long =
        capturedNotificationDao.insertNotification(notification)

    suspend fun updateNotification(notification: CapturedNotification) =
        capturedNotificationDao.updateNotification(notification)

    suspend fun deleteNotification(notification: CapturedNotification) =
        capturedNotificationDao.deleteNotification(notification)

    suspend fun deleteNotificationById(id: Long) =
        capturedNotificationDao.deleteNotificationById(id)

    suspend fun updateStatus(id: Long, status: NotificationStatus) =
        capturedNotificationDao.updateStatus(id, status)

    suspend fun markAsProcessed(notificationId: Long, itemId: Long, cardId: Long) =
        capturedNotificationDao.markAsProcessed(notificationId, itemId, cardId)

    /**
     * Clean up old notifications that were processed, ignored, or marked as duplicates
     * and are older than the specified number of days.
     */
    suspend fun cleanupOldNotifications(daysToKeep: Int = 30) {
        val olderThan = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysToKeep.toLong())
        capturedNotificationDao.deleteOldNotifications(olderThan)
    }

    suspend fun deleteNotificationsByStatus(status: NotificationStatus) =
        capturedNotificationDao.deleteNotificationsByStatus(status)
}

