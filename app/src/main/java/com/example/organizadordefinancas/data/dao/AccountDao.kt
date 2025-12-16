package com.example.organizadordefinancas.data.dao

import androidx.room.*
import com.example.organizadordefinancas.data.model.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    // Basic CRUD operations
    @Query("SELECT * FROM accounts ORDER BY bank_name ASC, name ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE is_active = 1 ORDER BY bank_name ASC, name ASC")
    fun getActiveAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getAccountById(id: Long): Flow<Account?>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountByIdSync(id: Long): Account?

    @Query("SELECT * FROM accounts WHERE bank_name = :bankName ORDER BY name ASC")
    fun getAccountsByBankName(bankName: String): Flow<List<Account>>

    @Query("SELECT DISTINCT bank_name FROM accounts ORDER BY bank_name ASC")
    fun getAllBankNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    @Query("UPDATE accounts SET is_active = :isActive WHERE id = :id")
    suspend fun setAccountActive(id: Long, isActive: Boolean)
}

