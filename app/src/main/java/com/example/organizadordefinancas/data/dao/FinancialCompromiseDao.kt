package com.example.organizadordefinancas.data.dao

import androidx.room.*
import com.example.organizadordefinancas.data.model.FinancialCompromise
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialCompromiseDao {
    @Query("SELECT * FROM financial_compromises WHERE isActive = 1 ORDER BY dueDay ASC")
    fun getAllActiveCompromises(): Flow<List<FinancialCompromise>>

    @Query("SELECT * FROM financial_compromises ORDER BY dueDay ASC")
    fun getAllCompromises(): Flow<List<FinancialCompromise>>

    @Query("SELECT * FROM financial_compromises WHERE id = :id")
    fun getCompromiseById(id: Long): Flow<FinancialCompromise?>

    @Query("SELECT * FROM financial_compromises WHERE isActive = 1 AND linkedCreditCardId = :cardId ORDER BY dueDay ASC")
    fun getCompromisesByCardId(cardId: Long): Flow<List<FinancialCompromise>>

    @Query("SELECT * FROM financial_compromises WHERE isActive = 1 AND frequency = :frequency ORDER BY dueDay ASC")
    fun getCompromisesByFrequency(frequency: String): Flow<List<FinancialCompromise>>

    @Query("SELECT SUM(amount) FROM financial_compromises WHERE isActive = 1")
    fun getTotalMonthlyCompromises(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM financial_compromises WHERE isActive = 1 AND linkedCreditCardId IS NULL")
    fun getTotalNonLinkedCompromises(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM financial_compromises WHERE isActive = 1 AND linkedCreditCardId = :cardId")
    fun getTotalCompromisesByCardId(cardId: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompromise(compromise: FinancialCompromise): Long

    @Update
    suspend fun updateCompromise(compromise: FinancialCompromise)

    @Delete
    suspend fun deleteCompromise(compromise: FinancialCompromise)

    @Query("DELETE FROM financial_compromises WHERE id = :id")
    suspend fun deleteCompromiseById(id: Long)

    @Query("UPDATE financial_compromises SET isPaid = :isPaid WHERE id = :id")
    suspend fun updatePaidStatus(id: Long, isPaid: Boolean)

    @Query("UPDATE financial_compromises SET isPaid = 0")
    suspend fun resetAllPaidStatus()
}

