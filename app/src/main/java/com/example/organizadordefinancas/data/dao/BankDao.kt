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

    @Query("SELECT * FROM banks WHERE id = :id")
    suspend fun getBankByIdSync(id: Long): Bank?

    @Query("SELECT SUM(balance) FROM banks")
    fun getTotalBalance(): Flow<Double?>

    @Query("SELECT SUM(savingsBalance) FROM banks")
    fun getTotalSavingsBalance(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBank(bank: Bank): Long

    @Update
    suspend fun updateBank(bank: Bank)

    @Query("UPDATE banks SET balance = balance - :amount WHERE id = :bankId")
    suspend fun deductFromBalance(bankId: Long, amount: Double)

    @Query("UPDATE banks SET balance = balance + :amount WHERE id = :bankId")
    suspend fun addToBalance(bankId: Long, amount: Double)

    @Delete
    suspend fun deleteBank(bank: Bank)

    @Query("DELETE FROM banks WHERE id = :id")
    suspend fun deleteBankById(id: Long)
}

