package com.example.organizadordefinancas.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

enum class CompromiseCategory {
    RENT,       // Aluguel
    ENERGY,     // Energia
    WATER,      // Água
    INTERNET,   // Internet
    PHONE,      // Telefone
    INSURANCE,  // Seguro
    STREAMING,  // Streaming (Netflix, Spotify, etc.)
    GYM,        // Academia
    EDUCATION,  // Educação
    HEALTH,     // Saúde
    TRANSPORT,  // Transporte
    TECHNOLOGY,  // Tecnologia
    FOOD,       // Alimentação
    COMPANY,    // Empresa
    HOUSEHOLD,   // Casa
    OTHER       // Outros
}

@Entity(tableName = "financial_compromises")
data class FinancialCompromise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val dueDay: Int, // Day of month when it's due (1-31) - kept for backward compatibility
    val category: CompromiseCategory = CompromiseCategory.OTHER,
    val isPaid: Boolean = false, // For current month (legacy, use occurrences for recurrent)
    val isActive: Boolean = true, // If the compromise is still active
    val linkedCreditCardId: Long? = null, // If linked to a credit card, it shows on the card bill

    // New fields for frequency support
    val frequency: CompromiseFrequency = CompromiseFrequency.MONTHLY,
    val dayOfWeek: Int? = null,      // For WEEKLY/BIWEEKLY (1-7, Monday=1, Sunday=7)
    val dayOfMonth: Int? = null,     // For MONTHLY+ (1-31), if null uses dueDay for compatibility
    val monthOfYear: Int? = null,    // For QUARTERLY/SEMIANNUAL/ANNUAL (1-12)
    val startDate: Long = System.currentTimeMillis(), // Start date of the compromise
    val endDate: Long? = null,       // End date (optional, null = no end)
    val reminderDaysBefore: Int = 3  // Days before due date to remind
) {
    /**
     * Gets the effective day of month for this compromise.
     * Uses dayOfMonth if set, otherwise falls back to dueDay for backward compatibility.
     */
    fun getEffectiveDayOfMonth(): Int {
        return dayOfMonth ?: dueDay
    }

    /**
     * Calculates the monthly equivalent amount for budgeting purposes.
     * For example, an annual expense of R$ 1200 would return R$ 100/month.
     */
    fun getMonthlyEquivalent(): Double {
        return amount * frequency.getOccurrencesPerMonth()
    }

    /**
     * Calculates the next due date from a given reference date.
     * Returns the timestamp of the next occurrence.
     */
    fun getNextDueDate(fromDate: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = fromDate
        }

        return when (frequency) {
            CompromiseFrequency.WEEKLY -> {
                val targetDayOfWeek = dayOfWeek ?: Calendar.MONDAY
                // Convert to Calendar day of week (Sunday=1, Saturday=7)
                val calendarDayOfWeek = if (targetDayOfWeek == 7) Calendar.SUNDAY else targetDayOfWeek + 1

                while (calendar.get(Calendar.DAY_OF_WEEK) != calendarDayOfWeek) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
                if (calendar.timeInMillis <= fromDate) {
                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                }
                calendar.timeInMillis
            }

            CompromiseFrequency.BIWEEKLY -> {
                val targetDayOfWeek = dayOfWeek ?: Calendar.MONDAY
                val calendarDayOfWeek = if (targetDayOfWeek == 7) Calendar.SUNDAY else targetDayOfWeek + 1

                // Find next occurrence of the day of week
                while (calendar.get(Calendar.DAY_OF_WEEK) != calendarDayOfWeek) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                // Check if we need to add 2 weeks based on start date alignment
                val startCal = Calendar.getInstance().apply { timeInMillis = startDate }
                val weeksDiff = ((calendar.timeInMillis - startCal.timeInMillis) / (7 * 24 * 60 * 60 * 1000L)).toInt()
                if (weeksDiff % 2 != 0) {
                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                }

                if (calendar.timeInMillis <= fromDate) {
                    calendar.add(Calendar.WEEK_OF_YEAR, 2)
                }
                calendar.timeInMillis
            }

            CompromiseFrequency.MONTHLY -> {
                val day = getEffectiveDayOfMonth()
                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, minOf(day, maxDay))

                if (calendar.timeInMillis <= fromDate) {
                    calendar.add(Calendar.MONTH, 1)
                    val newMaxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    calendar.set(Calendar.DAY_OF_MONTH, minOf(day, newMaxDay))
                }
                calendar.timeInMillis
            }

            CompromiseFrequency.QUARTERLY -> {
                val day = getEffectiveDayOfMonth()
                val targetMonth = monthOfYear ?: (calendar.get(Calendar.MONTH) + 1)

                // Find next quarter that includes the target month pattern
                val quarterMonths = listOf(
                    targetMonth,
                    ((targetMonth - 1 + 3) % 12) + 1,
                    ((targetMonth - 1 + 6) % 12) + 1,
                    ((targetMonth - 1 + 9) % 12) + 1
                ).sorted()

                val currentMonth = calendar.get(Calendar.MONTH) + 1
                var nextMonth = quarterMonths.firstOrNull { it >= currentMonth } ?: quarterMonths.first()

                if (nextMonth < currentMonth) {
                    calendar.add(Calendar.YEAR, 1)
                }
                calendar.set(Calendar.MONTH, nextMonth - 1)
                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, minOf(day, maxDay))

                if (calendar.timeInMillis <= fromDate) {
                    val idx = quarterMonths.indexOf(nextMonth)
                    nextMonth = quarterMonths.getOrNull(idx + 1) ?: run {
                        calendar.add(Calendar.YEAR, 1)
                        quarterMonths.first()
                    }
                    calendar.set(Calendar.MONTH, nextMonth - 1)
                    val newMaxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    calendar.set(Calendar.DAY_OF_MONTH, minOf(day, newMaxDay))
                }
                calendar.timeInMillis
            }

            CompromiseFrequency.SEMIANNUAL -> {
                val day = getEffectiveDayOfMonth()
                val targetMonth = monthOfYear ?: (calendar.get(Calendar.MONTH) + 1)
                val secondMonth = ((targetMonth - 1 + 6) % 12) + 1

                val semiAnnualMonths = listOf(targetMonth, secondMonth).sorted()
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                var nextMonth = semiAnnualMonths.firstOrNull { it >= currentMonth } ?: semiAnnualMonths.first()

                if (nextMonth < currentMonth) {
                    calendar.add(Calendar.YEAR, 1)
                }
                calendar.set(Calendar.MONTH, nextMonth - 1)
                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, minOf(day, maxDay))

                if (calendar.timeInMillis <= fromDate) {
                    val idx = semiAnnualMonths.indexOf(nextMonth)
                    nextMonth = semiAnnualMonths.getOrNull(idx + 1) ?: run {
                        calendar.add(Calendar.YEAR, 1)
                        semiAnnualMonths.first()
                    }
                    calendar.set(Calendar.MONTH, nextMonth - 1)
                    val newMaxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    calendar.set(Calendar.DAY_OF_MONTH, minOf(day, newMaxDay))
                }
                calendar.timeInMillis
            }

            CompromiseFrequency.ANNUAL -> {
                val day = getEffectiveDayOfMonth()
                val targetMonth = monthOfYear ?: (calendar.get(Calendar.MONTH) + 1)

                calendar.set(Calendar.MONTH, targetMonth - 1)
                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, minOf(day, maxDay))

                if (calendar.timeInMillis <= fromDate) {
                    calendar.add(Calendar.YEAR, 1)
                    val newMaxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    calendar.set(Calendar.DAY_OF_MONTH, minOf(day, newMaxDay))
                }
                calendar.timeInMillis
            }
        }
    }
}

