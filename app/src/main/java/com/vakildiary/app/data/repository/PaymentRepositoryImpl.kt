package com.vakildiary.app.data.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.data.local.dao.PaymentDao
import com.vakildiary.app.data.local.entities.PaymentEntity
import com.vakildiary.app.domain.model.Payment
import com.vakildiary.app.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PaymentRepositoryImpl @Inject constructor(
    private val paymentDao: PaymentDao
) : PaymentRepository {
    override suspend fun insertPayment(payment: Payment): Result<Unit> {
        return try {
            paymentDao.insertPayment(payment.toEntity())
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(message = "Failed to insert payment", throwable = t)
        }
    }

    override fun getPaymentsByCaseId(caseId: String): Flow<Result<List<Payment>>> {
        return paymentDao.getPaymentsByCaseId(caseId).map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override suspend fun deletePayment(payment: Payment): Result<Unit> {
        return try {
            paymentDao.deletePayment(payment.toEntity())
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(message = "Failed to delete payment", throwable = t)
        }
    }

    private fun Payment.toEntity(): PaymentEntity =
        PaymentEntity(
            paymentId = paymentId,
            caseId = caseId,
            amount = amount,
            paymentDate = paymentDate,
            paymentMode = paymentMode,
            referenceNumber = referenceNumber,
            receiptPath = receiptPath,
            notes = notes,
            createdAt = createdAt
        )

    private fun PaymentEntity.toDomain(): Payment =
        Payment(
            paymentId = paymentId,
            caseId = caseId,
            amount = amount,
            paymentDate = paymentDate,
            paymentMode = paymentMode,
            referenceNumber = referenceNumber,
            receiptPath = receiptPath,
            notes = notes,
            createdAt = createdAt
        )
}
