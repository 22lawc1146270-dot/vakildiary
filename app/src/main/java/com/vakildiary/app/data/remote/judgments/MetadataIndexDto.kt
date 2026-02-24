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
    @SerializedName("name") val name: String? = null,
    @SerializedName("part") val part: String? = null,
    val etag: String?,
    val size: Long?,
    val files: List<String>
) {
    fun partName(): String? = name ?: part
}
