package com.vakildiary.app.data.remote.judgments

import com.google.gson.annotations.SerializedName

data class JudgmentMetadataDto(
    @SerializedName("raw_html") val rawHtml: String? = null,
    val path: String? = null,
    @SerializedName("citation_year") val citationYear: Int? = null,
    @SerializedName("nc_display") val citationDisplay: String? = null,
    @SerializedName("scraped_at") val scrapedAt: String? = null
)
