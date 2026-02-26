package com.vakildiary.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vakildiary.app.domain.model.CourtType

@Entity(tableName = "ecourt_tracked_cases")
data class ECourtTrackedCaseEntity(
    @PrimaryKey
    val trackId: String,
    val caseTitle: String,
    val caseNumber: String,
    val year: String,
    val courtName: String,
    val courtType: CourtType?,
    val parties: String,
    val stage: String?,
    val nextHearingDate: String?,
    val caseDetails: String,
    val caseStatus: String,
    val petitionerAdvocate: String,
    val respondentAdvocate: String,
    val acts: String,
    val caseHistory: String,
    val transferDetails: String,
    val createdAt: Long
)
