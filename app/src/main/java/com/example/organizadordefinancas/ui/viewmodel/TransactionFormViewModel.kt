package com.example.organizadordefinancas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.organizadordefinancas.data.model.Balance
import com.example.organizadordefinancas.data.model.Bill
import com.example.organizadordefinancas.data.model.CreditCard
import com.example.organizadordefinancas.data.model.Transaction
import com.example.organizadordefinancas.data.model.TransactionStatus
import com.example.organizadordefinancas.data.model.TransactionType
import com.example.organizadordefinancas.data.repository.BalanceRepository
import com.example.organizadordefinancas.data.repository.BillRepository
import com.example.organizadordefinancas.data.repository.TransactionRepository
import com.example.organizadordefinancas.data.repository.TransactionRepository.InstallmentCreationResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Form data for creating/editing a transaction
 */
data class TransactionFormData(
    val amount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val type: String = TransactionType.EXPENSE,
    val category: String = "Outros",
    val subcategory: String? = null,
    val description: String? = null,
    val balanceId: Long? = null,
    val billId: Long? = null,
    val status: String = TransactionStatus.COMPLETED,
    val isInstallment: Boolean = false,
    val installmentCount: Int = 1,
    val creditCardId: Long? = null
)

/**
 * UI state for the Transaction Form screen
 */
data class TransactionFormUiState(
    val formData: TransactionFormData = TransactionFormData(),
    val availableBalances: List<Balance> = emptyList(),
    val availableBills: List<Bill> = emptyList(),
    val availableCreditCards: List<CreditCard> = emptyList(),
    val installmentPreview: String? = null,
    val isEditing: Boolean = false,
    val editingTransactionId: Long? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    val savedSuccessfully: Boolean = false
)

/**
 * Predefined categories for transactions
 */
object TransactionCategories {
    val EXPENSE_CATEGORIES = listOf(
        "Alimentação",
        "Transporte",
        "Moradia",
        "Saúde",
        "Educação",
        "Lazer",
        "Compras",
        "Eletrônicos",
        "Vestuário",
        "Serviços",
        "Assinaturas",
        "Cartão de Crédito",
        "Transferência",
        "Outros"
    )

    val INCOME_CATEGORIES = listOf(
        "Salário",
        "Freelance",
        "Investimentos",
        "Transferência",
        "Reembolso",
        "Presente",
        "Outros"
    )
}

/**
 * ViewModel for the Transaction Form screen.
 * Handles creation and editing of transactions, including installment purchases.
 */
class TransactionFormViewModel(
    private val transactionRepository: TransactionRepository,
    private val balanceRepository: BalanceRepository,
    private val billRepository: BillRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionFormUiState())
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()

    /**
     * Available balances for selection
     */
    val availableBalances: StateFlow<List<Balance>> = balanceRepository.getActiveBalances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Open bills for selection
     */
    val openBills: StateFlow<List<Bill>> = billRepository.getOpenBills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Load available options
        viewModelScope.launch {
            combine(
                availableBalances,
                openBills
            ) { balances, bills ->
                _uiState.update {
                    it.copy(
                        availableBalances = balances,
                        availableBills = bills,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    // ==================== Form Data Updates ====================

    /**
     * Update the amount
     */
    fun updateAmount(amount: Double) {
        _uiState.update { state ->
            val newFormData = state.formData.copy(amount = amount)
            state.copy(
                formData = newFormData,
                installmentPreview = calculateInstallmentPreview(newFormData),
                validationErrors = state.validationErrors - "amount"
            )
        }
    }

    /**
     * Update the date
     */
    fun updateDate(date: Long) {
        _uiState.update { state ->
            state.copy(
                formData = state.formData.copy(date = date),
                validationErrors = state.validationErrors - "date"
            )
        }
    }

    /**
     * Update the transaction type
     */
    fun updateType(type: String) {
        _uiState.update { state ->
            state.copy(
                formData = state.formData.copy(
                    type = type,
                    // Clear installment when switching to income
                    isInstallment = if (type == TransactionType.INCOME) false else state.formData.isInstallment
                )
            )
        }
    }

    /**
     * Update the category
     */
    fun updateCategory(category: String) {
        _uiState.update { state ->
            state.copy(
                formData = state.formData.copy(category = category),
                validationErrors = state.validationErrors - "category"
            )
        }
    }

    /**
     * Update the subcategory
     */
    fun updateSubcategory(subcategory: String?) {
        _uiState.update { state ->
            state.copy(formData = state.formData.copy(subcategory = subcategory))
        }
    }

    /**
     * Update the description
     */
    fun updateDescription(description: String?) {
        _uiState.update { state ->
            state.copy(formData = state.formData.copy(description = description))
        }
    }

    /**
     * Update the balance ID
     */
    fun updateBalanceId(balanceId: Long?) {
        _uiState.update { state ->
            state.copy(
                formData = state.formData.copy(balanceId = balanceId),
                validationErrors = state.validationErrors - "source"
            )
        }
    }

    /**
     * Update the bill ID
     */
    fun updateBillId(billId: Long?) {
        _uiState.update { state ->
            state.copy(
                formData = state.formData.copy(billId = billId),
                validationErrors = state.validationErrors - "source"
            )
        }
    }

    /**
     * Update the credit card ID (for installments)
     */
    fun updateCreditCardId(creditCardId: Long?) {
        _uiState.update { state ->
            state.copy(formData = state.formData.copy(creditCardId = creditCardId))
        }
    }

    /**
     * Update the status
     */
    fun updateStatus(status: String) {
        _uiState.update { state ->
            state.copy(formData = state.formData.copy(status = status))
        }
    }

    /**
     * Toggle installment mode
     */
    fun toggleInstallment(isInstallment: Boolean) {
        _uiState.update { state ->
            val newFormData = state.formData.copy(
                isInstallment = isInstallment,
                installmentCount = if (isInstallment) 2 else 1
            )
            state.copy(
                formData = newFormData,
                installmentPreview = calculateInstallmentPreview(newFormData)
            )
        }
    }

    /**
     * Update the installment count
     */
    fun updateInstallmentCount(count: Int) {
        val validCount = count.coerceIn(2, 48)
        _uiState.update { state ->
            val newFormData = state.formData.copy(installmentCount = validCount)
            state.copy(
                formData = newFormData,
                installmentPreview = calculateInstallmentPreview(newFormData)
            )
        }
    }

    /**
     * Calculate installment preview text
     */
    private fun calculateInstallmentPreview(formData: TransactionFormData): String? {
        if (!formData.isInstallment || formData.installmentCount <= 1) return null
        val amountPerInstallment = formData.amount / formData.installmentCount
        return "${formData.installmentCount}x R$ %.2f".format(amountPerInstallment)
    }

    // ==================== Load for Editing ====================

    /**
     * Load a transaction for editing
     */
    fun loadTransaction(transactionId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                transactionRepository.getTransactionById(transactionId).collect { transaction ->
                    transaction?.let {
                        _uiState.update { state ->
                            state.copy(
                                formData = TransactionFormData(
                                    amount = transaction.amount,
                                    date = transaction.date,
                                    type = transaction.type,
                                    category = transaction.category,
                                    subcategory = transaction.subcategory,
                                    description = transaction.description,
                                    balanceId = transaction.balanceId,
                                    billId = transaction.billId,
                                    status = transaction.status
                                ),
                                isEditing = true,
                                editingTransactionId = transactionId,
                                isLoading = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to load transaction: ${e.message}")
                }
            }
        }
    }

    // ==================== Validation ====================

    /**
     * Validate the form data
     */
    private fun validate(): Boolean {
        val errors = mutableMapOf<String, String>()
        val formData = _uiState.value.formData

        if (formData.amount <= 0) {
            errors["amount"] = "Amount must be greater than zero"
        }

        if (formData.category.isBlank()) {
            errors["category"] = "Category is required"
        }

        // Must have either balance or bill
        if (formData.balanceId == null && formData.billId == null) {
            errors["source"] = "Select a balance or bill"
        }

        // Installments require credit card and bill
        if (formData.isInstallment) {
            if (formData.billId == null) {
                errors["source"] = "Installment purchases require a bill"
            }
            if (formData.creditCardId == null) {
                errors["source"] = "Installment purchases require a credit card"
            }
        }

        _uiState.update { it.copy(validationErrors = errors) }
        return errors.isEmpty()
    }

    // ==================== Save Transaction ====================

    /**
     * Save the transaction (create or update)
     */
    fun save() {
        if (!validate()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val formData = _uiState.value.formData

                if (_uiState.value.isEditing) {
                    // Update existing transaction
                    updateExistingTransaction(formData)
                } else if (formData.isInstallment) {
                    // Create installment purchase
                    createInstallmentPurchase(formData)
                } else {
                    // Create simple transaction
                    createSimpleTransaction(formData)
                }

                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = "Failed to save: ${e.message}")
                }
            }
        }
    }

    /**
     * Create a simple (non-installment) transaction
     */
    private suspend fun createSimpleTransaction(formData: TransactionFormData) {
        val transaction = Transaction(
            amount = formData.amount,
            date = formData.date,
            balanceId = formData.balanceId,
            billId = formData.billId,
            type = formData.type,
            status = formData.status,
            category = formData.category,
            subcategory = formData.subcategory,
            description = formData.description
        )
        transactionRepository.insertTransaction(transaction)
    }

    /**
     * Create an installment purchase with parent and children
     */
    private suspend fun createInstallmentPurchase(formData: TransactionFormData) {
        val result = transactionRepository.createInstallmentPurchase(
            totalAmount = formData.amount,
            installments = formData.installmentCount,
            category = formData.category,
            description = formData.description,
            billId = formData.billId!!,
            creditCardId = formData.creditCardId!!,
            date = formData.date
        )

        if (!result.success) {
            throw Exception(result.errorMessage ?: "Failed to create installment purchase")
        }
    }

    /**
     * Update an existing transaction
     */
    private suspend fun updateExistingTransaction(formData: TransactionFormData) {
        val transactionId = _uiState.value.editingTransactionId
            ?: throw IllegalStateException("No transaction ID for editing")

        val existingTransaction = transactionRepository.getTransactionByIdSync(transactionId)
            ?: throw IllegalStateException("Transaction not found")

        val updatedTransaction = existingTransaction.copy(
            amount = formData.amount,
            date = formData.date,
            balanceId = formData.balanceId,
            billId = formData.billId,
            type = formData.type,
            status = formData.status,
            category = formData.category,
            subcategory = formData.subcategory,
            description = formData.description
        )

        transactionRepository.updateTransaction(updatedTransaction)
    }

    /**
     * Reset form to initial state
     */
    fun resetForm() {
        _uiState.update {
            TransactionFormUiState(
                availableBalances = it.availableBalances,
                availableBills = it.availableBills,
                availableCreditCards = it.availableCreditCards
            )
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Reset saved successfully flag
     */
    fun resetSavedState() {
        _uiState.update { it.copy(savedSuccessfully = false) }
    }
}

/**
 * Factory for creating TransactionFormViewModel instances
 */
class TransactionFormViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val balanceRepository: BalanceRepository,
    private val billRepository: BillRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionFormViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionFormViewModel(
                transactionRepository,
                balanceRepository,
                billRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

