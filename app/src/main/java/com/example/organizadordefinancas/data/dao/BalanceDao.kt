package com.example.organizadordefinancas.data.dao

import androidx.room.*
import com.example.organizadordefinancas.data.model.Balance
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceDao {
    // Basic CRUD operations
    @Query("SELECT * FROM balances ORDER BY balance_type ASC, name ASC")
    fun getAllBalances(): Flow<List<Balance>>

    @Query("SELECT * FROM balances WHERE is_active = 1 ORDER BY balance_type ASC, name ASC")
    fun getActiveBalances(): Flow<List<Balance>>

    @Query("SELECT * FROM balances WHERE id = :id")
    fun getBalanceById(id: Long): Flow<Balance?>

    @Query("SELECT * FROM balances WHERE id = :id")
    suspend fun getBalanceByIdSync(id: Long): Balance?

    // Get balances by account
    @Query("SELECT * FROM balances WHERE account_id = :accountId ORDER BY balance_type ASC, name ASC")
    fun getBalancesByAccountId(accountId: Long): Flow<List<Balance>>

    @Query("SELECT * FROM balances WHERE account_id = :accountId AND is_active = 1 ORDER BY balance_type ASC, name ASC")
    fun getActiveBalancesByAccountId(accountId: Long): Flow<List<Balance>>

    // Get pools for an account (balance_type = "pool")
    @Query("SELECT * FROM balances WHERE account_id = :accountId AND balance_type = 'pool' AND is_active = 1 ORDER BY name ASC")
    fun getPoolsForAccount(accountId: Long): Flow<List<Balance>>

    // Get main balance for an account (balance_type = "account")
    @Query("SELECT * FROM balances WHERE account_id = :accountId AND balance_type = 'account' LIMIT 1")
    fun getMainBalanceForAccount(accountId: Long): Flow<Balance?>

    @Query("SELECT * FROM balances WHERE account_id = :accountId AND balance_type = 'account' LIMIT 1")
    suspend fun getMainBalanceForAccountSync(accountId: Long): Balance?

    // Calculate total balance for an account (main + pools)
    @Query("SELECT SUM(current_balance) FROM balances WHERE account_id = :accountId AND is_active = 1")
    fun getTotalBalanceForAccount(accountId: Long): Flow<Double?>

    // Calculate sum of all pool balances for an account
    @Query("SELECT COALESCE(SUM(current_balance), 0.0) FROM balances WHERE account_id = :accountId AND balance_type = 'pool' AND is_active = 1")
    fun getSumOfPoolsForAccount(accountId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(current_balance), 0.0) FROM balances WHERE account_id = :accountId AND balance_type = 'pool' AND is_active = 1")
    suspend fun getSumOfPoolsForAccountSync(accountId: Long): Double

    // Calculate available balance (main balance - sum of pools)
    // This returns the main balance minus the amount reserved in pools
    @Query("""
        SELECT COALESCE(
            (SELECT current_balance FROM balances WHERE account_id = :accountId AND balance_type = 'account' LIMIT 1) - 
            (SELECT COALESCE(SUM(current_balance), 0.0) FROM balances WHERE account_id = :accountId AND balance_type = 'pool' AND is_active = 1),
            0.0
        )
    """)
    fun getAvailableBalanceForAccount(accountId: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(
            (SELECT current_balance FROM balances WHERE account_id = :accountId AND balance_type = 'account' LIMIT 1) - 
            (SELECT COALESCE(SUM(current_balance), 0.0) FROM balances WHERE account_id = :accountId AND balance_type = 'pool' AND is_active = 1),
            0.0
        )
    """)
    suspend fun getAvailableBalanceForAccountSync(accountId: Long): Double

    // Calculate total across all accounts
    @Query("SELECT COALESCE(SUM(current_balance), 0.0) FROM balances WHERE balance_type = 'account' AND is_active = 1")
    fun getTotalMainBalance(): Flow<Double>

    @Query("SELECT COALESCE(SUM(current_balance), 0.0) FROM balances WHERE balance_type = 'pool' AND is_active = 1")
    fun getTotalPoolBalance(): Flow<Double>

    // Insert/Update/Delete
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBalance(balance: Balance): Long

    @Update
    suspend fun updateBalance(balance: Balance)

    @Delete
    suspend fun deleteBalance(balance: Balance)

    @Query("DELETE FROM balances WHERE id = :id")
    suspend fun deleteBalanceById(id: Long)

    // Update balance amount
    @Query("UPDATE balances SET current_balance = current_balance + :amount, updated_at = :updatedAt WHERE id = :balanceId")
    suspend fun adjustBalance(balanceId: Long, amount: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE balances SET current_balance = :newBalance, updated_at = :updatedAt WHERE id = :balanceId")
    suspend fun setBalance(balanceId: Long, newBalance: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE balances SET is_active = :isActive WHERE id = :id")
    suspend fun setBalanceActive(id: Long, isActive: Boolean)
}

