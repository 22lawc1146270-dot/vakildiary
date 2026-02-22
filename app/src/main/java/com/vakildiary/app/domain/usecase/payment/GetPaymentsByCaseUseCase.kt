package com.vakildiary.app.domain.usecase.payment

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Payment
import com.vakildiary.app.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPaymentsByCaseUseCase @Inject constructor(
    private val repository: PaymentRepository
) {
    operator fun invoke(caseId: String): Flow<Result<List<Payment>>> {
        return repository.getPaymentsByCaseId(caseId)
    }
}
