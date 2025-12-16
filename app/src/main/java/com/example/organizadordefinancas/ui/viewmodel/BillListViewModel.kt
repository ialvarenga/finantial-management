package com.example.organizadordefinancas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.organizadordefinancas.data.model.Bill
import com.example.organizadordefinancas.data.model.BillStatus
import com.example.organizadordefinancas.data.model.CreditCard
import com.example.organizadordefinancas.data.repository.BillRepository
import com.example.organizadordefinancas.data.repository.BillRepository.BillWithTransactions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Summary of bills by status
 */
data class BillSummary(
    val openBills: List<BillWithCard> = emptyList(),
    val overdueBills: List<BillWithCard> = emptyList(),
    val paidBills: List<BillWithCard> = emptyList(),
    val totalUnpaid: Double = 0.0,
    val totalOverdue: Double = 0.0
)

/**
 * Bill with associated credit card info
 */
data class BillWithCard(
    val bill: Bill,
    val creditCard: CreditCard?,
    val remainingAmount: Double
)

/**
 * UI state for the Bill List screen
 */
data class BillListUiState(
    val bills: List<BillWithCard> = emptyList(),
    val summary: BillSummary = BillSummary(),
    val selectedCreditCardId: Long? = null,
    val filterStatus: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * Helper data class for combining bill flows
 */
private data class CombinedBillData(
    val allBills: List<Bill>,
    val openBills: List<Bill>,
    val overdueBills: List<Bill>,
    val unpaid: Double,
    val cardId: Long?
)

/**
 * ViewModel for the Bill List screen.
 * Manages bill listing, filtering, and status grouping.
 */
class BillListViewModel(
    private val billRepository: BillRepository
) : ViewModel() {

    private val _selectedCreditCardId = MutableStateFlow<Long?>(null)
    private val _filterStatus = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(BillListUiState())
    val uiState: StateFlow<BillListUiState> = _uiState.asStateFlow()

    /**
     * All bills (optionally filtered by credit card)
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val bills: StateFlow<List<Bill>> = _selectedCreditCardId
        .flatMapLatest { cardId ->
            if (cardId != null) {
                billRepository.getBillsByCreditCard(cardId)
            } else {
                billRepository.getAllBills()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Open bills
     */
    val openBills: StateFlow<List<Bill>> = billRepository.getOpenBills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Overdue bills
     */
    val overdueBills: StateFlow<List<Bill>> = billRepository.getOverdueBills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Total unpaid amount across all bills
     */
    val totalUnpaidAmount: StateFlow<Double> = billRepository.getTotalUnpaidAmount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        // Combine flows into UI state with bill summaries
        viewModelScope.launch {
            combine(
                bills,
                openBills,
                overdueBills,
                totalUnpaidAmount,
                _selectedCreditCardId
            ) { allBills: List<Bill>, open: List<Bill>, overdue: List<Bill>, unpaid: Double, cardId: Long? ->
                // Create combined data
                CombinedBillData(allBills, open, overdue, unpaid, cardId)
            }.combine(_filterStatus) { data, status ->
                val billsWithCards = data.allBills.map { bill ->
                    BillWithCard(
                        bill = bill,
                        creditCard = null, // Would need to join with credit cards
                        remainingAmount = bill.totalAmount - bill.paidAmount
                    )
                }

                val openBillsWithCards = data.openBills.map { bill ->
                    BillWithCard(
                        bill = bill,
                        creditCard = null,
                        remainingAmount = bill.totalAmount - bill.paidAmount
                    )
                }

                val overdueBillsWithCards = data.overdueBills.map { bill ->
                    BillWithCard(
                        bill = bill,
                        creditCard = null,
                        remainingAmount = bill.totalAmount - bill.paidAmount
                    )
                }

                val paidBills = data.allBills.filter { it.status == BillStatus.PAID }.map { bill ->
                    BillWithCard(
                        bill = bill,
                        creditCard = null,
                        remainingAmount = 0.0
                    )
                }

                val filteredBills = when (status) {
                    BillStatus.OPEN -> openBillsWithCards
                    BillStatus.OVERDUE -> overdueBillsWithCards
                    BillStatus.PAID -> paidBills
                    else -> billsWithCards
                }

                BillListUiState(
                    bills = filteredBills,
                    summary = BillSummary(
                        openBills = openBillsWithCards,
                        overdueBills = overdueBillsWithCards,
                        paidBills = paidBills,
                        totalUnpaid = data.unpaid,
                        totalOverdue = data.overdueBills.sumOf { it.totalAmount - it.paidAmount }
                    ),
                    selectedCreditCardId = data.cardId,
                    filterStatus = status,
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
     * Filter bills by credit card
     */
    fun filterByCreditCard(creditCardId: Long?) {
        _selectedCreditCardId.value = creditCardId
    }

    /**
     * Filter bills by status
     */
    fun filterByStatus(status: String?) {
        _filterStatus.value = status
    }

    /**
     * Show only open bills
     */
    fun showOpenBills() {
        filterByStatus(BillStatus.OPEN)
    }

    /**
     * Show only overdue bills
     */
    fun showOverdueBills() {
        filterByStatus(BillStatus.OVERDUE)
    }

    /**
     * Show only paid bills
     */
    fun showPaidBills() {
        filterByStatus(BillStatus.PAID)
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _selectedCreditCardId.value = null
        _filterStatus.value = null
    }

    // ==================== Bill Actions ====================

    /**
     * Generate bill for a credit card and month
     */
    fun generateBill(creditCardId: Long, year: Int, month: Int) {
        viewModelScope.launch {
            try {
                val billId = billRepository.generateBillForMonth(creditCardId, year, month)
                if (billId == null) {
                    _uiState.update { it.copy(error = "Bill already exists for this month") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to generate bill: ${e.message}") }
            }
        }
    }

    /**
     * Auto-generate bills for all cards if needed
     */
    fun autoGenerateBills() {
        viewModelScope.launch {
            try {
                billRepository.autoGenerateBillsIfNeeded()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to auto-generate bills: ${e.message}") }
            }
        }
    }

    /**
     * Delete a bill
     */
    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            try {
                billRepository.deleteBill(bill)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete bill: ${e.message}") }
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
 * Factory for creating BillListViewModel instances
 */
class BillListViewModelFactory(
    private val billRepository: BillRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BillListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BillListViewModel(billRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

