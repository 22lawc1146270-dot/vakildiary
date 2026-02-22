package com.vakildiary.app.domain.usecase.pdf

import com.vakildiary.app.core.Result
import com.vakildiary.app.data.pdf.PdfExportManager
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.model.HearingHistory
import java.io.File
import javax.inject.Inject

class ExportCaseHistoryUseCase @Inject constructor(
    private val pdfExportManager: PdfExportManager
) {
    suspend operator fun invoke(case: Case, hearings: List<HearingHistory>): Result<File> {
        return pdfExportManager.exportCaseHistory(case, hearings)
    }
}
