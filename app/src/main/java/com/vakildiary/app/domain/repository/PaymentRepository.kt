package com.vakildiary.app.domain.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Payment
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    suspend fun insertPayment(payment: Payment): Result<Unit>
    fun getPaymentsByCaseId(caseId: String): Flow<Result<List<Payment>>>
    suspend fun deletePayment(payment: Payment): Result<Unit>
    suspend fun deletePaymentsByCaseId(caseId: String): Result<Unit>
}
