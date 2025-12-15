package com.example.organizadordefinancas.data.dao

import androidx.room.*
import com.example.organizadordefinancas.data.model.Income
import com.example.organizadordefinancas.data.model.IncomeType
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Query("SELECT * FROM incomes WHERE isActive = 1 ORDER BY receiveDay ASC")
    fun getAllActiveIncomes(): Flow<List<Income>>

    @Query("SELECT * FROM incomes ORDER BY date DESC")
    fun getAllIncomes(): Flow<List<Income>>

    @Query("SELECT * FROM incomes WHERE type = :type AND isActive = 1 ORDER BY receiveDay ASC")
    fun getIncomesByType(type: IncomeType): Flow<List<Income>>

    @Query("SELECT * FROM incomes WHERE id = :id")
    fun getIncomeById(id: Long): Flow<Income?>

    @Query("SELECT SUM(amount) FROM incomes WHERE type = 'RECURRENT' AND isActive = 1")
    fun getTotalRecurrentIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM incomes WHERE isActive = 1")
    fun getTotalMonthlyIncome(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: Income): Long

    @Update
    suspend fun updateIncome(income: Income)

    @Delete
    suspend fun deleteIncome(income: Income)

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteIncomeById(id: Long)

    @Query("UPDATE incomes SET isReceived = :isReceived WHERE id = :id")
    suspend fun updateReceivedStatus(id: Long, isReceived: Boolean)

    @Query("UPDATE incomes SET isReceived = 0 WHERE type = 'RECURRENT'")
    suspend fun resetAllReceivedStatus()
}

