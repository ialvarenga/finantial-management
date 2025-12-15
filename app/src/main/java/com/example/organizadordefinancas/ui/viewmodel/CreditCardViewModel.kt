package com.example.organizadordefinancas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.organizadordefinancas.data.model.CreditCard
import com.example.organizadordefinancas.data.model.CreditCardItem
import com.example.organizadordefinancas.data.repository.CreditCardRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CreditCardWithTotal(
    val creditCard: CreditCard,
    val total: Double
)

class CreditCardViewModel(private val repository: CreditCardRepository) : ViewModel() {

    val allCreditCards: StateFlow<List<CreditCard>> = repository.getAllCreditCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allItems: StateFlow<List<CreditCardItem>> = repository.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    private val _selectedCard = MutableStateFlow<CreditCard?>(null)
    val selectedCard: StateFlow<CreditCard?> = _selectedCard.asStateFlow()

    private val _cardItems = MutableStateFlow<List<CreditCardItem>>(emptyList())
    val cardItems: StateFlow<List<CreditCardItem>> = _cardItems.asStateFlow()

    private val _cardTotal = MutableStateFlow(0.0)
    val cardTotal: StateFlow<Double> = _cardTotal.asStateFlow()

    fun getItemById(itemId: Long): Flow<CreditCardItem?> = repository.getItemById(itemId)

    fun selectCard(cardId: Long) {
        viewModelScope.launch {
            repository.getCreditCardById(cardId).collect { card ->
                _selectedCard.value = card
            }
        }
        viewModelScope.launch {
            repository.getItemsByCardId(cardId).collect { items ->
                _cardItems.value = items
            }
        }
        viewModelScope.launch {
            repository.getTotalByCardId(cardId).collect { total ->
                _cardTotal.value = total ?: 0.0
            }
        }
    }

    fun insertCreditCard(creditCard: CreditCard) {
        viewModelScope.launch {
            repository.insertCreditCard(creditCard)
        }
    }

    fun updateCreditCard(creditCard: CreditCard) {
        viewModelScope.launch {
            repository.updateCreditCard(creditCard)
        }
    }

    fun deleteCreditCard(creditCard: CreditCard) {
        viewModelScope.launch {
            repository.deleteCreditCard(creditCard)
        }
    }

    fun insertItem(item: CreditCardItem) {
        viewModelScope.launch {
            repository.insertItem(item)
        }
    }

    fun insertItemWithInstallments(item: CreditCardItem) {
        viewModelScope.launch {
            repository.insertItemWithInstallments(item)
        }
    }

    fun updateItem(item: CreditCardItem) {
        viewModelScope.launch {
            repository.updateItem(item)
        }
    }

    fun deleteItem(item: CreditCardItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun getTotalForAllCards(): Flow<Double> {
        return combine(allCreditCards, allItems) { cards, items ->
            items.sumOf { it.amount }
        }
    }
}

class CreditCardViewModelFactory(private val repository: CreditCardRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreditCardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreditCardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

