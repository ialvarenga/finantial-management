package com.example.organizadordefinancas.data.model

enum class NotificationStatus {
    PENDING,    // Waiting for user review
    PROCESSED,  // User confirmed and created transaction
    IGNORED,    // User explicitly ignored
    FAILED,     // Parser could not extract info
    DUPLICATE   // Duplicate notification detected
}

