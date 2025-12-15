package com.example.organizadordefinancas.data.model

/**
 * Represents the frequency at which a financial compromise recurs.
 */
enum class CompromiseFrequency {
    WEEKLY,      // Semanal
    BIWEEKLY,    // Quinzenal
    MONTHLY,     // Mensal
    QUARTERLY,   // Trimestral
    SEMIANNUAL,  // Semestral
    ANNUAL       // Anual
}

/**
 * Returns the display name in Portuguese for this frequency.
 */
fun CompromiseFrequency.getDisplayName(): String {
    return when (this) {
        CompromiseFrequency.WEEKLY -> "Semanal"
        CompromiseFrequency.BIWEEKLY -> "Quinzenal"
        CompromiseFrequency.MONTHLY -> "Mensal"
        CompromiseFrequency.QUARTERLY -> "Trimestral"
        CompromiseFrequency.SEMIANNUAL -> "Semestral"
        CompromiseFrequency.ANNUAL -> "Anual"
    }
}

/**
 * Returns how many months between each occurrence for this frequency.
 * Used to calculate monthly equivalent amount.
 */
fun CompromiseFrequency.getMonthsInterval(): Double {
    return when (this) {
        CompromiseFrequency.WEEKLY -> 0.25 // ~1 week = 1/4 month
        CompromiseFrequency.BIWEEKLY -> 0.5 // ~2 weeks = 1/2 month
        CompromiseFrequency.MONTHLY -> 1.0
        CompromiseFrequency.QUARTERLY -> 3.0
        CompromiseFrequency.SEMIANNUAL -> 6.0
        CompromiseFrequency.ANNUAL -> 12.0
    }
}

/**
 * Returns the number of occurrences per month for this frequency.
 */
fun CompromiseFrequency.getOccurrencesPerMonth(): Double {
    return 1.0 / getMonthsInterval()
}

