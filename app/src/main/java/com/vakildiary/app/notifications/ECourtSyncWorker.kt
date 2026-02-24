package com.vakildiary.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vakildiary.app.core.Result as AppResult
import com.vakildiary.app.domain.model.CaseStage
import com.vakildiary.app.domain.model.displayLabel
import com.vakildiary.app.presentation.model.ECourtParser
import com.vakildiary.app.presentation.model.ECourtSearchForm
import java.time.LocalDate
import java.time.ZoneId

class ECourtSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        NotificationChannels.ensureChannels(applicationContext)

        val caseDao = WorkerEntryPointAccessors.caseDao(applicationContext)
        val repository = WorkerEntryPointAccessors.ecourtRepository(applicationContext)
        val trackingStore = WorkerEntryPointAccessors.ecourtTrackingStore(applicationContext)
        val trackedCases = caseDao.getECourtTrackedCases()

        if (trackedCases.isEmpty()) return Result.success()

        val captcha = trackingStore.getLastCaptcha().orEmpty()
        var token = trackingStore.getLastAppToken().orEmpty()
        if (token.isBlank()) {
            when (val session = repository.fetchSession()) {
                is AppResult.Success -> {
                    token = session.data.token
                    trackingStore.saveLastAppToken(token)
                }
                is AppResult.Error -> {
                    notifyStatus(null, "eCourt Sync", "eCourt session unavailable. Open eCourt search to sync.")
                    return Result.success()
                }
            }
        }
        if (captcha.isBlank()) {
            notifyStatus(null, "eCourt Sync", "Captcha required. Open eCourt search to sync.")
            return Result.success()
        }

        var captchaInvalid = false
        trackedCases.forEach { caseEntity ->
            val tracking = trackingStore.get(caseEntity.caseId) ?: return@forEach
            val response = repository.searchCaseByNumber(
                token = token,
                stateCode = tracking.stateCode,
                districtCode = tracking.districtCode,
                courtComplexCode = tracking.courtCode,
                establishmentCode = tracking.establishmentCode,
                caseType = tracking.caseTypeCode,
                caseNumber = tracking.caseNumber,
                year = tracking.year,
                captcha = captcha
            )
            when (response) {
                is AppResult.Error -> return@forEach
                is AppResult.Success -> {
                    token = response.data.token
                    trackingStore.saveLastAppToken(token)
                    if (isCaptchaInvalid(response.data.caseHtml)) {
                        captchaInvalid = true
                        return@forEach
                    }
                    val form = ECourtSearchForm(
                        courtType = tracking.courtType,
                        courtName = tracking.courtName,
                        stateCode = tracking.stateCode,
                        districtCode = tracking.districtCode,
                        courtCode = tracking.courtCode,
                        establishmentCode = tracking.establishmentCode.orEmpty(),
                        caseType = tracking.caseTypeCode,
                        caseNumber = tracking.caseNumber,
                        year = tracking.year,
                        captcha = captcha
                    )
                    val parsed = ECourtParser.parse(response.data.caseHtml, form)
                    val item = parsed.firstOrNull { it.caseNumber == tracking.caseNumber }
                        ?: parsed.firstOrNull()
                        ?: return@forEach
                    val newStage = parseStage(item.stage) ?: caseEntity.caseStage
                    val newNextDate = parseDate(item.nextHearingDate) ?: caseEntity.nextHearingDate
                    val stageChanged = newStage != caseEntity.caseStage
                    val dateChanged = newNextDate != null && newNextDate != caseEntity.nextHearingDate
                    if (stageChanged || dateChanged) {
                        caseDao.updateCase(
                            caseEntity.copy(
                                caseStage = newStage,
                                customStage = null,
                                nextHearingDate = newNextDate,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        trackingStore.updateStatus(caseEntity.caseId, item.stage, item.nextHearingDate)
                        val message = buildString {
                            if (stageChanged) {
                                append(
                                    "Stage: ${caseEntity.caseStage.displayLabel(caseEntity.customStage)} → ${newStage.displayLabel()}"
                                )
                            }
                            if (dateChanged) {
                                if (isNotEmpty()) append(" • ")
                                append("Next: ${item.nextHearingDate}")
                            }
                        }.ifBlank { "Case status updated" }
                        notifyStatus(caseEntity.caseId, caseEntity.caseName, message)
                    } else {
                        trackingStore.updateStatus(caseEntity.caseId, item.stage, item.nextHearingDate)
                    }
                }
            }
        }

        if (captchaInvalid) {
            trackingStore.saveLastCaptcha("", null)
            notifyStatus(null, "eCourt Sync", "Captcha expired. Open eCourt search to refresh.")
        }
        return Result.success()
    }

    private fun notifyStatus(caseId: String?, title: String, message: String) {
        val intent = caseId?.let { NotificationIntents.caseDetail(applicationContext, it) }
        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext,
            NotificationChannels.ECOURT_ALERTS
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        androidx.core.app.NotificationManagerCompat.from(applicationContext)
            .notify((title + message).hashCode(), notification)
    }

    private fun parseStage(stage: String): CaseStage? {
        val normalized = stage.lowercase()
        return when {
            normalized.contains("disposed") -> CaseStage.DISPOSED
            normalized.contains("judgment") -> CaseStage.JUDGMENT
            normalized.contains("argument") -> CaseStage.ARGUMENTS
            normalized.contains("filing") -> CaseStage.FILING
            normalized.contains("hearing") -> CaseStage.HEARING
            else -> null
        }
    }

    private fun parseDate(value: String): Long? {
        if (value.isBlank()) return null
        return try {
            val parts = value.split("/")
            if (parts.size != 3) return null
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()
            LocalDate.of(year, month, day)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        } catch (t: Throwable) {
            null
        }
    }

    private fun isCaptchaInvalid(raw: String): Boolean {
        val normalized = raw.lowercase()
        return normalized.contains("invalid captcha") || normalized.contains("incorrect captcha")
    }
}
