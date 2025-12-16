package com.example.organizadordefinancas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.organizadordefinancas.data.model.Account
import com.example.organizadordefinancas.data.model.Balance
import com.example.organizadordefinancas.data.model.Transaction
import com.example.organizadordefinancas.data.repository.AccountRepository
import com.example.organizadordefinancas.data.repository.AccountRepository.AccountWithBalances
import com.example.organizadordefinancas.data.repository.BalanceRepository
import com.example.organizadordefinancas.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI state for the Account Detail screen
 */
data class AccountDetailUiState(
    val accountWithBalances: AccountWithBalances? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * ViewModel for the Account Detail screen.
 * Shows account details, main balance, pools, and recent transactions.
 */
class AccountDetailViewModel(
    private val accountRepository: AccountRepository,
    private val balanceRepository: BalanceRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _accountId = MutableStateFlow<Long?>(null)

    private val _uiState = MutableStateFlow(AccountDetailUiState())
    val uiState: StateFlow<AccountDetailUiState> = _uiState.asStateFlow()

    /**
     * The current account with its balances
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val accountWithBalances: StateFlow<AccountWithBalances?> = _accountId
        .filterNotNull()
        .flatMapLatest { id ->
            accountRepository.getAccountWithBalances(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Pools (caixinhas) for the current account
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val pools: StateFlow<List<Balance>> = _accountId
        .filterNotNull()
        .flatMapLatest { id ->
            balanceRepository.getPoolsForAccount(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Recent transactions for the main balance
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val recentTransactions: StateFlow<List<Transaction>> = accountWithBalances
        .filterNotNull()
        .flatMapLatest { account ->
            account.mainBalance?.let { balance ->
                transactionRepository.getTransactionsByBalanceId(balance.id)
                    .map { it.take(10) }
            } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Available balance for the current account
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val availableBalance: StateFlow<Double> = _accountId
        .filterNotNull()
        .flatMapLatest { id ->
            accountRepository.getAvailableBalanceForAccount(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        // Combine flows into UI state
        viewModelScope.launch {
            combine(
                accountWithBalances,
                recentTransactions
            ) { account, transactions ->
                AccountDetailUiState(
                    accountWithBalances = account,
                    recentTransactions = transactions,
                    isLoading = false,
                    error = null
                )
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /**
     * Load a specific account by ID
     */
    fun loadAccount(accountId: Long) {
        _accountId.value = accountId
    }

    /**
     * Update the current account
     */
    fun updateAccount(account: Account) {
        viewModelScope.launch {
            try {
                accountRepository.updateAccount(account)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update account: ${e.message}") }
            }
        }
    }

    /**
     * Create a new pool for the current account
     */
    fun createPool(
        name: String,
        goalAmount: Double? = null,
        color: Long? = null,
        icon: String? = null
    ) {
        viewModelScope.launch {
            try {
                val accountId = _accountId.value
                    ?: throw IllegalStateException("No account selected")

                balanceRepository.createPool(
                    accountId = accountId,
                    name = name,
                    goalAmount = goalAmount,
                    color = color,
                    icon = icon
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to create pool: ${e.message}") }
            }
        }
    }

    /**
     * Transfer money to a pool
     */
    fun transferToPool(poolId: Long, amount: Double, description: String? = null) {
        viewModelScope.launch {
            try {
                val accountId = _accountId.value
                    ?: throw IllegalStateException("No account selected")

                val result = balanceRepository.transferToPool(
                    accountId = accountId,
                    poolId = poolId,
                    amount = amount,
                    description = description
                )

                if (!result.success) {
                    _uiState.update { it.copy(error = result.errorMessage) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to transfer: ${e.message}") }
            }
        }
    }

    /**
     * Withdraw money from a pool
     */
    fun withdrawFromPool(poolId: Long, amount: Double, description: String? = null) {
        viewModelScope.launch {
            try {
                val accountId = _accountId.value
                    ?: throw IllegalStateException("No account selected")

                val result = balanceRepository.withdrawFromPool(
                    accountId = accountId,
                    poolId = poolId,
                    amount = amount,
                    description = description
                )

                if (!result.success) {
                    _uiState.update { it.copy(error = result.errorMessage) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to withdraw: ${e.message}") }
            }
        }
    }

    /**
     * Delete a pool
     */
    fun deletePool(pool: Balance) {
        viewModelScope.launch {
            try {
                balanceRepository.deleteBalance(pool)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete pool: ${e.message}") }
            }
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * Factory for creating AccountDetailViewModel instances
 */
class AccountDetailViewModelFactory(
    private val accountRepository: AccountRepository,
    private val balanceRepository: BalanceRepository,
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountDetailViewModel(
                accountRepository,
                balanceRepository,
                transactionRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

