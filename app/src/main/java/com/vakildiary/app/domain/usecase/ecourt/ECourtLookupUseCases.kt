package com.vakildiary.app.domain.usecase.ecourt

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.ECourtCaptcha
import com.vakildiary.app.domain.model.ECourtComplexOption
import com.vakildiary.app.domain.model.ECourtOption
import com.vakildiary.app.domain.model.ECourtSession
import com.vakildiary.app.domain.model.ECourtTokenResult
import com.vakildiary.app.domain.repository.ECourtRepository
import javax.inject.Inject

class FetchECourtSessionUseCase @Inject constructor(
    private val repository: ECourtRepository
) {
    suspend operator fun invoke(): Result<ECourtSession> = repository.fetchSession()
}

class FetchECourtDistrictsUseCase @Inject constructor(
    private val repository: ECourtRepository
) {
    suspend operator fun invoke(
        token: String,
        stateCode: String
    ): Result<ECourtTokenResult<List<ECourtOption>>> {
        return repository.fetchDistricts(token, stateCode)
    }
}

class FetchECourtCourtComplexesUseCase @Inject constructor(
    private val repository: ECourtRepository
) {
    suspend operator fun invoke(
        token: String,
        stateCode: String,
        districtCode: String
    ): Result<ECourtTokenResult<List<ECourtComplexOption>>> {
        return repository.fetchCourtComplexes(token, stateCode, districtCode)
    }
}

class FetchECourtEstablishmentsUseCase @Inject constructor(
    private val repository: ECourtRepository
) {
    suspend operator fun invoke(
        token: String,
        stateCode: String,
        districtCode: String,
        courtComplexCode: String
    ): Result<ECourtTokenResult<List<ECourtOption>>> {
        return repository.fetchEstablishments(token, stateCode, districtCode, courtComplexCode)
    }
}

class FetchECourtCaseTypesUseCase @Inject constructor(
    private val repository: ECourtRepository
) {
    suspend operator fun invoke(
        token: String,
        stateCode: String,
        districtCode: String,
        courtComplexCode: String,
        establishmentCode: String?
    ): Result<ECourtTokenResult<List<ECourtOption>>> {
        return repository.fetchCaseTypes(token, stateCode, districtCode, courtComplexCode, establishmentCode)
    }
}

class FetchECourtCaptchaUseCase @Inject constructor(
    private val repository: ECourtRepository
) {
    suspend operator fun invoke(token: String): Result<ECourtCaptcha> {
        return repository.fetchCaptcha(token)
    }
}
