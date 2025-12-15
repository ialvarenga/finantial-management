package com.example.organizadordefinancas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.organizadordefinancas.data.model.AnalyticsPeriod
import com.example.organizadordefinancas.data.model.CategoryTotal
import com.example.organizadordefinancas.data.model.MerchantTotal
import com.example.organizadordefinancas.data.model.MonthlyTotal
import com.example.organizadordefinancas.data.repository.AnalyticsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.ZoneId

data class AnalyticsUiState(
    val totalSpending: Double = 0.0,
    val averageSpending: Double = 0.0,
    val maxSpending: Double = 0.0,
    val transactionCount: Int = 0,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val monthlyTotals: List<MonthlyTotal> = emptyList(),
    val topMerchants: List<MerchantTotal> = emptyList(),
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.THREE_MONTHS,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(private val repository: AnalyticsRepository) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(AnalyticsPeriod.THREE_MONTHS)
    val selectedPeriod: StateFlow<AnalyticsPeriod> = _selectedPeriod.asStateFlow()

    private val dateRange: StateFlow<Pair<Long, Long>> = _selectedPeriod.map { period ->
        calculateDateRange(period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), calculateDateRange(AnalyticsPeriod.THREE_MONTHS))

    val totalSpending: StateFlow<Double> = dateRange.flatMapLatest { (start, end) ->
        repository.getTotalSpending(start, end).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val averageSpending: StateFlow<Double> = dateRange.flatMapLatest { (start, end) ->
        repository.getAverageSpending(start, end).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val maxSpending: StateFlow<Double> = dateRange.flatMapLatest { (start, end) ->
        repository.getMaxSpending(start, end).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val transactionCount: StateFlow<Int> = dateRange.flatMapLatest { (start, end) ->
        repository.getTransactionCount(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val categoryTotals: StateFlow<List<CategoryTotal>> = dateRange.flatMapLatest { (start, end) ->
        repository.getSpendingByCategory(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyTotals: StateFlow<List<MonthlyTotal>> = dateRange.flatMapLatest { (start, _) ->
        repository.getMonthlySpending(start)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topMerchants: StateFlow<List<MerchantTotal>> = dateRange.flatMapLatest { (start, end) ->
        repository.getTopMerchants(start, end, 10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<AnalyticsUiState> = combine(
        totalSpending,
        averageSpending,
        maxSpending,
        transactionCount,
        categoryTotals,
        monthlyTotals,
        topMerchants,
        _selectedPeriod
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        AnalyticsUiState(
            totalSpending = values[0] as Double,
            averageSpending = values[1] as Double,
            maxSpending = values[2] as Double,
            transactionCount = values[3] as Int,
            categoryTotals = values[4] as List<CategoryTotal>,
            monthlyTotals = values[5] as List<MonthlyTotal>,
            topMerchants = values[6] as List<MerchantTotal>,
            selectedPeriod = values[7] as AnalyticsPeriod,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AnalyticsUiState()
    )

    fun selectPeriod(period: AnalyticsPeriod) {
        _selectedPeriod.value = period
    }

    private fun calculateDateRange(period: AnalyticsPeriod): Pair<Long, Long> {
        val now = LocalDate.now()
        val endDate = now.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val startDate = when (period) {
            AnalyticsPeriod.CURRENT_MONTH -> now.withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            else -> now.minusMonths(period.months.toLong() - 1).withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        return startDate to endDate
    }
}

class AnalyticsViewModelFactory(private val repository: AnalyticsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalyticsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

