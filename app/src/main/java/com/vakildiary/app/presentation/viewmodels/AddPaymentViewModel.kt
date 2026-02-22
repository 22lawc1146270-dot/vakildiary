package com.vakildiary.app.presentation.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Payment
import com.vakildiary.app.domain.model.PaymentMode
import com.vakildiary.app.domain.usecase.cases.GetCaseByIdUseCase
import com.vakildiary.app.domain.usecase.payment.AddPaymentUseCase
import com.vakildiary.app.domain.usecase.payment.GetPaymentsByCaseUseCase
import com.vakildiary.app.presentation.viewmodels.state.AddPaymentUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddPaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addPaymentUseCase: AddPaymentUseCase,
    private val getCaseByIdUseCase: GetCaseByIdUseCase,
    private val getPaymentsByCaseUseCase: GetPaymentsByCaseUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val caseId: String = savedStateHandle[ARG_CASE_ID] ?: ""

    private val _formState = MutableStateFlow(AddPaymentFormState())
    val formState: StateFlow<AddPaymentFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<AddPaymentUiState>(AddPaymentUiState.Success(false))
    val uiState: StateFlow<AddPaymentUiState> = _uiState.asStateFlow()

    val outstandingAmount: StateFlow<Double?> = combine(
        getCaseByIdUseCase(caseId),
        getPaymentsByCaseUseCase(caseId)
    ) { caseResult, paymentResult ->
        val case = (caseResult as? Result.Success)?.data
        val payments = (paymentResult as? Result.Success)?.data.orEmpty()
        val agreed = case?.totalAgreedFees
        if (agreed != null) {
            (agreed - payments.sumOf { it.amount }).coerceAtLeast(0.0)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onAmountChanged(value: String) = updateForm { it.copy(amount = value) }
    fun onDateChanged(value: Long) = updateForm { it.copy(paymentDateMillis = value) }
    fun onPaymentModeChanged(value: PaymentMode) = updateForm { it.copy(paymentMode = value) }
    fun onReferenceNumberChanged(value: String) = updateForm { it.copy(referenceNumber = value) }
    fun onReceiptPathChanged(value: String) = updateForm { it.copy(receiptPath = value) }

    fun onReceiptSelected(uri: Uri) {
        viewModelScope.launch {
            val path = copyReceiptToInternalStorage(uri)
            if (path == null) {
                _uiState.value = AddPaymentUiState.Error("Failed to attach receipt")
            } else {
                onReceiptPathChanged(path)
            }
        }
    }

    fun onSave() {
        val state = formState.value
        if (!state.isValid() || caseId.isBlank()) {
            _uiState.value = AddPaymentUiState.Error("Please fill all mandatory fields")
            return
        }

        val amount = state.amount.trim().toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _uiState.value = AddPaymentUiState.Error("Amount must be a positive number")
            return
        }

        val outstanding = outstandingAmount.value
        if (outstanding != null && amount > outstanding) {
            _uiState.value = AddPaymentUiState.Error("Amount exceeds outstanding balance")
            return
        }

        viewModelScope.launch {
            _uiState.value = AddPaymentUiState.Loading
            val now = System.currentTimeMillis()
            val payment = Payment(
                paymentId = UUID.randomUUID().toString(),
                caseId = caseId,
                amount = amount,
                paymentDate = state.paymentDateMillis!!,
                paymentMode = state.paymentMode!!,
                referenceNumber = state.referenceNumber.trim().ifBlank { null },
                receiptPath = state.receiptPath.trim().ifBlank { null },
                notes = "",
                createdAt = now
            )

            _uiState.value = when (val result = addPaymentUseCase(payment)) {
                is Result.Success -> AddPaymentUiState.Success(true)
                is Result.Error -> AddPaymentUiState.Error(result.message)
            }
        }
    }

    private fun updateForm(block: (AddPaymentFormState) -> AddPaymentFormState) {
        _formState.value = block(_formState.value)
    }

    private suspend fun copyReceiptToInternalStorage(uri: Uri): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val receiptsDir = File(context.filesDir, "receipts")
            if (!receiptsDir.exists()) receiptsDir.mkdirs()
            val outputFile = File(receiptsDir, "receipt_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            outputFile.absolutePath
        } catch (t: Throwable) {
            null
        }
    }

    companion object {
        const val ARG_CASE_ID = "caseId"
    }
}

data class AddPaymentFormState(
    val amount: String = "",
    val paymentDateMillis: Long? = null,
    val paymentMode: PaymentMode? = null,
    val referenceNumber: String = "",
    val receiptPath: String = ""
) {
    fun isValid(): Boolean {
        return amount.isNotBlank() && paymentDateMillis != null && paymentMode != null
    }
}
