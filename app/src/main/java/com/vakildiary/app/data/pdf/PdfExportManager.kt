package com.vakildiary.app.data.pdf

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.itextpdf.text.Document
import com.itextpdf.text.Font
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Case
import com.vakildiary.app.domain.model.HearingHistory
import com.vakildiary.app.domain.model.Payment
import com.vakildiary.app.domain.model.displayLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfExportManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun exportCaseSummary(case: Case): Result<File> = withContext(Dispatchers.IO) {
        val file = createOutputFile("case_summary_${case.caseNumber}.pdf")
        return@withContext try {
            val doc = Document()
            PdfWriter.getInstance(doc, FileOutputStream(file))
            doc.open()
            val titleFont = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD)
            val bodyFont = Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL)

            doc.add(Paragraph("Case Summary", titleFont))
            doc.add(Paragraph("Case Name: ${case.caseName}", bodyFont))
            doc.add(Paragraph("Case Number: ${case.caseNumber}", bodyFont))
            doc.add(Paragraph("Court: ${case.courtName}", bodyFont))
            doc.add(Paragraph("Client: ${case.clientName.ifBlank { "Not set" }}", bodyFont))
            doc.add(Paragraph("Stage: ${case.caseStage.displayLabel(case.customStage)}", bodyFont))
            doc.add(Paragraph("Next Hearing: ${formatDate(case.nextHearingDate)}", bodyFont))
            doc.close()
            Result.Success(file)
        } catch (t: Throwable) {
            Result.Error("Failed to export case summary", t)
        }
    }

    suspend fun exportFeeLedger(
        case: Case,
        payments: List<Payment>
    ): Result<File> = withContext(Dispatchers.IO) {
        val file = createOutputFile("fee_ledger_${case.caseNumber}.pdf")
        return@withContext try {
            val doc = Document()
            PdfWriter.getInstance(doc, FileOutputStream(file))
            doc.open()
            val titleFont = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD)
            val bodyFont = Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL)

            doc.add(Paragraph("Fee Ledger", titleFont))
            doc.add(Paragraph("Case: ${case.caseName} (${case.caseNumber})", bodyFont))
            doc.add(Paragraph("Client: ${case.clientName.ifBlank { "Not set" }}", bodyFont))

            val table = PdfPTable(3)
            table.addCell("Date")
            table.addCell("Amount")
            table.addCell("Mode")
            payments.sortedBy { it.paymentDate }.forEach { payment ->
                table.addCell(formatDate(payment.paymentDate))
                table.addCell("₹${payment.amount}")
                table.addCell(payment.paymentMode.name)
            }
            doc.add(table)
            doc.close()
            Result.Success(file)
        } catch (t: Throwable) {
            Result.Error("Failed to export fee ledger", t)
        }
    }

    suspend fun exportCaseHistory(
        case: Case,
        hearings: List<HearingHistory>
    ): Result<File> = withContext(Dispatchers.IO) {
        val file = createOutputFile("case_history_${case.caseNumber}.pdf")
        return@withContext try {
            val doc = Document()
            PdfWriter.getInstance(doc, FileOutputStream(file))
            doc.open()
            val titleFont = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD)
            val bodyFont = Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL)

            doc.add(Paragraph("Case History", titleFont))
            doc.add(Paragraph("Case: ${case.caseName} (${case.caseNumber})", bodyFont))
            doc.add(Paragraph("Client: ${case.clientName.ifBlank { "Not set" }}", bodyFont))
            doc.add(Paragraph("Court: ${case.courtName}", bodyFont))

            val table = PdfPTable(5)
            table.addCell("Date")
            table.addCell("Purpose")
            table.addCell("Outcome")
            table.addCell("Order")
            table.addCell("Next Date")
            hearings.sortedBy { it.hearingDate }.forEach { hearing ->
                table.addCell(formatDate(hearing.hearingDate))
                table.addCell(hearing.purpose)
                table.addCell(hearing.outcome ?: "-")
                table.addCell(hearing.orderDetails ?: "-")
                table.addCell(formatDate(hearing.nextDateGiven))
            }
            doc.add(table)
            doc.close()
            Result.Success(file)
        } catch (t: Throwable) {
            Result.Error("Failed to export case history", t)
        }
    }

    private fun createOutputFile(name: String): File {
        val dir = File(context.filesDir, OUTPUT_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, name)
    }

    private fun formatDate(epochMillis: Long?): String {
        if (epochMillis == null) return "--"
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formatter.format(Date(epochMillis))
    }

    companion object {
        private const val OUTPUT_DIR = "pdf_exports"
    }
}
