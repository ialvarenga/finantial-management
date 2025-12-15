package com.example.organizadordefinancas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.organizadordefinancas.data.model.Income
import com.example.organizadordefinancas.data.model.IncomeType
import com.example.organizadordefinancas.data.repository.IncomeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IncomeViewModel(private val repository: IncomeRepository) : ViewModel() {

    val allIncomes: StateFlow<List<Income>> = repository.getAllActiveIncomes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurrentIncomes: StateFlow<List<Income>> = repository.getIncomesByType(IncomeType.RECURRENT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val oneTimeIncomes: StateFlow<List<Income>> = repository.getIncomesByType(IncomeType.ONE_TIME)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMonthlyIncome: StateFlow<Double> = repository.getTotalMonthlyIncome()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalRecurrentIncome: StateFlow<Double> = repository.getTotalRecurrentIncome()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _selectedIncome = MutableStateFlow<Income?>(null)
    val selectedIncome: StateFlow<Income?> = _selectedIncome.asStateFlow()

    fun selectIncome(incomeId: Long) {
        viewModelScope.launch {
            repository.getIncomeById(incomeId).collect { income ->
                _selectedIncome.value = income
            }
        }
    }

    fun insertIncome(income: Income) {
        viewModelScope.launch {
            repository.insertIncome(income)
        }
    }

    fun updateIncome(income: Income) {
        viewModelScope.launch {
            repository.updateIncome(income)
        }
    }

    fun deleteIncome(income: Income) {
        viewModelScope.launch {
            repository.deleteIncome(income)
        }
    }

    fun toggleReceivedStatus(income: Income) {
        viewModelScope.launch {
            repository.updateReceivedStatus(income.id, !income.isReceived)
        }
    }
}

class IncomeViewModelFactory(private val repository: IncomeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IncomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IncomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

