package com.example.organizadordefinancas.data.dao

import androidx.room.*
import com.example.organizadordefinancas.data.model.CompromiseOccurrence
import com.example.organizadordefinancas.data.model.OccurrenceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CompromiseOccurrenceDao {

    /**
     * Get all occurrences for a specific compromise.
     */
    @Query("SELECT * FROM compromise_occurrences WHERE compromiseId = :compromiseId ORDER BY dueDate ASC")
    fun getOccurrencesByCompromiseId(compromiseId: Long): Flow<List<CompromiseOccurrence>>

    /**
     * Get all occurrences within a date range.
     */
    @Query("""
        SELECT * FROM compromise_occurrences 
        WHERE dueDate >= :startDate AND dueDate <= :endDate 
        ORDER BY dueDate ASC
    """)
    fun getOccurrencesInRange(startDate: Long, endDate: Long): Flow<List<CompromiseOccurrence>>

    /**
     * Get all pending occurrences (not paid yet).
     */
    @Query("""
        SELECT * FROM compromise_occurrences 
        WHERE status = 'PENDING' 
        ORDER BY dueDate ASC
    """)
    fun getPendingOccurrences(): Flow<List<CompromiseOccurrence>>

    /**
     * Get overdue occurrences (past due date and not paid).
     */
    @Query("""
        SELECT * FROM compromise_occurrences 
        WHERE status = 'PENDING' AND dueDate < :currentTime 
        ORDER BY dueDate ASC
    """)
    fun getOverdueOccurrences(currentTime: Long = System.currentTimeMillis()): Flow<List<CompromiseOccurrence>>

    /**
     * Get upcoming occurrences within the next N days.
     */
    @Query("""
        SELECT * FROM compromise_occurrences 
        WHERE status = 'PENDING' 
        AND dueDate >= :startTime 
        AND dueDate <= :endTime 
        ORDER BY dueDate ASC
    """)
    fun getUpcomingOccurrences(startTime: Long, endTime: Long): Flow<List<CompromiseOccurrence>>

    /**
     * Get the next occurrence for a specific compromise.
     */
    @Query("""
        SELECT * FROM compromise_occurrences 
        WHERE compromiseId = :compromiseId 
        AND status = 'PENDING' 
        ORDER BY dueDate ASC 
        LIMIT 1
    """)
    suspend fun getNextOccurrence(compromiseId: Long): CompromiseOccurrence?

    /**
     * Get the latest occurrence for a specific compromise (regardless of status).
     */
    @Query("""
        SELECT * FROM compromise_occurrences 
        WHERE compromiseId = :compromiseId 
        ORDER BY dueDate DESC 
        LIMIT 1
    """)
    suspend fun getLatestOccurrence(compromiseId: Long): CompromiseOccurrence?

    /**
     * Get occurrence by ID.
     */
    @Query("SELECT * FROM compromise_occurrences WHERE id = :id")
    suspend fun getOccurrenceById(id: Long): CompromiseOccurrence?

    /**
     * Check if an occurrence already exists for a compromise on a specific date.
     * Uses a tolerance of 1 day to account for time differences.
     */
    @Query("""
        SELECT COUNT(*) FROM compromise_occurrences 
        WHERE compromiseId = :compromiseId 
        AND dueDate >= :startOfDay 
        AND dueDate < :endOfDay
    """)
    suspend fun countOccurrencesOnDate(compromiseId: Long, startOfDay: Long, endOfDay: Long): Int

    /**
     * Get total expected amount for pending occurrences in a date range.
     */
    @Query("""
        SELECT SUM(expectedAmount) FROM compromise_occurrences 
        WHERE status = 'PENDING' 
        AND dueDate >= :startDate 
        AND dueDate <= :endDate
    """)
    fun getTotalPendingInRange(startDate: Long, endDate: Long): Flow<Double?>

    /**
     * Get count of occurrences by status for a compromise.
     */
    @Query("""
        SELECT COUNT(*) FROM compromise_occurrences 
        WHERE compromiseId = :compromiseId 
        AND status = :status
    """)
    suspend fun countByStatus(compromiseId: Long, status: OccurrenceStatus): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccurrence(occurrence: CompromiseOccurrence): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccurrences(occurrences: List<CompromiseOccurrence>): List<Long>

    @Update
    suspend fun updateOccurrence(occurrence: CompromiseOccurrence)

    @Delete
    suspend fun deleteOccurrence(occurrence: CompromiseOccurrence)

    /**
     * Delete all occurrences for a compromise.
     */
    @Query("DELETE FROM compromise_occurrences WHERE compromiseId = :compromiseId")
    suspend fun deleteOccurrencesByCompromiseId(compromiseId: Long)

    /**
     * Delete old paid occurrences (older than specified date).
     * Used for cleanup of historical data.
     */
    @Query("""
        DELETE FROM compromise_occurrences 
        WHERE status IN ('PAID', 'LATE', 'SKIPPED') 
        AND dueDate < :beforeDate
    """)
    suspend fun deleteOldPaidOccurrences(beforeDate: Long)

    /**
     * Mark an occurrence as paid.
     */
    @Query("""
        UPDATE compromise_occurrences 
        SET status = :status, paidDate = :paidDate, paidAmount = :paidAmount 
        WHERE id = :id
    """)
    suspend fun markAsPaid(id: Long, status: OccurrenceStatus, paidDate: Long, paidAmount: Double)

    /**
     * Update occurrence status.
     */
    @Query("UPDATE compromise_occurrences SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: OccurrenceStatus)
}

