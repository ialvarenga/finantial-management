package com.example.organizadordefinancas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.organizadordefinancas.data.model.Balance
import com.example.organizadordefinancas.data.model.Bill
import com.example.organizadordefinancas.data.model.BillStatus
import com.example.organizadordefinancas.data.model.CreditCard
import com.example.organizadordefinancas.data.model.Transaction
import com.example.organizadordefinancas.data.repository.BalanceRepository
import com.example.organizadordefinancas.data.repository.BillRepository
import com.example.organizadordefinancas.data.repository.BillRepository.BillWithTransactions
import com.example.organizadordefinancas.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Payment status for bill
 */
sealed class PaymentStatus {
    object Unpaid : PaymentStatus()
    data class PartiallyPaid(val paidAmount: Double, val remainingAmount: Double) : PaymentStatus()
    object FullyPaid : PaymentStatus()
    object Overdue : PaymentStatus()
}

/**
 * UI state for the Bill Detail screen
 */
data class BillDetailUiState(
    val bill: Bill? = null,
    val creditCard: CreditCard? = null,
    val transactions: List<Transaction> = emptyList(),
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val paymentStatus: PaymentStatus = PaymentStatus.Unpaid,
    val formattedClosingDate: String = "",
    val formattedDueDate: String = "",
    val availableBalancesForPayment: List<Balance> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * ViewModel for the Bill Detail screen.
 * Shows bill details, transactions, and handles payment.
 */
class BillDetailViewModel(
    private val billRepository: BillRepository,
    private val transactionRepository: TransactionRepository,
    private val balanceRepository: BalanceRepository
) : ViewModel() {

    private val _billId = MutableStateFlow<Long?>(null)

    private val _uiState = MutableStateFlow(BillDetailUiState())
    val uiState: StateFlow<BillDetailUiState> = _uiState.asStateFlow()

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    /**
     * The bill with its transactions
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val billWithTransactions: StateFlow<BillWithTransactions?> = _billId
        .filterNotNull()
        .flatMapLatest { id ->
            billRepository.getBillWithTransactions(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Available balances for payment
     */
    val availableBalances: StateFlow<List<Balance>> = balanceRepository.getActiveBalances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Combine flows into UI state
        viewModelScope.launch {
            combine(
                billWithTransactions,
                availableBalances
            ) { billData, balances ->
                if (billData != null) {
                    val bill = billData.bill
                    val remaining = bill.totalAmount - bill.paidAmount

                    val paymentStatus = when {
                        bill.status == BillStatus.PAID -> PaymentStatus.FullyPaid
                        bill.status == BillStatus.OVERDUE -> PaymentStatus.Overdue
                        bill.paidAmount > 0 -> PaymentStatus.PartiallyPaid(
                            paidAmount = bill.paidAmount,
                            remainingAmount = remaining
                        )
                        else -> PaymentStatus.Unpaid
                    }

                    BillDetailUiState(
                        bill = bill,
                        creditCard = billData.creditCard,
                        transactions = billData.transactions,
                        totalAmount = billData.calculatedTotal,
                        paidAmount = bill.paidAmount,
                        remainingAmount = remaining,
                        paymentStatus = paymentStatus,
                        formattedClosingDate = dateFormatter.format(Date(bill.closingDate)),
                        formattedDueDate = dateFormatter.format(Date(bill.dueDate)),
                        availableBalancesForPayment = balances.filter { it.currentBalance > 0 },
                        isLoading = false,
                        error = null
                    )
                } else {
                    BillDetailUiState(isLoading = true)
                }
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /**
     * Load a specific bill by ID
     */
    fun loadBill(billId: Long) {
        _billId.value = billId
    }

    /**
     * Pay the full remaining amount
     */
    fun payFullAmount(sourceBalanceId: Long) {
        val bill = _uiState.value.bill ?: return
        val remainingAmount = _uiState.value.remainingAmount

        if (remainingAmount <= 0) {
            _uiState.update { it.copy(error = "Bill is already paid") }
            return
        }

        payBill(sourceBalanceId, remainingAmount)
    }

    /**
     * Pay a partial amount
     */
    fun payPartialAmount(sourceBalanceId: Long, amount: Double) {
        if (amount <= 0) {
            _uiState.update { it.copy(error = "Payment amount must be positive") }
            return
        }

        val remainingAmount = _uiState.value.remainingAmount
        if (amount > remainingAmount) {
            _uiState.update { it.copy(error = "Payment exceeds remaining amount") }
            return
        }

        payBill(sourceBalanceId, amount)
    }

    /**
     * Process bill payment
     */
    private fun payBill(sourceBalanceId: Long, amount: Double) {
        viewModelScope.launch {
            try {
                val billId = _billId.value ?: throw IllegalStateException("No bill selected")

                // Check if balance has sufficient funds
                val balance = balanceRepository.getBalanceByIdSync(sourceBalanceId)
                if (balance == null || balance.currentBalance < amount) {
                    _uiState.update { it.copy(error = "Insufficient balance for payment") }
                    return@launch
                }

                // Create payment transaction
                val paymentTransactionId = transactionRepository.createExpense(
                    amount = amount,
                    balanceId = sourceBalanceId,
                    category = "Cartão de Crédito",
                    description = "Pagamento de fatura",
                    status = "completed"
                )

                // Record the payment
                val result = billRepository.recordBillPayment(
                    billId = billId,
                    paymentAmount = amount,
                    paymentTransactionId = paymentTransactionId
                )

                if (!result.success) {
                    _uiState.update { it.copy(error = result.errorMessage) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to process payment: ${e.message}") }
            }
        }
    }

    /**
     * Recalculate bill total from transactions
     */
    fun recalculateBillTotal() {
        viewModelScope.launch {
            try {
                val billId = _billId.value ?: return@launch
                billRepository.recalculateBillTotal(billId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to recalculate: ${e.message}") }
            }
        }
    }

    /**
     * Get transactions grouped by date
     */
    fun getTransactionsGroupedByDate(): Map<String, List<Transaction>> {
        return _uiState.value.transactions.groupBy { transaction ->
            dateFormatter.format(Date(transaction.date))
        }
    }

    /**
     * Check if a transaction is an installment
     */
    fun isInstallmentTransaction(transaction: Transaction): Boolean {
        return transaction.parentTransactionId != null || transaction.isInstallmentParent
    }

    /**
     * Get installment info text (e.g., "1/12")
     */
    fun getInstallmentInfo(transaction: Transaction): String? {
        if (transaction.installmentNumber != null && transaction.totalInstallments != null) {
            return "${transaction.installmentNumber}/${transaction.totalInstallments}"
        }
        return null
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * Factory for creating BillDetailViewModel instances
 */
class BillDetailViewModelFactory(
    private val billRepository: BillRepository,
    private val transactionRepository: TransactionRepository,
    private val balanceRepository: BalanceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BillDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BillDetailViewModel(
                billRepository,
                transactionRepository,
                balanceRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

