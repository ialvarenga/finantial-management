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
import java.util.Date
import java.util.Locale

/**
 * Represents a single installment in the payment schedule
 */
data class InstallmentScheduleItem(
    val transaction: Transaction,
    val installmentNumber: Int,
    val formattedDate: String, // e.g., "Jan 2024"
    val formattedAmount: String, // e.g., "R$ 100.00"
    val status: String,
    val statusDisplayText: String, // e.g., "✓ Paid", "⏱ Due", "✗ Cancelled"
    val isPaid: Boolean,
    val isExpected: Boolean,
    val isCancelled: Boolean
)

/**
 * UI state for the Installment Detail screen
 */
data class InstallmentDetailUiState(
    val parentTransaction: Transaction? = null,
    val summary: InstallmentSummary? = null,
    val scheduleItems: List<InstallmentScheduleItem> = emptyList(),
    val formattedTotalAmount: String = "",
    val formattedPaidAmount: String = "",
    val formattedRemainingAmount: String = "",
    val progressPercentage: Float = 0f,
    val progressText: String = "", // e.g., "3/12 paid"
    val purchaseDate: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * ViewModel for the Installment Detail screen.
 * Shows detailed payment schedule for an installment purchase.
 */
class InstallmentDetailViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _parentTransactionId = MutableStateFlow<Long?>(null)

    private val _uiState = MutableStateFlow(InstallmentDetailUiState())
    val uiState: StateFlow<InstallmentDetailUiState> = _uiState.asStateFlow()

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val monthYearFormatter = SimpleDateFormat("MMM yyyy", Locale("pt", "BR"))

    /**
     * The parent transaction
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val parentTransaction: StateFlow<Transaction?> = _parentTransactionId
        .filterNotNull()
        .flatMapLatest { id ->
            transactionRepository.getTransactionById(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Child installment transactions
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val childTransactions: StateFlow<List<Transaction>> = _parentTransactionId
        .filterNotNull()
        .flatMapLatest { id ->
            transactionRepository.getInstallmentChildren(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Combine flows into UI state
        viewModelScope.launch {
            combine(
                parentTransaction,
                childTransactions
            ) { parent, children ->
                if (parent != null) {
                    buildUiState(parent, children)
                } else {
                    InstallmentDetailUiState(isLoading = true)
                }
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /**
     * Build the UI state from parent and child transactions
     */
    private suspend fun buildUiState(
        parent: Transaction,
        children: List<Transaction>
    ): InstallmentDetailUiState {
        val summary = transactionRepository.getInstallmentSummary(parent.id)

        val scheduleItems = children
            .sortedBy { it.installmentNumber }
            .map { child ->
                val isPaid = child.status == TransactionStatus.COMPLETED
                val isCancelled = child.status == TransactionStatus.CANCELLED
                val isExpected = child.status == TransactionStatus.EXPECTED

                val statusDisplayText = when {
                    isPaid -> "✓ Pago"
                    isCancelled -> "✗ Cancelado"
                    isExpected -> "⏱ A vencer"
                    else -> child.status
                }

                InstallmentScheduleItem(
                    transaction = child,
                    installmentNumber = child.installmentNumber ?: 0,
                    formattedDate = monthYearFormatter.format(Date(child.date)),
                    formattedAmount = "R$ %.2f".format(child.amount),
                    status = child.status,
                    statusDisplayText = statusDisplayText,
                    isPaid = isPaid,
                    isExpected = isExpected,
                    isCancelled = isCancelled
                )
            }

        val progressPercentage = if (summary != null && summary.totalInstallments > 0) {
            (summary.completedCount.toFloat() / summary.totalInstallments) * 100f
        } else 0f

        val progressText = "${summary?.completedCount ?: 0}/${summary?.totalInstallments ?: 0} pago"

        return InstallmentDetailUiState(
            parentTransaction = parent,
            summary = summary,
            scheduleItems = scheduleItems,
            formattedTotalAmount = "R$ %.2f".format(summary?.totalAmount ?: 0.0),
            formattedPaidAmount = "R$ %.2f".format(summary?.paidAmount ?: 0.0),
            formattedRemainingAmount = "R$ %.2f".format(summary?.remainingAmount ?: 0.0),
            progressPercentage = progressPercentage,
            progressText = progressText,
            purchaseDate = dateFormatter.format(Date(parent.date)),
            isLoading = false,
            error = null
        )
    }

    /**
     * Load a specific installment purchase by parent transaction ID
     */
    fun loadInstallment(parentTransactionId: Long) {
        _parentTransactionId.value = parentTransactionId
    }

    /**
     * Cancel all remaining (expected) installments
     */
    fun cancelRemainingInstallments() {
        viewModelScope.launch {
            try {
                val parentId = _parentTransactionId.value
                    ?: throw IllegalStateException("No installment selected")

                transactionRepository.cancelInstallment(parentId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to cancel installments: ${e.message}") }
            }
        }
    }

    /**
     * Mark a specific installment as paid
     */
    fun markInstallmentAsPaid(childTransactionId: Long) {
        viewModelScope.launch {
            try {
                transactionRepository.updateTransactionStatus(
                    childTransactionId,
                    TransactionStatus.COMPLETED
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to mark as paid: ${e.message}") }
            }
        }
    }

    /**
     * Mark a specific installment as expected (unpaid)
     */
    fun markInstallmentAsExpected(childTransactionId: Long) {
        viewModelScope.launch {
            try {
                transactionRepository.updateTransactionStatus(
                    childTransactionId,
                    TransactionStatus.EXPECTED
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update status: ${e.message}") }
            }
        }
    }

    /**
     * Get count of remaining (expected) installments
     */
    fun getRemainingCount(): Int {
        return _uiState.value.scheduleItems.count { it.isExpected }
    }

    /**
     * Get count of paid (completed) installments
     */
    fun getPaidCount(): Int {
        return _uiState.value.scheduleItems.count { it.isPaid }
    }

    /**
     * Check if there are any remaining installments to cancel
     */
    fun hasRemainingInstallments(): Boolean {
        return getRemainingCount() > 0
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * Factory for creating InstallmentDetailViewModel instances
 */
class InstallmentDetailViewModelFactory(
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InstallmentDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InstallmentDetailViewModel(transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

