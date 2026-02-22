package com.vakildiary.app.domain.usecase.documents

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.storage.DocumentStorage
import java.io.File
import javax.inject.Inject

class PrepareDocumentForViewingUseCase @Inject constructor(
    private val storage: DocumentStorage
) {
    suspend operator fun invoke(filePath: String): Result<File> {
        return storage.prepareFileForViewing(filePath)
    }
}
