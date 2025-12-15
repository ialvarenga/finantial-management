package com.example.organizadordefinancas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.organizadordefinancas.data.model.CompromiseOccurrence
import com.example.organizadordefinancas.data.model.FinancialCompromise
import com.example.organizadordefinancas.data.model.OccurrenceStatus
import com.example.organizadordefinancas.data.repository.CompromiseOccurrenceRepository
import com.example.organizadordefinancas.data.repository.FinancialCompromiseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FinancialCompromiseViewModel(
    private val repository: FinancialCompromiseRepository,
    private val occurrenceRepository: CompromiseOccurrenceRepository? = null
) : ViewModel() {

    val allCompromises: StateFlow<List<FinancialCompromise>> = repository.getAllActiveCompromises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMonthlyCompromises: StateFlow<Double> = repository.getTotalMonthlyCompromises()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Total of compromises NOT linked to any credit card (to avoid double counting)
    val totalNonLinkedCompromises: StateFlow<Double> = repository.getTotalNonLinkedCompromises()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Pending occurrences
    val pendingOccurrences: StateFlow<List<CompromiseOccurrence>> = occurrenceRepository?.getPendingOccurrences()
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    // Overdue occurrences
    val overdueOccurrences: StateFlow<List<CompromiseOccurrence>> = occurrenceRepository?.getOverdueOccurrences()
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    private val _selectedCompromise = MutableStateFlow<FinancialCompromise?>(null)
    val selectedCompromise: StateFlow<FinancialCompromise?> = _selectedCompromise.asStateFlow()

    private val _selectedOccurrences = MutableStateFlow<List<CompromiseOccurrence>>(emptyList())
    val selectedOccurrences: StateFlow<List<CompromiseOccurrence>> = _selectedOccurrences.asStateFlow()

    fun getCompromisesByCardId(cardId: Long): Flow<List<FinancialCompromise>> =
        repository.getCompromisesByCardId(cardId)

    fun getTotalCompromisesByCardId(cardId: Long): Flow<Double> =
        repository.getTotalCompromisesByCardId(cardId).map { it ?: 0.0 }

    fun getOccurrencesByCompromiseId(compromiseId: Long): Flow<List<CompromiseOccurrence>> =
        occurrenceRepository?.getOccurrencesByCompromiseId(compromiseId) ?: flowOf(emptyList())

    fun selectCompromise(compromiseId: Long) {
        viewModelScope.launch {
            repository.getCompromiseById(compromiseId).collect { compromise ->
                _selectedCompromise.value = compromise
            }
        }
        // Also load occurrences for this compromise
        viewModelScope.launch {
            occurrenceRepository?.getOccurrencesByCompromiseId(compromiseId)?.collect { occurrences ->
                _selectedOccurrences.value = occurrences
            }
        }
    }

    fun insertCompromise(compromise: FinancialCompromise) {
        viewModelScope.launch {
            val id = repository.insertCompromise(compromise)
            // Generate occurrences for the new compromise
            occurrenceRepository?.generateOccurrences(compromise.copy(id = id))
        }
    }

    fun updateCompromise(compromise: FinancialCompromise) {
        viewModelScope.launch {
            repository.updateCompromise(compromise)
            // Regenerate occurrences after update
            occurrenceRepository?.regenerateOccurrences(compromise)
        }
    }

    fun deleteCompromise(compromise: FinancialCompromise) {
        viewModelScope.launch {
            // Occurrences will be deleted automatically due to CASCADE
            repository.deleteCompromise(compromise)
        }
    }

    fun togglePaidStatus(compromise: FinancialCompromise) {
        viewModelScope.launch {
            repository.updatePaidStatus(compromise.id, !compromise.isPaid)
        }
    }

    /**
     * Mark an occurrence as paid.
     */
    fun markOccurrenceAsPaid(occurrence: CompromiseOccurrence, paidAmount: Double = occurrence.expectedAmount) {
        viewModelScope.launch {
            occurrenceRepository?.markAsPaid(occurrence.id, paidAmount)
        }
    }

    /**
     * Skip an occurrence.
     */
    fun skipOccurrence(occurrence: CompromiseOccurrence) {
        viewModelScope.launch {
            occurrenceRepository?.skipOccurrence(occurrence.id)
        }
    }

    /**
     * Generate occurrences for all active compromises.
     * Should be called on app startup.
     */
    fun generateAllOccurrences() {
        viewModelScope.launch {
            allCompromises.value.forEach { compromise ->
                occurrenceRepository?.generateOccurrences(compromise)
            }
        }
    }

    /**
     * Clean up old paid occurrences.
     */
    fun cleanupOldOccurrences() {
        viewModelScope.launch {
            occurrenceRepository?.cleanupOldOccurrences()
        }
    }

    /**
     * Get the next occurrence for a specific compromise.
     */
    suspend fun getNextOccurrence(compromiseId: Long): CompromiseOccurrence? =
        occurrenceRepository?.getNextOccurrence(compromiseId)
}

class FinancialCompromiseViewModelFactory(
    private val repository: FinancialCompromiseRepository,
    private val occurrenceRepository: CompromiseOccurrenceRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinancialCompromiseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinancialCompromiseViewModel(repository, occurrenceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

