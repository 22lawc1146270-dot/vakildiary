package com.vakildiary.app.data.remote.judgments

import com.google.gson.annotations.SerializedName

/**
 * S3 metadata index for a given year. Structure documented in the dataset repo.
 */
data class MetadataIndexDto(
    @SerializedName("_type") val type: String?,
    @SerializedName("_version") val version: String?,
    val splitter: String?,
    val parts: List<MetadataIndexPartDto>
)

data class MetadataIndexPartDto(
    val part: String?,
    val etag: String?,
    val size: Long?,
    val files: List<String>
)
