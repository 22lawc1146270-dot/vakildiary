package com.vakildiary.app.domain.usecase.pdf

import com.vakildiary.app.core.Result
import com.vakildiary.app.data.pdf.PdfExportManager
import com.vakildiary.app.domain.model.Case
import java.io.File
import javax.inject.Inject

class ExportCaseSummaryUseCase @Inject constructor(
    private val pdfExportManager: PdfExportManager
) {
    suspend operator fun invoke(case: Case): Result<File> {
        return pdfExportManager.exportCaseSummary(case)
    }
}
