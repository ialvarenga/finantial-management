package com.example.organizadordefinancas.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "captured_notifications",
    foreignKeys = [
        ForeignKey(
            entity = CreditCardItem::class,
            parentColumns = ["id"],
            childColumns = ["linkedItemId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["linkedItemId"]),
        Index(value = ["status"]),
        Index(value = ["capturedAt"])
    ]
)
data class CapturedNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,                    // Ex: "com.nu.production", "com.google.android.apps.walletnfcrel"
    val title: String,
    val content: String,
    val capturedAt: Long,                       // Timestamp when notification was received
    val status: NotificationStatus = NotificationStatus.PENDING,
    val extractedAmount: Double?,               // Parsed amount (nullable if parsing failed)
    val extractedMerchant: String?,             // Parsed merchant name
    val extractedCardLastFour: String? = null,  // Last 4 digits extracted from notification (for Google Wallet)
    val transactionType: String = "PURCHASE",   // PURCHASE, PIX_SENT, PIX_RECEIVED, TRANSFER, UNKNOWN
    val linkedItemId: Long? = null,             // FK -> CreditCardItem (after processing)
    val linkedCardId: Long? = null,             // FK -> CreditCard (suggested or selected card)
    val linkedBankId: Long? = null,             // FK -> Bank (for PIX/transfer transactions)
    val parsingConfidence: Float? = null,       // 0.0 to 1.0, how confident we are in the parsing
    val rawNotificationExtras: String? = null   // JSON string with extra notification data for debugging
)

