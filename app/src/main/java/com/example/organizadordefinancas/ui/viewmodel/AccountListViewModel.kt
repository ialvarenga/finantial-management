package com.example.organizadordefinancas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.organizadordefinancas.data.model.Account
import com.example.organizadordefinancas.data.repository.AccountRepository
import com.example.organizadordefinancas.data.repository.AccountRepository.AccountWithBalances
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI state for the Account List screen
 */
data class AccountListUiState(
    val accounts: List<AccountWithBalances> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalAvailableBalance: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * ViewModel for the Account List screen.
 * Manages account listing, creation, and deletion.
 */
class AccountListViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountListUiState())
    val uiState: StateFlow<AccountListUiState> = _uiState.asStateFlow()

    /**
     * All accounts with their balances
     */
    val accountsWithBalances: StateFlow<List<AccountWithBalances>> =
        accountRepository.getAllAccountsWithBalances()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Total balance across all accounts
     */
    val totalBalance: StateFlow<Double> = accountRepository.getTotalMainBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * Total available balance (total - pools)
     */
    val totalAvailableBalance: StateFlow<Double> = accountRepository.getTotalAvailableBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * Unique bank names for filtering/grouping
     */
    val bankNames: StateFlow<List<String>> = accountRepository.getAllBankNames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Combine all flows into a single UI state
        viewModelScope.launch {
            combine(
                accountRepository.getAllAccountsWithBalances(),
                accountRepository.getTotalMainBalance(),
                accountRepository.getTotalAvailableBalance()
            ) { accounts, total, available ->
                AccountListUiState(
                    accounts = accounts,
                    totalBalance = total,
                    totalAvailableBalance = available,
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
     * Create a new account with an initial balance
     */
    fun createAccount(
        name: String,
        bankName: String,
        accountType: String = "checking",
        initialBalance: Double = 0.0,
        color: Long = 0xFF03DAC5
    ) {
        viewModelScope.launch {
            try {
                val account = Account(
                    name = name,
                    bankName = bankName,
                    accountType = accountType,
                    color = color
                )
                accountRepository.createAccountWithInitialBalance(account, initialBalance)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to create account: ${e.message}") }
            }
        }
    }

    /**
     * Update an existing account
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
     * Delete an account
     */
    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            try {
                accountRepository.deleteAccount(account)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete account: ${e.message}") }
            }
        }
    }

    /**
     * Deactivate an account (soft delete)
     */
    fun deactivateAccount(account: Account) {
        viewModelScope.launch {
            try {
                accountRepository.deactivateAccount(account.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to deactivate account: ${e.message}") }
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
 * Factory for creating AccountListViewModel instances
 */
class AccountListViewModelFactory(
    private val accountRepository: AccountRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountListViewModel(accountRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

