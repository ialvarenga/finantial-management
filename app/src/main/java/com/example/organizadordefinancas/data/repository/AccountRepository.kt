package com.example.organizadordefinancas.data.repository

import com.example.organizadordefinancas.data.dao.AccountDao
import com.example.organizadordefinancas.data.dao.BalanceDao
import com.example.organizadordefinancas.data.model.Account
import com.example.organizadordefinancas.data.model.Balance
import com.example.organizadordefinancas.data.model.BalanceTypes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Repository for managing Account entities.
 * Handles CRUD operations and provides account-related business logic.
 */
class AccountRepository(
    private val accountDao: AccountDao,
    private val balanceDao: BalanceDao
) {
    // ==================== Read Operations ====================

    /**
     * Get all accounts ordered by bank name and account name
     */
    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAllAccounts()

    /**
     * Get only active accounts
     */
    fun getActiveAccounts(): Flow<List<Account>> = accountDao.getActiveAccounts()

    /**
     * Get a specific account by ID
     */
    fun getAccountById(id: Long): Flow<Account?> = accountDao.getAccountById(id)

    /**
     * Get a specific account by ID (synchronous)
     */
    suspend fun getAccountByIdSync(id: Long): Account? = accountDao.getAccountByIdSync(id)

    /**
     * Get accounts filtered by bank name
     */
    fun getAccountsByBankName(bankName: String): Flow<List<Account>> =
        accountDao.getAccountsByBankName(bankName)

    /**
     * Get all unique bank names for filtering/grouping
     */
    fun getAllBankNames(): Flow<List<String>> = accountDao.getAllBankNames()

    // ==================== Account with Balances ====================

    /**
     * Data class representing an account with all its balances
     */
    data class AccountWithBalances(
        val account: Account,
        val mainBalance: Balance?,
        val pools: List<Balance>,
        val totalBalance: Double,
        val availableBalance: Double
    )

    /**
     * Get an account with all its balances (main + pools)
     */
    fun getAccountWithBalances(accountId: Long): Flow<AccountWithBalances?> {
        return combine(
            accountDao.getAccountById(accountId),
            balanceDao.getBalancesByAccountId(accountId),
            balanceDao.getAvailableBalanceForAccount(accountId)
        ) { account, balances, availableBalance ->
            account?.let {
                val mainBalance = balances.find { b -> b.balanceType == BalanceTypes.ACCOUNT }
                val pools = balances.filter { b -> b.balanceType == BalanceTypes.POOL && b.isActive }
                val totalBalance = balances.filter { b -> b.isActive }.sumOf { b -> b.currentBalance }

                AccountWithBalances(
                    account = account,
                    mainBalance = mainBalance,
                    pools = pools,
                    totalBalance = totalBalance,
                    availableBalance = availableBalance
                )
            }
        }
    }

    /**
     * Get all accounts with their balances
     */
    fun getAllAccountsWithBalances(): Flow<List<AccountWithBalances>> {
        return combine(
            accountDao.getActiveAccounts(),
            balanceDao.getActiveBalances()
        ) { accounts, allBalances ->
            accounts.map { account ->
                val accountBalances = allBalances.filter { it.accountId == account.id }
                val mainBalance = accountBalances.find { it.balanceType == BalanceTypes.ACCOUNT }
                val pools = accountBalances.filter { it.balanceType == BalanceTypes.POOL }
                val totalBalance = accountBalances.sumOf { it.currentBalance }
                val poolsSum = pools.sumOf { it.currentBalance }
                val availableBalance = (mainBalance?.currentBalance ?: 0.0) - poolsSum

                AccountWithBalances(
                    account = account,
                    mainBalance = mainBalance,
                    pools = pools,
                    totalBalance = totalBalance,
                    availableBalance = availableBalance
                )
            }
        }
    }

    // ==================== Balance Calculations ====================

    /**
     * Get the total balance across all accounts (main balances only)
     */
    fun getTotalMainBalance(): Flow<Double> = balanceDao.getTotalMainBalance()

    /**
     * Get the total amount reserved in pools across all accounts
     */
    fun getTotalPoolBalance(): Flow<Double> = balanceDao.getTotalPoolBalance()

    /**
     * Get the total available balance across all accounts
     * (Total main balance - Total pools)
     */
    fun getTotalAvailableBalance(): Flow<Double> {
        return combine(
            balanceDao.getTotalMainBalance(),
            balanceDao.getTotalPoolBalance()
        ) { mainTotal, poolTotal ->
            mainTotal - poolTotal
        }
    }

    /**
     * Get available balance for a specific account
     */
    fun getAvailableBalanceForAccount(accountId: Long): Flow<Double> =
        balanceDao.getAvailableBalanceForAccount(accountId)

    /**
     * Get available balance for a specific account (synchronous)
     */
    suspend fun getAvailableBalanceForAccountSync(accountId: Long): Double =
        balanceDao.getAvailableBalanceForAccountSync(accountId)

    // ==================== Write Operations ====================

    /**
     * Create a new account with its main balance
     * @return The ID of the created account
     */
    suspend fun createAccount(account: Account): Long {
        val accountId = accountDao.insertAccount(account)

        // Create the main balance for the account
        val mainBalance = Balance(
            name = "Principal",
            accountId = accountId,
            currentBalance = 0.0,
            balanceType = BalanceTypes.ACCOUNT,
            isActive = true
        )
        balanceDao.insertBalance(mainBalance)

        return accountId
    }

    /**
     * Create a new account with initial balance
     * @return The ID of the created account
     */
    suspend fun createAccountWithInitialBalance(account: Account, initialBalance: Double): Long {
        val accountId = accountDao.insertAccount(account)

        // Create the main balance for the account with initial value
        val mainBalance = Balance(
            name = "Principal",
            accountId = accountId,
            currentBalance = initialBalance,
            balanceType = BalanceTypes.ACCOUNT,
            isActive = true
        )
        balanceDao.insertBalance(mainBalance)

        return accountId
    }

    /**
     * Update an existing account
     */
    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account)

    /**
     * Delete an account (will cascade delete all balances)
     */
    suspend fun deleteAccount(account: Account) = accountDao.deleteAccount(account)

    /**
     * Delete an account by ID (will cascade delete all balances)
     */
    suspend fun deleteAccountById(id: Long) = accountDao.deleteAccountById(id)

    /**
     * Set account active/inactive status
     */
    suspend fun setAccountActive(id: Long, isActive: Boolean) =
        accountDao.setAccountActive(id, isActive)

    /**
     * Deactivate an account (soft delete)
     */
    suspend fun deactivateAccount(id: Long) =
        accountDao.setAccountActive(id, false)
}

