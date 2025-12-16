package com.example.organizadordefinancas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.organizadordefinancas.data.dao.CategoryTotal
import com.example.organizadordefinancas.data.model.Transaction
import com.example.organizadordefinancas.data.model.TransactionStatus
import com.example.organizadordefinancas.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Filter options for transaction list
 */
data class TransactionFilter(
    val balanceId: Long? = null,
    val billId: Long? = null,
    val type: String? = null, // "income" or "expense"
    val status: String? = null, // "completed", "expected", "cancelled"
    val category: String? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val excludeInstallmentParents: Boolean = true // Always true by default
)

/**
 * UI state for the Transaction List screen
 */
data class TransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val expensesByCategory: List<CategoryTotal> = emptyList(),
    val filter: TransactionFilter = TransactionFilter(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * ViewModel for the Transaction List screen.
 * Handles transaction listing with filters, excluding installment parents by default.
 */
class TransactionListViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(TransactionFilter())
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    private val _uiState = MutableStateFlow(TransactionListUiState())
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    /**
     * Transactions filtered by current criteria (excludes installment parents by default)
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<Transaction>> = _filter
        .flatMapLatest { filter ->
            if (filter == TransactionFilter()) {
                // Default: get all transactions excluding installment parents
                transactionRepository.getTransactionsExcludingParents()
            } else {
                transactionRepository.getFilteredTransactions(
                    balanceId = filter.balanceId,
                    billId = filter.billId,
                    type = filter.type,
                    status = filter.status,
                    category = filter.category,
                    startDate = filter.startDate,
                    endDate = filter.endDate
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Recent transactions for dashboard
     */
    val recentTransactions: StateFlow<List<Transaction>> =
        transactionRepository.getRecentTransactions(10)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Current month expenses
     */
    val currentMonthExpenses: StateFlow<Double> =
        transactionRepository.getCurrentMonthExpenses()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * Current month income
     */
    val currentMonthIncome: StateFlow<Double> =
        transactionRepository.getCurrentMonthIncome()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * Expenses grouped by category for the current month
     */
    val expensesByCategory: StateFlow<List<CategoryTotal>> =
        transactionRepository.getCurrentMonthExpensesByCategory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Expected (future/planned) transactions
     */
    val expectedTransactions: StateFlow<List<Transaction>> =
        transactionRepository.getExpectedTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Combine flows into UI state
        viewModelScope.launch {
            combine(
                transactions,
                currentMonthExpenses,
                currentMonthIncome,
                expensesByCategory,
                _filter
            ) { transactions, expenses, income, categories, filter ->
                TransactionListUiState(
                    transactions = transactions,
                    totalExpenses = expenses,
                    totalIncome = income,
                    expensesByCategory = categories,
                    filter = filter,
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

    // ==================== Filter Methods ====================

    /**
     * Set filter for transactions by balance
     */
    fun filterByBalance(balanceId: Long?) {
        _filter.update { it.copy(balanceId = balanceId) }
    }

    /**
     * Set filter for transactions by bill
     */
    fun filterByBill(billId: Long?) {
        _filter.update { it.copy(billId = billId) }
    }

    /**
     * Set filter for transactions by type
     */
    fun filterByType(type: String?) {
        _filter.update { it.copy(type = type) }
    }

    /**
     * Set filter for transactions by status
     */
    fun filterByStatus(status: String?) {
        _filter.update { it.copy(status = status) }
    }

    /**
     * Set filter for transactions by category
     */
    fun filterByCategory(category: String?) {
        _filter.update { it.copy(category = category) }
    }

    /**
     * Set filter for transactions by date range
     */
    fun filterByDateRange(startDate: Long?, endDate: Long?) {
        _filter.update { it.copy(startDate = startDate, endDate = endDate) }
    }

    /**
     * Filter by current month
     */
    fun filterByCurrentMonth() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        filterByDateRange(startDate, endDate)
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _filter.value = TransactionFilter()
    }

    // ==================== Transaction Actions ====================

    /**
     * Update a transaction's status
     */
    fun updateTransactionStatus(transactionId: Long, status: String) {
        viewModelScope.launch {
            try {
                transactionRepository.updateTransactionStatus(transactionId, status)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update status: ${e.message}") }
            }
        }
    }

    /**
     * Mark a transaction as completed
     */
    fun markAsCompleted(transactionId: Long) {
        updateTransactionStatus(transactionId, TransactionStatus.COMPLETED)
    }

    /**
     * Mark a transaction as cancelled
     */
    fun markAsCancelled(transactionId: Long) {
        updateTransactionStatus(transactionId, TransactionStatus.CANCELLED)
    }

    /**
     * Delete a transaction
     */
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransaction(transaction)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete transaction: ${e.message}") }
            }
        }
    }

    /**
     * Delete a transaction by ID
     */
    fun deleteTransactionById(transactionId: Long) {
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransactionById(transactionId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete transaction: ${e.message}") }
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
 * Factory for creating TransactionListViewModel instances
 */
class TransactionListViewModelFactory(
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionListViewModel(transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

