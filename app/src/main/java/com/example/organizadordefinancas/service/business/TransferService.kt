package com.example.organizadordefinancas.service.business

import com.example.organizadordefinancas.data.model.Balance
import com.example.organizadordefinancas.data.model.BalanceTypes
import com.example.organizadordefinancas.data.model.Transaction
import com.example.organizadordefinancas.data.repository.BalanceRepository
import kotlinx.coroutines.flow.Flow

/**
 * Service class for handling transfers between balances.
 * Provides a clean API for moving money between accounts, pools, and other balances.
 *
 * Transfer Model:
 * - Creates two linked transactions (expense from source, income to destination)
 * - Transactions are linked via transfer_pair_id
 * - Both balances are updated atomically
 */
class TransferService(
    private val balanceRepository: BalanceRepository
) {
    // ==================== Data Classes ====================

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
     * Details about a transfer for display
     */
    data class TransferDetails(
        val expenseTransaction: Transaction,
        val incomeTransaction: Transaction,
        val fromBalanceName: String,
        val toBalanceName: String,
        val amount: Double,
        val date: Long
    )

    /**
     * Preview of a transfer before execution
     */
    data class TransferPreview(
        val fromBalance: Balance,
        val toBalance: Balance,
        val amount: Double,
        val fromBalanceAfter: Double,
        val toBalanceAfter: Double,
        val isValid: Boolean,
        val validationMessage: String?
    )

    // ==================== Transfer Operations ====================

    /**
     * Transfer money between any two balances.
     *
     * @param fromBalanceId Source balance ID
     * @param toBalanceId Destination balance ID
     * @param amount Amount to transfer (must be positive)
     * @param description Optional description for the transfer
     * @param validateSufficientBalance Whether to check if source has enough funds
     * @return TransferResult with transaction IDs or error
     */
    suspend fun transferBetweenBalances(
        fromBalanceId: Long,
        toBalanceId: Long,
        amount: Double,
        description: String? = null,
        validateSufficientBalance: Boolean = true
    ): TransferResult {
        // Validate same balance transfer
        if (fromBalanceId == toBalanceId) {
            return TransferResult(
                success = false,
                errorMessage = "Não é possível transferir para a mesma conta"
            )
        }

        // Validate amount
        if (amount <= 0) {
            return TransferResult(
                success = false,
                errorMessage = "O valor da transferência deve ser positivo"
            )
        }

        // Delegate to repository
        val result = balanceRepository.transferBetweenBalances(
            fromBalanceId = fromBalanceId,
            toBalanceId = toBalanceId,
            amount = amount,
            description = description,
            validateSufficientBalance = validateSufficientBalance
        )

        return TransferResult(
            success = result.success,
            expenseTransactionId = result.expenseTransactionId,
            incomeTransactionId = result.incomeTransactionId,
            errorMessage = result.errorMessage
        )
    }

    /**
     * Transfer money to a pool (caixinha) from the account's main balance.
     *
     * @param accountId The account ID
     * @param poolId The pool ID
     * @param amount Amount to transfer
     * @param description Optional description
     * @return TransferResult
     */
    suspend fun transferToPool(
        accountId: Long,
        poolId: Long,
        amount: Double,
        description: String? = null
    ): TransferResult {
        val result = balanceRepository.transferToPool(
            accountId = accountId,
            poolId = poolId,
            amount = amount,
            description = description
        )

        return TransferResult(
            success = result.success,
            expenseTransactionId = result.expenseTransactionId,
            incomeTransactionId = result.incomeTransactionId,
            errorMessage = result.errorMessage
        )
    }

    /**
     * Withdraw money from a pool back to the account's main balance.
     *
     * @param accountId The account ID
     * @param poolId The pool ID
     * @param amount Amount to withdraw
     * @param description Optional description
     * @return TransferResult
     */
    suspend fun withdrawFromPool(
        accountId: Long,
        poolId: Long,
        amount: Double,
        description: String? = null
    ): TransferResult {
        val result = balanceRepository.withdrawFromPool(
            accountId = accountId,
            poolId = poolId,
            amount = amount,
            description = description
        )

        return TransferResult(
            success = result.success,
            expenseTransactionId = result.expenseTransactionId,
            incomeTransactionId = result.incomeTransactionId,
            errorMessage = result.errorMessage
        )
    }

    // ==================== Preview & Validation ====================

    /**
     * Preview a transfer before executing it.
     * Shows what balances will look like after the transfer.
     *
     * @param fromBalanceId Source balance ID
     * @param toBalanceId Destination balance ID
     * @param amount Amount to transfer
     * @return TransferPreview with validation info
     */
    suspend fun previewTransfer(
        fromBalanceId: Long,
        toBalanceId: Long,
        amount: Double
    ): TransferPreview? {
        val fromBalance = balanceRepository.getBalanceByIdSync(fromBalanceId) ?: return null
        val toBalance = balanceRepository.getBalanceByIdSync(toBalanceId) ?: return null

        val fromBalanceAfter = fromBalance.currentBalance - amount
        val toBalanceAfter = toBalance.currentBalance + amount

        val isValid: Boolean
        val validationMessage: String?

        when {
            amount <= 0 -> {
                isValid = false
                validationMessage = "O valor deve ser positivo"
            }
            fromBalanceId == toBalanceId -> {
                isValid = false
                validationMessage = "Não é possível transferir para a mesma conta"
            }
            fromBalanceAfter < 0 -> {
                isValid = false
                validationMessage = "Saldo insuficiente. Disponível: R$ ${"%.2f".format(fromBalance.currentBalance)}"
            }
            else -> {
                isValid = true
                validationMessage = null
            }
        }

        return TransferPreview(
            fromBalance = fromBalance,
            toBalance = toBalance,
            amount = amount,
            fromBalanceAfter = fromBalanceAfter,
            toBalanceAfter = toBalanceAfter,
            isValid = isValid,
            validationMessage = validationMessage
        )
    }

    /**
     * Check if a transfer is possible without executing it.
     *
     * @param fromBalanceId Source balance ID
     * @param toBalanceId Destination balance ID
     * @param amount Amount to transfer
     * @return Pair of (isValid, errorMessage)
     */
    suspend fun validateTransfer(
        fromBalanceId: Long,
        toBalanceId: Long,
        amount: Double
    ): Pair<Boolean, String?> {
        val preview = previewTransfer(fromBalanceId, toBalanceId, amount)
            ?: return Pair(false, "Conta não encontrada")

        return Pair(preview.isValid, preview.validationMessage)
    }

    // ==================== Balance Queries ====================

    /**
     * Get available balances for transfer (source selection).
     * Returns only active balances with positive balance.
     */
    fun getSourceBalances(): Flow<List<Balance>> = balanceRepository.getActiveBalances()

    /**
     * Get available balances for receiving transfer (destination selection).
     * Returns all active balances.
     */
    fun getDestinationBalances(): Flow<List<Balance>> = balanceRepository.getActiveBalances()

    /**
     * Get pools for a specific account.
     */
    fun getPoolsForAccount(accountId: Long): Flow<List<Balance>> =
        balanceRepository.getPoolsForAccount(accountId)

    /**
     * Get the main balance for an account.
     */
    fun getMainBalanceForAccount(accountId: Long): Flow<Balance?> =
        balanceRepository.getMainBalanceForAccount(accountId)

    /**
     * Get available balance for an account (main balance minus pools).
     */
    fun getAvailableBalance(accountId: Long): Flow<Double> =
        balanceRepository.getAvailableBalanceForAccount(accountId)

    // ==================== Pool Operations ====================

    /**
     * Quick allocate - transfer funds to a pool to meet a goal.
     *
     * @param accountId The account ID
     * @param poolId The pool ID
     * @param targetAmount The target amount to have in the pool
     * @return TransferResult or null if already at/above target
     */
    suspend fun allocateToPoolGoal(
        accountId: Long,
        poolId: Long,
        targetAmount: Double
    ): TransferResult? {
        val pool = balanceRepository.getBalanceByIdSync(poolId) ?: return TransferResult(
            success = false,
            errorMessage = "Pool não encontrado"
        )

        if (pool.balanceType != BalanceTypes.POOL) {
            return TransferResult(
                success = false,
                errorMessage = "Saldo selecionado não é uma caixinha"
            )
        }

        val amountNeeded = targetAmount - pool.currentBalance
        if (amountNeeded <= 0) {
            return null // Already at or above target
        }

        return transferToPool(
            accountId = accountId,
            poolId = poolId,
            amount = amountNeeded,
            description = "Alocação para meta: R$ ${"%.2f".format(targetAmount)}"
        )
    }

    /**
     * Empty a pool - withdraw all funds back to main balance.
     *
     * @param accountId The account ID
     * @param poolId The pool ID
     * @return TransferResult
     */
    suspend fun emptyPool(
        accountId: Long,
        poolId: Long
    ): TransferResult {
        val pool = balanceRepository.getBalanceByIdSync(poolId) ?: return TransferResult(
            success = false,
            errorMessage = "Pool não encontrado"
        )

        if (pool.currentBalance <= 0) {
            return TransferResult(
                success = false,
                errorMessage = "Pool já está vazio"
            )
        }

        return withdrawFromPool(
            accountId = accountId,
            poolId = poolId,
            amount = pool.currentBalance,
            description = "Resgate total da caixinha"
        )
    }
}

