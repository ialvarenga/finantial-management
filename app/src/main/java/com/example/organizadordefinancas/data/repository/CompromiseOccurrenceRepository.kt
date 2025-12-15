package com.example.organizadordefinancas.data.repository

import com.example.organizadordefinancas.data.dao.CompromiseOccurrenceDao
import com.example.organizadordefinancas.data.model.CompromiseOccurrence
import com.example.organizadordefinancas.data.model.FinancialCompromise
import com.example.organizadordefinancas.data.model.OccurrenceStatus
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class CompromiseOccurrenceRepository(private val occurrenceDao: CompromiseOccurrenceDao) {

    companion object {
        // Generate occurrences for the next 3 months by default
        const val DEFAULT_GENERATION_MONTHS = 3
        // Clean up paid occurrences older than 6 months
        const val CLEANUP_MONTHS = 6
    }

    fun getOccurrencesByCompromiseId(compromiseId: Long): Flow<List<CompromiseOccurrence>> =
        occurrenceDao.getOccurrencesByCompromiseId(compromiseId)

    fun getOccurrencesInRange(startDate: Long, endDate: Long): Flow<List<CompromiseOccurrence>> =
        occurrenceDao.getOccurrencesInRange(startDate, endDate)

    fun getPendingOccurrences(): Flow<List<CompromiseOccurrence>> =
        occurrenceDao.getPendingOccurrences()

    fun getOverdueOccurrences(): Flow<List<CompromiseOccurrence>> =
        occurrenceDao.getOverdueOccurrences()

    fun getUpcomingOccurrences(days: Int = 7): Flow<List<CompromiseOccurrence>> {
        val now = System.currentTimeMillis()
        val endTime = now + (days * 24 * 60 * 60 * 1000L)
        return occurrenceDao.getUpcomingOccurrences(now, endTime)
    }

    fun getTotalPendingInRange(startDate: Long, endDate: Long): Flow<Double?> =
        occurrenceDao.getTotalPendingInRange(startDate, endDate)

    suspend fun getNextOccurrence(compromiseId: Long): CompromiseOccurrence? =
        occurrenceDao.getNextOccurrence(compromiseId)

    suspend fun getLatestOccurrence(compromiseId: Long): CompromiseOccurrence? =
        occurrenceDao.getLatestOccurrence(compromiseId)

    suspend fun getOccurrenceById(id: Long): CompromiseOccurrence? =
        occurrenceDao.getOccurrenceById(id)

    suspend fun insertOccurrence(occurrence: CompromiseOccurrence): Long =
        occurrenceDao.insertOccurrence(occurrence)

    suspend fun insertOccurrences(occurrences: List<CompromiseOccurrence>): List<Long> =
        occurrenceDao.insertOccurrences(occurrences)

    suspend fun updateOccurrence(occurrence: CompromiseOccurrence) =
        occurrenceDao.updateOccurrence(occurrence)

    suspend fun deleteOccurrence(occurrence: CompromiseOccurrence) =
        occurrenceDao.deleteOccurrence(occurrence)

    suspend fun deleteOccurrencesByCompromiseId(compromiseId: Long) =
        occurrenceDao.deleteOccurrencesByCompromiseId(compromiseId)

    /**
     * Mark an occurrence as paid.
     */
    suspend fun markAsPaid(id: Long, paidAmount: Double, paidDate: Long = System.currentTimeMillis()) {
        val occurrence = occurrenceDao.getOccurrenceById(id) ?: return
        val status = if (paidDate > occurrence.dueDate) OccurrenceStatus.LATE else OccurrenceStatus.PAID
        occurrenceDao.markAsPaid(id, status, paidDate, paidAmount)
    }

    /**
     * Skip an occurrence.
     */
    suspend fun skipOccurrence(id: Long) {
        occurrenceDao.updateStatus(id, OccurrenceStatus.SKIPPED)
    }

    /**
     * Generate occurrences for a compromise for the specified number of months ahead.
     * Avoids creating duplicate occurrences.
     */
    suspend fun generateOccurrences(
        compromise: FinancialCompromise,
        monthsAhead: Int = DEFAULT_GENERATION_MONTHS
    ): List<CompromiseOccurrence> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MONTH, monthsAhead)
        }
        val endDate = compromise.endDate?.let { minOf(it, calendar.timeInMillis) } ?: calendar.timeInMillis

        val occurrencesToCreate = mutableListOf<CompromiseOccurrence>()
        var nextDueDate = compromise.getNextDueDate(now - (24 * 60 * 60 * 1000L)) // Start from yesterday to catch today

        // Limit iterations to prevent infinite loops
        var iterations = 0
        val maxIterations = 100

        while (nextDueDate <= endDate && iterations < maxIterations) {
            iterations++

            // Check if occurrence already exists for this date
            val startOfDay = Calendar.getInstance().apply {
                timeInMillis = nextDueDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfDay = startOfDay + (24 * 60 * 60 * 1000L)

            val existingCount = occurrenceDao.countOccurrencesOnDate(
                compromise.id,
                startOfDay,
                endOfDay
            )

            if (existingCount == 0) {
                occurrencesToCreate.add(
                    CompromiseOccurrence(
                        compromiseId = compromise.id,
                        dueDate = nextDueDate,
                        expectedAmount = compromise.amount
                    )
                )
            }

            // Move to next occurrence
            nextDueDate = compromise.getNextDueDate(nextDueDate + 1000) // Add 1 second to move past current
        }

        if (occurrencesToCreate.isNotEmpty()) {
            occurrenceDao.insertOccurrences(occurrencesToCreate)
        }

        return occurrencesToCreate
    }

    /**
     * Clean up old paid occurrences.
     */
    suspend fun cleanupOldOccurrences(monthsOld: Int = CLEANUP_MONTHS) {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MONTH, -monthsOld)
        }
        occurrenceDao.deleteOldPaidOccurrences(calendar.timeInMillis)
    }

    /**
     * Regenerate all occurrences for a compromise (e.g., after editing).
     * Only removes future pending occurrences, keeps historical ones.
     */
    suspend fun regenerateOccurrences(
        compromise: FinancialCompromise,
        monthsAhead: Int = DEFAULT_GENERATION_MONTHS
    ): List<CompromiseOccurrence> {
        // Note: We don't delete existing occurrences to preserve payment history
        // The generateOccurrences method already handles duplicates
        return generateOccurrences(compromise, monthsAhead)
    }
}

