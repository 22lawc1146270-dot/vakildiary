package com.vakildiary.app.data.remote.ecourt

import com.google.gson.annotations.SerializedName

data class ECourtListResponse(
    @SerializedName("dist_list") val districtList: String? = null,
    @SerializedName("complex_list") val complexList: String? = null,
    @SerializedName("establishment_list") val establishmentList: String? = null,
    @SerializedName("casetype_list") val caseTypeList: String? = null,
    @SerializedName("status") val status: Int? = null,
    @SerializedName("errormsg") val errorMessage: String? = null,
    @SerializedName("app_token") val appToken: String? = null
)

data class ECourtCaptchaResponse(
    @SerializedName("div_captcha") val captchaHtml: String? = null,
    @SerializedName("app_token") val appToken: String? = null,
    @SerializedName("errormsg") val errorMessage: String? = null
)

data class ECourtCaseSearchResponse(
    @SerializedName("case_data") val caseData: String? = null,
    @SerializedName("div_captcha") val captchaHtml: String? = null,
    @SerializedName("status") val status: Int? = null,
    @SerializedName("errormsg") val errorMessage: String? = null,
    @SerializedName("app_token") val appToken: String? = null
)
