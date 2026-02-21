package com.vakildiary.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vakildiary.app.domain.model.CaseStage
import com.vakildiary.app.domain.model.CaseType
import com.vakildiary.app.domain.model.CourtType

@Entity(tableName = "cases")
data class CaseEntity(
    @PrimaryKey
    val caseId: String, // UUID
    val caseName: String,
    val caseNumber: String,
    val courtName: String,
    val courtType: CourtType,
    val clientName: String,
    val clientPhone: String?,
    val clientEmail: String?,
    val oppositeParty: String?,
    val caseType: CaseType,
    val caseStage: CaseStage,
    val assignedJudge: String?,
    val firNumber: String?,
    val actsAndSections: String?,
    val nextHearingDate: Long?, // Epoch
    val totalAgreedFees: Double?,
    val isECourtTracked: Boolean,
    val eCourtCaseId: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean = false
)
