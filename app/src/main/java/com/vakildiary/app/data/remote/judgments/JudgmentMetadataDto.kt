package com.vakildiary.app.data.remote.judgments

import com.google.gson.annotations.SerializedName

data class JudgmentMetadataDto(
    @SerializedName("raw_html") val rawHtml: String? = null,
    val path: String? = null,
    @SerializedName("petitioner") val petitioner: String? = null,
    @SerializedName("respondent") val respondent: String? = null,
    @SerializedName("citation") val citation: String? = null,
    @SerializedName("decision_date") val decisionDate: String? = null,
    @SerializedName("case_id") val caseId: String? = null,
    @SerializedName("citation_year") val citationYear: Int? = null,
    @SerializedName("nc_display") val citationDisplay: String? = null,
    @SerializedName("scraped_at") val scrapedAt: String? = null
)
