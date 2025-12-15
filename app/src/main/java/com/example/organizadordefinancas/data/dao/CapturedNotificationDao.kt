package com.example.organizadordefinancas.data.dao

import androidx.room.*
import com.example.organizadordefinancas.data.model.CapturedNotification
import com.example.organizadordefinancas.data.model.NotificationStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CapturedNotificationDao {

    @Query("SELECT * FROM captured_notifications ORDER BY capturedAt DESC")
    fun getAllNotifications(): Flow<List<CapturedNotification>>

    @Query("SELECT * FROM captured_notifications WHERE status = :status ORDER BY capturedAt DESC")
    fun getNotificationsByStatus(status: NotificationStatus): Flow<List<CapturedNotification>>

    @Query("SELECT * FROM captured_notifications WHERE status = 'PENDING' ORDER BY capturedAt DESC")
    fun getPendingNotifications(): Flow<List<CapturedNotification>>

    @Query("SELECT COUNT(*) FROM captured_notifications WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM captured_notifications WHERE id = :id")
    fun getNotificationById(id: Long): Flow<CapturedNotification?>

    @Query("SELECT * FROM captured_notifications WHERE id = :id")
    suspend fun getNotificationByIdSync(id: Long): CapturedNotification?

    /**
     * Check for potential duplicates based on content, amount, and time window.
     * A notification is considered duplicate if the same content and amount were captured
     * within the last 5 minutes.
     */
    @Query("""
        SELECT * FROM captured_notifications 
        WHERE content = :content 
        AND extractedAmount = :amount 
        AND capturedAt > :timeWindow
        AND status != 'DUPLICATE'
        LIMIT 1
    """)
    suspend fun findDuplicate(content: String, amount: Double?, timeWindow: Long): CapturedNotification?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: CapturedNotification): Long

    @Update
    suspend fun updateNotification(notification: CapturedNotification)

    @Delete
    suspend fun deleteNotification(notification: CapturedNotification)

    @Query("DELETE FROM captured_notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Long)

    @Query("DELETE FROM captured_notifications WHERE status = :status")
    suspend fun deleteNotificationsByStatus(status: NotificationStatus)

    /**
     * Delete old processed/ignored notifications older than specified timestamp.
     */
    @Query("""
        DELETE FROM captured_notifications 
        WHERE capturedAt < :olderThan 
        AND status IN ('PROCESSED', 'IGNORED', 'DUPLICATE')
    """)
    suspend fun deleteOldNotifications(olderThan: Long)

    @Query("UPDATE captured_notifications SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: NotificationStatus)

    @Query("UPDATE captured_notifications SET linkedItemId = :itemId, linkedCardId = :cardId, status = 'PROCESSED' WHERE id = :id")
    suspend fun markAsProcessed(id: Long, itemId: Long, cardId: Long)
}

