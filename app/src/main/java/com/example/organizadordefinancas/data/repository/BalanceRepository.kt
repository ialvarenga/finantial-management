package com.example.organizadordefinancas.data.repository

import com.example.organizadordefinancas.data.dao.BalanceDao
import com.example.organizadordefinancas.data.dao.TransactionDao
import com.example.organizadordefinancas.data.model.Balance
import com.example.organizadordefinancas.data.model.BalanceTypes
import com.example.organizadordefinancas.data.model.Transaction
import com.example.organizadordefinancas.data.model.TransactionStatus
import com.example.organizadordefinancas.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Balance entities (both account main balances and pools/caixinhas).
 * Handles CRUD operations, balance adjustments, and transfers between balances.
 */
class BalanceRepository(
    private val balanceDao: BalanceDao,
    private val transactionDao: TransactionDao
) {
    // ==================== Read Operations ====================

    /**
     * Get all balances
     */
    fun getAllBalances(): Flow<List<Balance>> = balanceDao.getAllBalances()

    /**
     * Get only active balances
     */
    fun getActiveBalances(): Flow<List<Balance>> = balanceDao.getActiveBalances()

    /**
     * Get a specific balance by ID
     */
    fun getBalanceById(id: Long): Flow<Balance?> = balanceDao.getBalanceById(id)

    /**
     * Get a specific balance by ID (synchronous)
     */
    suspend fun getBalanceByIdSync(id: Long): Balance? = balanceDao.getBalanceByIdSync(id)

    // ==================== Account-specific Queries ====================

    /**
     * Get all balances for a specific account
     */
    fun getBalancesByAccountId(accountId: Long): Flow<List<Balance>> =
        balanceDao.getBalancesByAccountId(accountId)

    /**
     * Get only active balances for a specific account
     */
    fun getActiveBalancesByAccountId(accountId: Long): Flow<List<Balance>> =
        balanceDao.getActiveBalancesByAccountId(accountId)

    /**
     * Get pools (caixinhas) for a specific account
     */
    fun getPoolsForAccount(accountId: Long): Flow<List<Balance>> =
        balanceDao.getPoolsForAccount(accountId)

    /**
     * Get the main balance for a specific account
     */
    fun getMainBalanceForAccount(accountId: Long): Flow<Balance?> =
        balanceDao.getMainBalanceForAccount(accountId)

    /**
     * Get the main balance for a specific account (synchronous)
     */
    suspend fun getMainBalanceForAccountSync(accountId: Long): Balance? =
        balanceDao.getMainBalanceForAccountSync(accountId)

    // ==================== Balance Calculations ====================

    /**
     * Get the sum of all pool balances for an account
     */
    fun getSumOfPoolsForAccount(accountId: Long): Flow<Double> =
        balanceDao.getSumOfPoolsForAccount(accountId)

    /**
     * Get the available balance for an account (main balance - pools)
     */
    fun getAvailableBalanceForAccount(accountId: Long): Flow<Double> =
        balanceDao.getAvailableBalanceForAccount(accountId)

    /**
     * Get the available balance for an account (synchronous)
     */
    suspend fun getAvailableBalanceForAccountSync(accountId: Long): Double =
        balanceDao.getAvailableBalanceForAccountSync(accountId)

    /**
     * Get total main balance across all accounts
     */
    fun getTotalMainBalance(): Flow<Double> = balanceDao.getTotalMainBalance()

    /**
     * Get total pool balance across all accounts
     */
    fun getTotalPoolBalance(): Flow<Double> = balanceDao.getTotalPoolBalance()

    // ==================== Write Operations ====================

    /**
     * Create a new balance
     * @return The ID of the created balance
     */
    suspend fun insertBalance(balance: Balance): Long = balanceDao.insertBalance(balance)

    /**
     * Create a new pool (caixinha) for an account
     * @return The ID of the created pool
     */
    suspend fun createPool(
        accountId: Long,
        name: String,
        goalAmount: Double? = null,
        color: Long? = null,
        icon: String? = null
    ): Long {
        val pool = Balance(
            name = name,
            accountId = accountId,
            currentBalance = 0.0,
            balanceType = BalanceTypes.POOL,
            goalAmount = goalAmount,
            color = color,
            icon = icon,
            isActive = true
        )
        return balanceDao.insertBalance(pool)
    }

    /**
     * Update an existing balance
     */
    suspend fun updateBalance(balance: Balance) = balanceDao.updateBalance(balance)

    /**
     * Delete a balance
     */
    suspend fun deleteBalance(balance: Balance) = balanceDao.deleteBalance(balance)

    /**
     * Delete a balance by ID
     */
    suspend fun deleteBalanceById(id: Long) = balanceDao.deleteBalanceById(id)

    /**
     * Set balance active/inactive status
     */
    suspend fun setBalanceActive(id: Long, isActive: Boolean) =
        balanceDao.setBalanceActive(id, isActive)

    // ==================== Balance Adjustments ====================

    /**
     * Adjust a balance by a certain amount (positive or negative)
     */
    suspend fun adjustBalance(balanceId: Long, amount: Double) =
        balanceDao.adjustBalance(balanceId, amount)

    /**
     * Set the balance to a specific amount
     */
    suspend fun setBalance(balanceId: Long, newBalance: Double) =
        balanceDao.setBalance(balanceId, newBalance)

    // ==================== Transfer Operations ====================

    /**
     * Result of a transfer operation
     */
    data class TransferResult(
        val success: Boolean,
        val expenseTransactionId: Long? = null,
        val incomeTransactionId: Long? = null,
        val errorMessage: String? = null
    )

    /**
     * Transfer money between two balances.
     * Creates two linked transactions: expense from source, income to destination.
     *
     * @param fromBalanceId Source balance ID
     * @param toBalanceId Destination balance ID
     * @param amount Amount to transfer
     * @param description Optional description for the transfer
     * @param validateSufficientBalance If true, checks that source has sufficient funds
     * @return TransferResult with transaction IDs or error message
     */
    suspend fun transferBetweenBalances(
        fromBalanceId: Long,
        toBalanceId: Long,
        amount: Double,
        description: String? = null,
        validateSufficientBalance: Boolean = true
    ): TransferResult {
        // Validate amount
        if (amount <= 0) {
            return TransferResult(
                success = false,
                errorMessage = "Transfer amount must be positive"
            )
        }

        // Validate balances exist
        val fromBalance = balanceDao.getBalanceByIdSync(fromBalanceId)
            ?: return TransferResult(
                success = false,
                errorMessage = "Source balance not found"
            )

        val toBalance = balanceDao.getBalanceByIdSync(toBalanceId)
            ?: return TransferResult(
                success = false,
                errorMessage = "Destination balance not found"
            )

        // Validate sufficient balance if required
        if (validateSufficientBalance && fromBalance.currentBalance < amount) {
            return TransferResult(
                success = false,
                errorMessage = "Insufficient balance. Available: ${fromBalance.currentBalance}, Required: $amount"
            )
        }

        val currentTime = System.currentTimeMillis()
        val transferPairId = currentTime // Use timestamp as unique transfer pair ID

        // Create expense transaction (from source)
        val expenseTransaction = Transaction(
            amount = amount,
            date = currentTime,
            balanceId = fromBalanceId,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.COMPLETED,
            category = "Transferência",
            description = description ?: "Transferência para ${toBalance.name}",
            transferPairId = transferPairId
        )
        val expenseId = transactionDao.insertTransaction(expenseTransaction)

        // Create income transaction (to destination)
        val incomeTransaction = Transaction(
            amount = amount,
            date = currentTime,
            balanceId = toBalanceId,
            type = TransactionType.INCOME,
            status = TransactionStatus.COMPLETED,
            category = "Transferência",
            description = description ?: "Transferência de ${fromBalance.name}",
            transferPairId = transferPairId
        )
        val incomeId = transactionDao.insertTransaction(incomeTransaction)

        // Update balance amounts
        balanceDao.adjustBalance(fromBalanceId, -amount)
        balanceDao.adjustBalance(toBalanceId, amount)

        return TransferResult(
            success = true,
            expenseTransactionId = expenseId,
            incomeTransactionId = incomeId
        )
    }

    /**
     * Transfer money to a pool from the account's main balance.
     * This is a convenience method for allocating funds to savings goals.
     */
    suspend fun transferToPool(
        accountId: Long,
        poolId: Long,
        amount: Double,
        description: String? = null
    ): TransferResult {
        // Get the main balance for the account
        val mainBalance = balanceDao.getMainBalanceForAccountSync(accountId)
            ?: return TransferResult(
                success = false,
                errorMessage = "Main balance not found for account"
            )

        // Verify the pool belongs to the same account
        val pool = balanceDao.getBalanceByIdSync(poolId)
        if (pool == null || pool.accountId != accountId || pool.balanceType != BalanceTypes.POOL) {
            return TransferResult(
                success = false,
                errorMessage = "Invalid pool for this account"
            )
        }

        return transferBetweenBalances(
            fromBalanceId = mainBalance.id,
            toBalanceId = poolId,
            amount = amount,
            description = description ?: "Alocação para ${pool.name}",
            validateSufficientBalance = true
        )
    }

    /**
     * Withdraw money from a pool back to the account's main balance.
     */
    suspend fun withdrawFromPool(
        accountId: Long,
        poolId: Long,
        amount: Double,
        description: String? = null
    ): TransferResult {
        // Get the main balance for the account
        val mainBalance = balanceDao.getMainBalanceForAccountSync(accountId)
            ?: return TransferResult(
                success = false,
                errorMessage = "Main balance not found for account"
            )

        // Verify the pool belongs to the same account
        val pool = balanceDao.getBalanceByIdSync(poolId)
        if (pool == null || pool.accountId != accountId || pool.balanceType != BalanceTypes.POOL) {
            return TransferResult(
                success = false,
                errorMessage = "Invalid pool for this account"
            )
        }

        return transferBetweenBalances(
            fromBalanceId = poolId,
            toBalanceId = mainBalance.id,
            amount = amount,
            description = description ?: "Resgate de ${pool.name}",
            validateSufficientBalance = true
        )
    }
}

