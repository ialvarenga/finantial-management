package com.example.organizadordefinancas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.organizadordefinancas.data.model.Balance
import com.example.organizadordefinancas.data.model.Transaction
import com.example.organizadordefinancas.data.repository.BalanceRepository
import com.example.organizadordefinancas.data.repository.BalanceRepository.TransferResult
import com.example.organizadordefinancas.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI state for the Balance Detail screen
 */
data class BalanceDetailUiState(
    val balance: Balance? = null,
    val transactions: List<Transaction> = emptyList(),
    val progressPercentage: Float = 0f,
    val isPool: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * ViewModel for the Balance/Pool Detail screen.
 * Shows balance details, transactions, and (for pools) progress toward goal.
 */
class BalanceDetailViewModel(
    private val balanceRepository: BalanceRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _balanceId = MutableStateFlow<Long?>(null)

    private val _uiState = MutableStateFlow(BalanceDetailUiState())
    val uiState: StateFlow<BalanceDetailUiState> = _uiState.asStateFlow()

    /**
     * The current balance
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val balance: StateFlow<Balance?> = _balanceId
        .filterNotNull()
        .flatMapLatest { id ->
            balanceRepository.getBalanceById(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Transactions for this balance
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<Transaction>> = _balanceId
        .filterNotNull()
        .flatMapLatest { id ->
            transactionRepository.getTransactionsByBalanceId(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Progress percentage for pools with goals
     */
    val progressPercentage: StateFlow<Float> = balance
        .map { balance ->
            if (balance?.goalAmount != null && balance.goalAmount > 0) {
                (balance.currentBalance / balance.goalAmount * 100).toFloat().coerceIn(0f, 100f)
            } else {
                0f
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    init {
        // Combine flows into UI state
        viewModelScope.launch {
            combine(
                balance,
                transactions,
                progressPercentage
            ) { balance, transactions, progress ->
                BalanceDetailUiState(
                    balance = balance,
                    transactions = transactions,
                    progressPercentage = progress,
                    isPool = balance?.balanceType == "pool",
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
     * Load a specific balance by ID
     */
    fun loadBalance(balanceId: Long) {
        _balanceId.value = balanceId
    }

    /**
     * Update the current balance
     */
    fun updateBalance(balance: Balance) {
        viewModelScope.launch {
            try {
                balanceRepository.updateBalance(balance)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update balance: ${e.message}") }
            }
        }
    }

    /**
     * Update the goal amount for a pool
     */
    fun updateGoalAmount(goalAmount: Double?) {
        viewModelScope.launch {
            try {
                val currentBalance = balance.value
                    ?: throw IllegalStateException("No balance selected")

                balanceRepository.updateBalance(currentBalance.copy(goalAmount = goalAmount))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update goal: ${e.message}") }
            }
        }
    }

    /**
     * Transfer money from this balance to another
     */
    fun transferTo(toBalanceId: Long, amount: Double, description: String? = null): Flow<TransferResult> {
        return flow {
            try {
                val fromBalanceId = _balanceId.value
                    ?: throw IllegalStateException("No balance selected")

                val result = balanceRepository.transferBetweenBalances(
                    fromBalanceId = fromBalanceId,
                    toBalanceId = toBalanceId,
                    amount = amount,
                    description = description
                )

                if (!result.success) {
                    _uiState.update { it.copy(error = result.errorMessage) }
                }
                emit(result)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to transfer: ${e.message}") }
                emit(TransferResult(success = false, errorMessage = e.message))
            }
        }
    }

    /**
     * Transfer money to this balance from another
     */
    fun transferFrom(fromBalanceId: Long, amount: Double, description: String? = null): Flow<TransferResult> {
        return flow {
            try {
                val toBalanceId = _balanceId.value
                    ?: throw IllegalStateException("No balance selected")

                val result = balanceRepository.transferBetweenBalances(
                    fromBalanceId = fromBalanceId,
                    toBalanceId = toBalanceId,
                    amount = amount,
                    description = description
                )

                if (!result.success) {
                    _uiState.update { it.copy(error = result.errorMessage) }
                }
                emit(result)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to transfer: ${e.message}") }
                emit(TransferResult(success = false, errorMessage = e.message))
            }
        }
    }

    /**
     * Delete the current balance
     */
    fun deleteBalance() {
        viewModelScope.launch {
            try {
                val currentBalance = balance.value
                    ?: throw IllegalStateException("No balance selected")

                balanceRepository.deleteBalance(currentBalance)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete balance: ${e.message}") }
            }
        }
    }

    /**
     * Deactivate the balance (soft delete)
     */
    fun deactivateBalance() {
        viewModelScope.launch {
            try {
                val currentBalance = balance.value
                    ?: throw IllegalStateException("No balance selected")

                balanceRepository.deactivateBalance(currentBalance.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to deactivate balance: ${e.message}") }
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
 * Factory for creating BalanceDetailViewModel instances
 */
class BalanceDetailViewModelFactory(
    private val balanceRepository: BalanceRepository,
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BalanceDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BalanceDetailViewModel(balanceRepository, transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

