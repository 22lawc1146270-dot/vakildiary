package com.vakildiary.app.domain.usecase.payment

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Payment
import com.vakildiary.app.domain.repository.PaymentRepository
import javax.inject.Inject

class AddPaymentUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(payment: Payment): Result<Unit> {
        return paymentRepository.insertPayment(payment)
    }
}
