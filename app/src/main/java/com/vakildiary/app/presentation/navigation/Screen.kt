package com.vakildiary.app.presentation.navigation

import android.net.Uri
import com.vakildiary.app.domain.model.CaseStage
import com.vakildiary.app.domain.model.CaseType
import com.vakildiary.app.domain.model.CourtType

sealed class Screen(val route: String) {
    object CaseList : Screen("case_list")
    object AddCase :
        Screen(
            "add_case?caseName={caseName}&caseNumber={caseNumber}&courtName={courtName}" +
                "&clientName={clientName}&courtType={courtType}&caseType={caseType}&caseStage={caseStage}" +
                "&ecourtStateCode={ecourtStateCode}&ecourtDistrictCode={ecourtDistrictCode}" +
                "&ecourtCourtCode={ecourtCourtCode}&ecourtEstablishmentCode={ecourtEstablishmentCode}" +
                "&ecourtCaseTypeCode={ecourtCaseTypeCode}" +
                "&ecourtYear={ecourtYear}"
        ) {
        const val ARG_CASE_NAME = "caseName"
        const val ARG_CASE_NUMBER = "caseNumber"
        const val ARG_COURT_NAME = "courtName"
        const val ARG_CLIENT_NAME = "clientName"
        const val ARG_COURT_TYPE = "courtType"
        const val ARG_CASE_TYPE = "caseType"
        const val ARG_CASE_STAGE = "caseStage"
        const val ARG_ECOURT_STATE_CODE = "ecourtStateCode"
        const val ARG_ECOURT_DISTRICT_CODE = "ecourtDistrictCode"
        const val ARG_ECOURT_COURT_CODE = "ecourtCourtCode"
        const val ARG_ECOURT_ESTABLISHMENT_CODE = "ecourtEstablishmentCode"
        const val ARG_ECOURT_CASE_TYPE_CODE = "ecourtCaseTypeCode"
        const val ARG_ECOURT_YEAR = "ecourtYear"

        fun createRoute(
            caseName: String? = null,
            caseNumber: String? = null,
            courtName: String? = null,
            clientName: String? = null,
            courtType: CourtType? = null,
            caseType: CaseType? = null,
            caseStage: CaseStage? = null,
            ecourtStateCode: String? = null,
            ecourtDistrictCode: String? = null,
            ecourtCourtCode: String? = null,
            ecourtEstablishmentCode: String? = null,
            ecourtCaseTypeCode: String? = null,
            ecourtYear: String? = null
        ): String {
            return "add_case?caseName=${encode(caseName)}&caseNumber=${encode(caseNumber)}" +
                "&courtName=${encode(courtName)}&clientName=${encode(clientName)}" +
                "&courtType=${encode(courtType?.name)}&caseType=${encode(caseType?.name)}" +
                "&caseStage=${encode(caseStage?.name)}&ecourtStateCode=${encode(ecourtStateCode)}" +
                "&ecourtDistrictCode=${encode(ecourtDistrictCode)}&ecourtCourtCode=${encode(ecourtCourtCode)}" +
                "&ecourtEstablishmentCode=${encode(ecourtEstablishmentCode)}" +
                "&ecourtCaseTypeCode=${encode(ecourtCaseTypeCode)}&ecourtYear=${encode(ecourtYear)}"
        }
    }
    object AddHearing : Screen("add_hearing?caseId={caseId}") {
        const val ARG_CASE_ID = "caseId"
        fun createRoute(caseId: String? = null): String {
            return if (caseId.isNullOrBlank()) "add_hearing" else "add_hearing?caseId=$caseId"
        }
    }
    object AddTask : Screen("add_task?caseId={caseId}") {
        const val ARG_CASE_ID = "caseId"
        fun createRoute(caseId: String?): String {
            return if (caseId.isNullOrBlank()) "add_task" else "add_task?caseId=$caseId"
        }
    }
    object AddPayment : Screen("add_payment/{caseId}") {
        const val ARG_CASE_ID = "caseId"
        fun createRoute(caseId: String): String = "add_payment/$caseId"
    }
    object CaseDetail : Screen("case_detail/{caseId}") {
        const val ARG_CASE_ID = "caseId"
        fun createRoute(caseId: String): String = "case_detail/$caseId"
    }
    object EditCase : Screen("edit_case/{caseId}") {
        const val ARG_CASE_ID = "caseId"
        fun createRoute(caseId: String): String = "edit_case/$caseId"
    }
    object CaseDocuments : Screen("case_documents/{caseId}") {
        const val ARG_CASE_ID = "caseId"
        fun createRoute(caseId: String): String = "case_documents/$caseId"
    }
    object MeetingList : Screen("meeting_list/{caseId}") {
        const val ARG_CASE_ID = "caseId"
        fun createRoute(caseId: String): String = "meeting_list/$caseId"
    }
    object AddMeeting : Screen("add_meeting/{caseId}") {
        const val ARG_CASE_ID = "caseId"
        fun createRoute(caseId: String): String = "add_meeting/$caseId"
    }
    object UpcomingMeetings : Screen("upcoming_meetings")
    object ECourtSearch : Screen("ecourt_search")
    object JudgmentSearch : Screen("judgment_search")
    object ReportableJudgment : Screen(
        "reportable_judgment?judgmentId={judgmentId}&caseNumber={caseNumber}&year={year}"
    ) {
        const val ARG_JUDGMENT_ID = "judgmentId"
        const val ARG_CASE_NUMBER = "caseNumber"
        const val ARG_YEAR = "year"

        fun createRoute(
            judgmentId: String,
            caseNumber: String? = null,
            year: String? = null
        ): String {
            return "reportable_judgment?judgmentId=${encode(judgmentId)}" +
                "&caseNumber=${encode(caseNumber)}&year=${encode(year)}"
        }
    }
    object Dashboard : Screen("dashboard")
    object Calendar : Screen("calendar")
    object Documents : Screen("documents")
    object More : Screen("more")
    object OverdueTasks : Screen("overdue_tasks")
    object BackupStatus : Screen("backup_status")
}

private fun encode(value: String?): String = Uri.encode(value.orEmpty())
