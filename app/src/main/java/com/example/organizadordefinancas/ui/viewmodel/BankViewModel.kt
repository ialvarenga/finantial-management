package com.example.organizadordefinancas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.organizadordefinancas.data.model.Bank
import com.example.organizadordefinancas.data.repository.BankRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BankViewModel(private val repository: BankRepository) : ViewModel() {

    val allBanks: StateFlow<List<Bank>> = repository.getAllBanks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Double> = repository.getTotalBalance()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalSavingsBalance: StateFlow<Double> = repository.getTotalSavingsBalance()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _selectedBank = MutableStateFlow<Bank?>(null)
    val selectedBank: StateFlow<Bank?> = _selectedBank.asStateFlow()

    fun selectBank(bankId: Long) {
        viewModelScope.launch {
            repository.getBankById(bankId).collect { bank ->
                _selectedBank.value = bank
            }
        }
    }

    fun insertBank(bank: Bank) {
        viewModelScope.launch {
            repository.insertBank(bank)
        }
    }

    fun updateBank(bank: Bank) {
        viewModelScope.launch {
            repository.updateBank(bank)
        }
    }

    fun deleteBank(bank: Bank) {
        viewModelScope.launch {
            repository.deleteBank(bank)
        }
    }
}

class BankViewModelFactory(private val repository: BankRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BankViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BankViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

