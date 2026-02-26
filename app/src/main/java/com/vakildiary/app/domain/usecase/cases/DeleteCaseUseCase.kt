package com.vakildiary.app.domain.usecase.cases

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.repository.HearingRepository
import com.vakildiary.app.domain.repository.MeetingRepository
import com.vakildiary.app.domain.repository.PaymentRepository
import com.vakildiary.app.domain.repository.TaskRepository
import com.vakildiary.app.domain.repository.CaseRepository
import com.vakildiary.app.domain.usecase.documents.DeleteDocumentUseCase
import com.vakildiary.app.domain.usecase.documents.GetDocumentsByCaseIdUseCase
import com.vakildiary.app.domain.usecase.hearing.GetHearingHistoryUseCase
import com.vakildiary.app.domain.usecase.payment.GetPaymentsByCaseUseCase
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject

class DeleteCaseUseCase @Inject constructor(
    private val caseRepository: CaseRepository,
    private val getDocumentsByCaseIdUseCase: GetDocumentsByCaseIdUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val getPaymentsByCaseUseCase: GetPaymentsByCaseUseCase,
    private val getHearingHistoryUseCase: GetHearingHistoryUseCase,
    private val taskRepository: TaskRepository,
    private val hearingRepository: HearingRepository,
    private val meetingRepository: MeetingRepository,
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(caseId: String): Result<Unit> {
        var warning: String? = null

        when (val documentsResult = getDocumentsByCaseIdUseCase(caseId).first()) {
            is Result.Success -> {
                documentsResult.data.forEach { document ->
                    val deleteResult = deleteDocumentUseCase(document)
                    if (deleteResult is Result.Error && warning == null) {
                        warning = deleteResult.message
                    }
                }
            }
            is Result.Error -> if (warning == null) {
                warning = documentsResult.message
            }
        }

        when (val hearingResult = getHearingHistoryUseCase(caseId).first()) {
            is Result.Success -> {
                hearingResult.data.forEach { hearing ->
                    val voicePath = hearing.voiceNotePath
                    if (!voicePath.isNullOrBlank()) {
                        val file = File(voicePath)
                        if (file.exists() && !file.delete() && warning == null) {
                            warning = "Failed to delete some voice note files"
                        }
                    }
                }
            }
            is Result.Error -> if (warning == null) {
                warning = hearingResult.message
            }
        }

        when (val taskDeleteResult = taskRepository.deleteTasksByCaseId(caseId)) {
            is Result.Error -> if (warning == null) {
                warning = taskDeleteResult.message
            }
            else -> Unit
        }

        when (val meetingDeleteResult = meetingRepository.deleteMeetingsByCaseId(caseId)) {
            is Result.Error -> if (warning == null) {
                warning = meetingDeleteResult.message
            }
            else -> Unit
        }

        when (val paymentsResult = getPaymentsByCaseUseCase(caseId).first()) {
            is Result.Success -> {
                paymentsResult.data.forEach { payment ->
                    val receiptPath = payment.receiptPath
                    if (!receiptPath.isNullOrBlank()) {
                        val file = File(receiptPath)
                        if (file.exists() && !file.delete() && warning == null) {
                            warning = "Failed to delete some receipt files"
                        }
                    }
                }
            }
            is Result.Error -> if (warning == null) {
                warning = paymentsResult.message
            }
        }

        when (val paymentsDeleteResult = paymentRepository.deletePaymentsByCaseId(caseId)) {
            is Result.Error -> if (warning == null) {
                warning = paymentsDeleteResult.message
            }
            else -> Unit
        }

        when (val hearingsDeleteResult = hearingRepository.deleteHearingsByCaseId(caseId)) {
            is Result.Error -> if (warning == null) {
                warning = hearingsDeleteResult.message
            }
            else -> Unit
        }

        val deleteResult = caseRepository.deleteCase(caseId)
        if (deleteResult is Result.Error) return deleteResult

        return if (warning != null) Result.Error(warning!!) else Result.Success(Unit)
    }
}
