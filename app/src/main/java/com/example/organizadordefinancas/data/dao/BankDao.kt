package com.example.organizadordefinancas.data.dao

import androidx.room.*
import com.example.organizadordefinancas.data.model.Bank
import kotlinx.coroutines.flow.Flow

@Dao
interface BankDao {
    @Query("SELECT * FROM banks ORDER BY name ASC")
    fun getAllBanks(): Flow<List<Bank>>

    @Query("SELECT * FROM banks WHERE id = :id")
    fun getBankById(id: Long): Flow<Bank?>

    @Query("SELECT SUM(balance) FROM banks")
    fun getTotalBalance(): Flow<Double?>

    @Query("SELECT SUM(savingsBalance) FROM banks")
    fun getTotalSavingsBalance(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBank(bank: Bank): Long

    @Update
    suspend fun updateBank(bank: Bank)

    @Delete
    suspend fun deleteBank(bank: Bank)

    @Query("DELETE FROM banks WHERE id = :id")
    suspend fun deleteBankById(id: Long)
}

