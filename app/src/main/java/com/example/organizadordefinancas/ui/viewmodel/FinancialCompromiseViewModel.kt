package com.example.organizadordefinancas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.organizadordefinancas.data.model.FinancialCompromise
import com.example.organizadordefinancas.data.repository.FinancialCompromiseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FinancialCompromiseViewModel(private val repository: FinancialCompromiseRepository) : ViewModel() {

    val allCompromises: StateFlow<List<FinancialCompromise>> = repository.getAllActiveCompromises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMonthlyCompromises: StateFlow<Double> = repository.getTotalMonthlyCompromises()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Total of compromises NOT linked to any credit card (to avoid double counting)
    val totalNonLinkedCompromises: StateFlow<Double> = repository.getTotalNonLinkedCompromises()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _selectedCompromise = MutableStateFlow<FinancialCompromise?>(null)
    val selectedCompromise: StateFlow<FinancialCompromise?> = _selectedCompromise.asStateFlow()

    fun getCompromisesByCardId(cardId: Long): Flow<List<FinancialCompromise>> =
        repository.getCompromisesByCardId(cardId)

    fun getTotalCompromisesByCardId(cardId: Long): Flow<Double> =
        repository.getTotalCompromisesByCardId(cardId).map { it ?: 0.0 }

    fun selectCompromise(compromiseId: Long) {
        viewModelScope.launch {
            repository.getCompromiseById(compromiseId).collect { compromise ->
                _selectedCompromise.value = compromise
            }
        }
    }

    fun insertCompromise(compromise: FinancialCompromise) {
        viewModelScope.launch {
            repository.insertCompromise(compromise)
        }
    }

    fun updateCompromise(compromise: FinancialCompromise) {
        viewModelScope.launch {
            repository.updateCompromise(compromise)
        }
    }

    fun deleteCompromise(compromise: FinancialCompromise) {
        viewModelScope.launch {
            repository.deleteCompromise(compromise)
        }
    }

    fun togglePaidStatus(compromise: FinancialCompromise) {
        viewModelScope.launch {
            repository.updatePaidStatus(compromise.id, !compromise.isPaid)
        }
    }
}

class FinancialCompromiseViewModelFactory(private val repository: FinancialCompromiseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinancialCompromiseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinancialCompromiseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

