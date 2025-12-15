package com.example.organizadordefinancas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.organizadordefinancas.data.model.Bank
import com.example.organizadordefinancas.data.model.CapturedNotification
import com.example.organizadordefinancas.data.model.CreditCard
import com.example.organizadordefinancas.data.model.CreditCardItem
import com.example.organizadordefinancas.data.model.NotificationStatus
import com.example.organizadordefinancas.data.repository.BankRepository
import com.example.organizadordefinancas.data.repository.CapturedNotificationRepository
import com.example.organizadordefinancas.data.repository.CreditCardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val notificationRepository: CapturedNotificationRepository,
    private val creditCardRepository: CreditCardRepository,
    private val bankRepository: BankRepository
) : ViewModel() {

    val pendingNotifications: StateFlow<List<CapturedNotification>> =
        notificationRepository.getPendingNotifications()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> =
        notificationRepository.getPendingCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allNotifications: StateFlow<List<CapturedNotification>> =
        notificationRepository.getAllNotifications()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creditCards: StateFlow<List<CreditCard>> =
        creditCardRepository.getAllCreditCards()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val banks: StateFlow<List<Bank>> =
        bankRepository.getAllBanks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedNotification = MutableStateFlow<CapturedNotification?>(null)
    val selectedNotification: StateFlow<CapturedNotification?> = _selectedNotification.asStateFlow()

    fun selectNotification(notification: CapturedNotification) {
        _selectedNotification.value = notification
    }

    fun clearSelection() {
        _selectedNotification.value = null
    }

    /**
     * Check if the notification is a PIX transaction (either sent or received).
     * Checks both the transactionType field and notification content for PIX keywords.
     */
    fun isPixTransaction(notification: CapturedNotification): Boolean {
        // Check transaction type first
        if (notification.transactionType == "PIX_SENT" ||
            notification.transactionType == "PIX_RECEIVED" ||
            notification.transactionType == "TRANSFER") {
            return true
        }

        // Fallback: check notification content for PIX keywords
        val contentLower = "${notification.title} ${notification.content}".lowercase()
        val pixKeywords = listOf(
            "pix enviado",
            "pix recebido",
            "você enviou",
            "você recebeu",
            "transferência",
            "transferencia",
            "pix para",
            "enviou r$",
            "recebeu r$"
        )
        return pixKeywords.any { contentLower.contains(it) }
    }

    /**
     * Check if the notification is a PIX received transaction (money received).
     * Returns true if this is a PIX where money was RECEIVED (should ADD to balance).
     */
    fun isPixReceivedTransaction(notification: CapturedNotification): Boolean {
        // Check transaction type first
        if (notification.transactionType == "PIX_RECEIVED") {
            return true
        }

        // Fallback: check notification content for PIX received keywords
        val contentLower = "${notification.title} ${notification.content}".lowercase()
        val pixReceivedKeywords = listOf(
            "pix recebido",
            "você recebeu",
            "recebeu r$"
        )
        return pixReceivedKeywords.any { contentLower.contains(it) }
    }

    /**
     * Process a notification as a credit card purchase.
     */
    fun processAsCreditCardPurchase(
        notificationId: Long,
        cardId: Long,
        description: String,
        amount: Double,
        category: String = "Outros",
        installments: Int = 1
    ) {
        viewModelScope.launch {
            // Create the credit card item
            val item = CreditCardItem(
                cardId = cardId,
                description = description,
                amount = amount,
                purchaseDate = System.currentTimeMillis(),
                installments = installments,
                currentInstallment = 1,
                category = category
            )

            // Insert with installments support
            if (installments > 1) {
                creditCardRepository.insertItemWithInstallments(item)
            } else {
                creditCardRepository.insertItem(item)
            }

            // Mark notification as processed
            notificationRepository.updateNotification(
                notificationRepository.getNotificationByIdSync(notificationId)?.copy(
                    status = NotificationStatus.PROCESSED,
                    linkedCardId = cardId
                ) ?: return@launch
            )

            clearSelection()
        }
    }

    /**
     * Process a notification as a PIX/transfer transaction.
     * For PIX sent: deducts from bank balance.
     * For PIX received: adds to bank balance.
     */
    fun processAsPixTransaction(
        notificationId: Long,
        bankId: Long,
        description: String,
        amount: Double
    ) {
        viewModelScope.launch {
            val notification = notificationRepository.getNotificationByIdSync(notificationId) ?: return@launch

            // Determine if this is a received PIX (add to balance) or sent PIX (deduct from balance)
            val isReceived = isPixReceivedTransaction(notification)

            if (isReceived) {
                // Add the amount to bank balance for received PIX
                bankRepository.addToBalance(bankId, amount)
            } else {
                // Deduct the amount from bank balance for sent PIX
                bankRepository.deductFromBalance(bankId, amount)
            }

            // Mark notification as processed
            notificationRepository.updateNotification(
                notification.copy(
                    status = NotificationStatus.PROCESSED,
                    linkedBankId = bankId
                )
            )

            clearSelection()
        }
    }

    /**
     * Ignore a notification - it won't appear in pending list anymore.
     */
    fun ignoreNotification(notificationId: Long) {
        viewModelScope.launch {
            notificationRepository.updateStatus(notificationId, NotificationStatus.IGNORED)
            clearSelection()
        }
    }

    /**
     * Delete a notification completely.
     */
    fun deleteNotification(notificationId: Long) {
        viewModelScope.launch {
            notificationRepository.deleteNotificationById(notificationId)
            clearSelection()
        }
    }

    /**
     * Clean up old processed/ignored notifications.
     */
    fun cleanupOldNotifications(daysToKeep: Int = 30) {
        viewModelScope.launch {
            notificationRepository.cleanupOldNotifications(daysToKeep)
        }
    }

    /**
     * Find the best matching credit card for a notification based on last 4 digits.
     */
    fun findMatchingCard(notification: CapturedNotification, cards: List<CreditCard>): CreditCard? {
        val lastFour = notification.extractedCardLastFour ?: return null

        return cards.find { card ->
            card.lastFourDigits?.equals(lastFour, ignoreCase = true) == true
        }
    }
}

class NotificationViewModelFactory(
    private val notificationRepository: CapturedNotificationRepository,
    private val creditCardRepository: CreditCardRepository,
    private val bankRepository: BankRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(notificationRepository, creditCardRepository, bankRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

