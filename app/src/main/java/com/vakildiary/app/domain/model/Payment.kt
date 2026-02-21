package com.vakildiary.app.domain.model

data class Payment(
    val paymentId: String,
    val caseId: String,
    val amount: Double,
    val paymentDate: Long,
    val paymentMode: PaymentMode,
    val referenceNumber: String?,
    val receiptPath: String?,
    val notes: String,
    val createdAt: Long
)

enum class PaymentMode {
    CASH,
    UPI,
    CHEQUE,
    BANK_TRANSFER,
    OTHER
}
