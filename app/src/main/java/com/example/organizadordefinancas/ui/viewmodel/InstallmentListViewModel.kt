package com.example.organizadordefinancas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.organizadordefinancas.data.model.Transaction
import com.example.organizadordefinancas.data.model.TransactionStatus
import com.example.organizadordefinancas.data.repository.TransactionRepository
import com.example.organizadordefinancas.data.repository.TransactionRepository.InstallmentSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Display data for an installment purchase
 */
data class InstallmentDisplayItem(
    val parentTransaction: Transaction,
    val summary: InstallmentSummary?,
    val progressPercentage: Float,
    val formattedProgress: String, // e.g., "3/12 paid"
    val formattedTotal: String, // e.g., "12x R$100"
    val formattedRemaining: String // e.g., "R$900 remaining"
)

/**
 * UI state for the Installment List screen
 */
data class InstallmentListUiState(
    val installments: List<InstallmentDisplayItem> = emptyList(),
    val filter: InstallmentFilter = InstallmentFilter.ALL,
    val sortBy: InstallmentSortBy = InstallmentSortBy.REMAINING_AMOUNT,
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * Filter options for installment list
 */
enum class InstallmentFilter {
    ALL,           // All installment purchases
    IN_PROGRESS,   // Still has expected payments
    COMPLETED      // All payments completed
}

/**
 * Sort options for installment list
 */
enum class InstallmentSortBy {
    REMAINING_AMOUNT,
    PURCHASE_DATE,
    TOTAL_AMOUNT,
    INSTALLMENT_COUNT
}

/**
 * ViewModel for the Installment List screen.
 * Shows active installment purchases and their progress.
 */
class InstallmentListViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(InstallmentFilter.ALL)
    private val _sortBy = MutableStateFlow(InstallmentSortBy.REMAINING_AMOUNT)

    private val _uiState = MutableStateFlow(InstallmentListUiState())
    val uiState: StateFlow<InstallmentListUiState> = _uiState.asStateFlow()

    /**
     * All installment parent transactions
     */
    val installmentParents: StateFlow<List<Transaction>> =
        transactionRepository.getActiveInstallmentParents()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Build UI state from installment data
        viewModelScope.launch {
            combine(
                installmentParents,
                _filter,
                _sortBy
            ) { parents, filter, sortBy ->
                val installmentItems = parents.mapNotNull { parent ->
                    buildInstallmentDisplayItem(parent)
                }

                // Apply filter
                val filteredItems = when (filter) {
                    InstallmentFilter.ALL -> installmentItems
                    InstallmentFilter.IN_PROGRESS -> installmentItems.filter { it.summary?.expectedCount ?: 0 > 0 }
                    InstallmentFilter.COMPLETED -> installmentItems.filter { it.summary?.expectedCount == 0 }
                }

                // Apply sort
                val sortedItems = when (sortBy) {
                    InstallmentSortBy.REMAINING_AMOUNT ->
                        filteredItems.sortedByDescending { it.summary?.remainingAmount ?: 0.0 }
                    InstallmentSortBy.PURCHASE_DATE ->
                        filteredItems.sortedByDescending { it.parentTransaction.date }
                    InstallmentSortBy.TOTAL_AMOUNT ->
                        filteredItems.sortedByDescending { it.parentTransaction.amount }
                    InstallmentSortBy.INSTALLMENT_COUNT ->
                        filteredItems.sortedByDescending { it.parentTransaction.totalInstallments }
                }

                InstallmentListUiState(
                    installments = sortedItems,
                    filter = filter,
                    sortBy = sortBy,
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
     * Build display item for an installment purchase
     */
    private suspend fun buildInstallmentDisplayItem(parent: Transaction): InstallmentDisplayItem? {
        val summary = transactionRepository.getInstallmentSummary(parent.id)
        if (summary == null) return null

        val progressPercentage = if (summary.totalInstallments > 0) {
            (summary.completedCount.toFloat() / summary.totalInstallments) * 100f
        } else 0f

        val formattedProgress = "${summary.completedCount}/${summary.totalInstallments} paid"
        val formattedTotal = "${summary.totalInstallments}x R$ %.2f".format(summary.installmentAmount)
        val formattedRemaining = "R$ %.2f remaining".format(summary.remainingAmount)

        return InstallmentDisplayItem(
            parentTransaction = parent,
            summary = summary,
            progressPercentage = progressPercentage,
            formattedProgress = formattedProgress,
            formattedTotal = formattedTotal,
            formattedRemaining = formattedRemaining
        )
    }

    // ==================== Filter & Sort ====================

    /**
     * Set filter for installments
     */
    fun setFilter(filter: InstallmentFilter) {
        _filter.value = filter
    }

    /**
     * Set sort order
     */
    fun setSortBy(sortBy: InstallmentSortBy) {
        _sortBy.value = sortBy
    }

    /**
     * Show all installments
     */
    fun showAll() {
        setFilter(InstallmentFilter.ALL)
    }

    /**
     * Show only in-progress installments
     */
    fun showInProgress() {
        setFilter(InstallmentFilter.IN_PROGRESS)
    }

    /**
     * Show only completed installments
     */
    fun showCompleted() {
        setFilter(InstallmentFilter.COMPLETED)
    }

    // ==================== Actions ====================

    /**
     * Cancel remaining installments for a purchase
     */
    fun cancelInstallment(parentTransactionId: Long) {
        viewModelScope.launch {
            try {
                transactionRepository.cancelInstallment(parentTransactionId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to cancel installment: ${e.message}") }
            }
        }
    }

    /**
     * Get total remaining amount across all installments
     */
    fun getTotalRemainingAmount(): Double {
        return _uiState.value.installments.sumOf { it.summary?.remainingAmount ?: 0.0 }
    }

    /**
     * Get count of active installments (with remaining payments)
     */
    fun getActiveInstallmentCount(): Int {
        return _uiState.value.installments.count { (it.summary?.expectedCount ?: 0) > 0 }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * Factory for creating InstallmentListViewModel instances
 */
class InstallmentListViewModelFactory(
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InstallmentListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InstallmentListViewModel(transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

