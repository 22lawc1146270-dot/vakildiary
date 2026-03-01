package com.vakildiary.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vakildiary.app.presentation.screens.cases.AddCaseScreen
import com.vakildiary.app.presentation.screens.cases.CaseDetailScreen
import com.vakildiary.app.presentation.screens.cases.CaseListScreen
import com.vakildiary.app.presentation.screens.cases.EditCaseScreen
import com.vakildiary.app.presentation.screens.hearings.AddHearingScreen
import com.vakildiary.app.presentation.screens.documents.DocumentListScreen
import com.vakildiary.app.presentation.screens.home.DashboardScreen
import com.vakildiary.app.presentation.screens.more.MoreScreen
import com.vakildiary.app.presentation.screens.calendar.CalendarScreen
import com.vakildiary.app.presentation.screens.tasks.AddTaskScreen
import com.vakildiary.app.presentation.screens.tasks.OverdueTasksScreen
import com.vakildiary.app.presentation.screens.fees.AddPaymentScreen
import com.vakildiary.app.presentation.screens.ecourt.ECourtSearchScreen
import com.vakildiary.app.presentation.screens.judgments.JudgmentSearchScreen
import com.vakildiary.app.presentation.screens.judgments.ReportableJudgmentScreen
import com.vakildiary.app.presentation.screens.meetings.AddMeetingScreen
import com.vakildiary.app.presentation.screens.meetings.MeetingListScreen
import com.vakildiary.app.presentation.screens.meetings.UpcomingMeetingsScreen
import com.vakildiary.app.presentation.screens.backup.BackupStatusScreen
import com.vakildiary.app.domain.model.CaseStage
import com.vakildiary.app.domain.model.CaseType
import com.vakildiary.app.domain.model.CourtType

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
    onOpenDocket: () -> Unit = {},
    docketPendingCount: Int = 0
) {
    NavHost(
        navController = navController,
        startDestination = Screen.CaseList.route,
        modifier = modifier
    ) {
        composable(Screen.CaseList.route) {
            CaseListScreen(
                onAddCase = { navController.navigate(Screen.AddCase.route) },
                onOpenCase = { caseId -> navController.navigate(Screen.CaseDetail.createRoute(caseId)) }
            )
        }
        composable(
            route = Screen.AddCase.route,
            arguments = listOf(
                navArgument(Screen.AddCase.ARG_CASE_NAME) { defaultValue = ""; nullable = true },
                navArgument(Screen.AddCase.ARG_CASE_NUMBER) { defaultValue = ""; nullable = true },
                navArgument(Screen.AddCase.ARG_COURT_NAME) { defaultValue = ""; nullable = true },
                navArgument(Screen.AddCase.ARG_CLIENT_NAME) { defaultValue = ""; nullable = true },
                navArgument(Screen.AddCase.ARG_COURT_TYPE) { defaultValue = ""; nullable = true },
                navArgument(Screen.AddCase.ARG_CASE_TYPE) { defaultValue = ""; nullable = true },
                navArgument(Screen.AddCase.ARG_CASE_STAGE) { defaultValue = ""; nullable = true },
                navArgument(Screen.AddCase.ARG_ECOURT_STATE_CODE) { defaultValue = ""; nullable = true },
                navArgument(Screen.AddCase.ARG_ECOURT_DISTRICT_CODE) { defaultValue = ""; nullable = true },
                navArgument(Screen.AddCase.ARG_ECOURT_COURT_CODE) { defaultValue = ""; nullable = true },
                navArgument(Screen.AddCase.ARG_ECOURT_ESTABLISHMENT_CODE) { defaultValue = ""; nullable = true },
                navArgument(Screen.AddCase.ARG_ECOURT_CASE_TYPE_CODE) { defaultValue = ""; nullable = true },
                navArgument(Screen.AddCase.ARG_ECOURT_YEAR) { defaultValue = ""; nullable = true }
            )
        ) { entry ->
            val caseName = entry.arguments?.getString(Screen.AddCase.ARG_CASE_NAME)
            val caseNumber = entry.arguments?.getString(Screen.AddCase.ARG_CASE_NUMBER)
            val courtName = entry.arguments?.getString(Screen.AddCase.ARG_COURT_NAME)
            val clientName = entry.arguments?.getString(Screen.AddCase.ARG_CLIENT_NAME)
            val courtType = parseEnum(entry.arguments?.getString(Screen.AddCase.ARG_COURT_TYPE), CourtType.values())
            val caseType = parseEnum(entry.arguments?.getString(Screen.AddCase.ARG_CASE_TYPE), CaseType.values())
            val caseStage = parseEnum(entry.arguments?.getString(Screen.AddCase.ARG_CASE_STAGE), CaseStage.values())
            val ecourtState = entry.arguments?.getString(Screen.AddCase.ARG_ECOURT_STATE_CODE)
            val ecourtDistrict = entry.arguments?.getString(Screen.AddCase.ARG_ECOURT_DISTRICT_CODE)
            val ecourtCourt = entry.arguments?.getString(Screen.AddCase.ARG_ECOURT_COURT_CODE)
            val ecourtEstablishment = entry.arguments?.getString(Screen.AddCase.ARG_ECOURT_ESTABLISHMENT_CODE)
            val ecourtCaseType = entry.arguments?.getString(Screen.AddCase.ARG_ECOURT_CASE_TYPE_CODE)
            val ecourtYear = entry.arguments?.getString(Screen.AddCase.ARG_ECOURT_YEAR)
            AddCaseScreen(
                prefillCaseName = caseName,
                prefillCaseNumber = caseNumber,
                prefillCourtName = courtName,
                prefillClientName = clientName,
                prefillCourtType = courtType,
                prefillCaseType = caseType,
                prefillCaseStage = caseStage,
                prefillEcourtStateCode = ecourtState,
                prefillEcourtDistrictCode = ecourtDistrict,
                prefillEcourtCourtCode = ecourtCourt,
                prefillEcourtEstablishmentCode = ecourtEstablishment,
                prefillEcourtCaseTypeCode = ecourtCaseType,
                prefillEcourtYear = ecourtYear,
                onBack = { navController.popBackStack() },
                onRegistered = {
                    val popped = navController.popBackStack(Screen.CaseList.route, false)
                    if (!popped) {
                        navController.navigate(Screen.CaseList.route)
                    }
                }
            )
        }
        composable(
            route = Screen.AddHearing.route,
            arguments = listOf(navArgument(Screen.AddHearing.ARG_CASE_ID) {
                type = NavType.StringType
                defaultValue = ""
                nullable = true
            })
        ) {
            val caseId = it.arguments?.getString(Screen.AddHearing.ARG_CASE_ID).orEmpty()
            AddHearingScreen(
                preselectedCaseId = caseId.ifBlank { null },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.AddTask.route,
            arguments = listOf(navArgument(Screen.AddTask.ARG_CASE_ID) {
                type = NavType.StringType
                defaultValue = ""
                nullable = true
            })
        ) {
            val caseId = it.arguments?.getString(Screen.AddTask.ARG_CASE_ID).orEmpty()
            AddTaskScreen(
                initialCaseId = caseId.ifBlank { null },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EditCase.route,
            arguments = listOf(navArgument(Screen.EditCase.ARG_CASE_ID) { type = NavType.StringType })
        ) {
            val caseId = it.arguments?.getString(Screen.EditCase.ARG_CASE_ID).orEmpty()
            EditCaseScreen(
                caseId = caseId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.CaseDetail.route,
            arguments = listOf(navArgument(Screen.CaseDetail.ARG_CASE_ID) { type = NavType.StringType })
        ) {
            val caseId = it.arguments?.getString(Screen.CaseDetail.ARG_CASE_ID).orEmpty()
            CaseDetailScreen(
                caseId = caseId,
                onAddTask = { navController.navigate(Screen.AddTask.createRoute(caseId)) },
                onAddHearing = { navController.navigate(Screen.AddHearing.createRoute(caseId)) },
                onAddPayment = { navController.navigate(Screen.AddPayment.createRoute(caseId)) },
                onAddDocument = { navController.navigate(Screen.CaseDocuments.createRoute(caseId)) },
                onEdit = { navController.navigate(Screen.EditCase.createRoute(caseId)) },
                onAddMeeting = { navController.navigate(Screen.AddMeeting.createRoute(caseId)) },
                onCaseDeleted = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.CaseDocuments.route,
            arguments = listOf(navArgument(Screen.CaseDocuments.ARG_CASE_ID) { type = NavType.StringType })
        ) {
            val caseId = it.arguments?.getString(Screen.CaseDocuments.ARG_CASE_ID).orEmpty()
            DocumentListScreen(
                caseId = caseId,
                showBack = true,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.MeetingList.route,
            arguments = listOf(navArgument(Screen.MeetingList.ARG_CASE_ID) { type = NavType.StringType })
        ) {
            val caseId = it.arguments?.getString(Screen.MeetingList.ARG_CASE_ID).orEmpty()
            MeetingListScreen(
                caseId = caseId,
                showTopBar = true,
                showBack = true,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.AddMeeting.route,
            arguments = listOf(navArgument(Screen.AddMeeting.ARG_CASE_ID) { type = NavType.StringType })
        ) {
            val caseId = it.arguments?.getString(Screen.AddMeeting.ARG_CASE_ID).orEmpty()
            AddMeetingScreen(
                caseId = caseId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.UpcomingMeetings.route) {
            UpcomingMeetingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.AddPayment.route,
            arguments = listOf(navArgument(Screen.AddPayment.ARG_CASE_ID) { type = NavType.StringType })
        ) {
            val caseId = it.arguments?.getString(Screen.AddPayment.ARG_CASE_ID).orEmpty()
            AddPaymentScreen(
                caseId = caseId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onOpenOverdue = { navController.navigate(Screen.OverdueTasks.route) },
                onOpenDocket = onOpenDocket,
                onAddTask = { navController.navigate(Screen.AddTask.createRoute(null)) },
                docketPendingCount = docketPendingCount
            )
        }
        composable(Screen.Calendar.route) {
            CalendarScreen(
                onAddTask = { navController.navigate(Screen.AddTask.createRoute(null)) }
            )
        }
        composable(Screen.Documents.route) {
            DocumentListScreen(
                onDownloadReportable = { judgmentId, caseNumber, year, petitionerName, judgmentDate ->
                    navController.navigate(
                        Screen.ReportableJudgment.createRoute(
                            judgmentId = judgmentId,
                            caseNumber = caseNumber,
                            year = year,
                            petitionerName = petitionerName,
                            judgmentDate = judgmentDate
                        )
                    )
                }
            )
        }
        composable(Screen.OverdueTasks.route) {
            OverdueTasksScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.ECourtSearch.route) {
            ECourtSearchScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.JudgmentSearch.route) {
            JudgmentSearchScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.ReportableJudgment.route,
            arguments = listOf(
                navArgument(Screen.ReportableJudgment.ARG_JUDGMENT_ID) { defaultValue = "" },
                navArgument(Screen.ReportableJudgment.ARG_CASE_NUMBER) { defaultValue = ""; nullable = true },
                navArgument(Screen.ReportableJudgment.ARG_YEAR) { defaultValue = ""; nullable = true },
                navArgument(Screen.ReportableJudgment.ARG_PETITIONER_NAME) {
                    defaultValue = ""
                    nullable = true
                },
                navArgument(Screen.ReportableJudgment.ARG_JUDGMENT_DATE) {
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { entry ->
            val judgmentId = entry.arguments?.getString(Screen.ReportableJudgment.ARG_JUDGMENT_ID).orEmpty()
            val caseNumber = entry.arguments?.getString(Screen.ReportableJudgment.ARG_CASE_NUMBER)
            val year = entry.arguments?.getString(Screen.ReportableJudgment.ARG_YEAR)
            val petitionerName = entry.arguments?.getString(Screen.ReportableJudgment.ARG_PETITIONER_NAME)
            val judgmentDate = entry.arguments?.getString(Screen.ReportableJudgment.ARG_JUDGMENT_DATE)
            ReportableJudgmentScreen(
                judgmentId = judgmentId,
                caseNumber = caseNumber,
                year = year,
                petitionerName = petitionerName,
                judgmentDate = judgmentDate,
                onBack = { navController.popBackStack() },
                onOpenDocuments = {
                    val popped = navController.popBackStack(Screen.Documents.route, false)
                    if (!popped) {
                        navController.navigate(Screen.Documents.route)
                    }
                }
            )
        }
        composable(Screen.More.route) {
            MoreScreen(
                onOpenECourt = { navController.navigate(Screen.ECourtSearch.route) },
                onOpenJudgments = { navController.navigate(Screen.JudgmentSearch.route) },
                onOpenBackupStatus = { navController.navigate(Screen.BackupStatus.route) },
                onOpenUpcomingMeetings = { navController.navigate(Screen.UpcomingMeetings.route) }
            )
        }
        composable(Screen.BackupStatus.route) {
            BackupStatusScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun <T : Enum<T>> parseEnum(value: String?, values: Array<T>): T? {
    if (value.isNullOrBlank()) return null
    return values.firstOrNull { it.name == value }
}
