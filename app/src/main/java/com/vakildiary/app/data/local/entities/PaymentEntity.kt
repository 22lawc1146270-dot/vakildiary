package com.vakildiary.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vakildiary.app.domain.model.PaymentMode

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = CaseEntity::class,
            parentColumns = ["caseId"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["caseId"])]
)
data class PaymentEntity(
    @PrimaryKey
    val paymentId: String, // UUID
    val caseId: String,
    val amount: Double,
    val paymentDate: Long, // Epoch
    val paymentMode: PaymentMode,
    val referenceNumber: String?,
    val receiptPath: String?,
    val notes: String,
    val createdAt: Long
)
