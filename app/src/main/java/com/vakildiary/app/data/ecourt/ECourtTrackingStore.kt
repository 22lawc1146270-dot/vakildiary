package com.vakildiary.app.data.ecourt

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vakildiary.app.domain.model.CourtType
import com.vakildiary.app.domain.model.ECourtTrackingInfo
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ECourtTrackingStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    suspend fun save(caseId: String, info: ECourtTrackingInfo) {
        dataStore.edit { prefs ->
            prefs[key(caseId)] = serialize(info)
        }
    }

    suspend fun updateStatus(caseId: String, stage: String?, nextDate: String?) {
        val current = get(caseId) ?: return
        save(caseId, current.copy(lastStage = stage, lastNextDate = nextDate))
    }

    suspend fun get(caseId: String): ECourtTrackingInfo? {
        val prefs = dataStore.data.first()
        val raw = prefs[key(caseId)] ?: return null
        return deserialize(raw)
    }

    suspend fun getAll(): Map<String, ECourtTrackingInfo> {
        val prefs = dataStore.data.first()
        return prefs.asMap().mapNotNull { (key, value) ->
            val name = key.name
            if (!name.startsWith(PREFIX)) return@mapNotNull null
            val caseId = name.removePrefix(PREFIX)
            val info = deserialize(value as? String ?: return@mapNotNull null) ?: return@mapNotNull null
            caseId to info
        }.toMap()
    }

    private fun key(caseId: String) = stringPreferencesKey("$PREFIX$caseId")

    private fun serialize(info: ECourtTrackingInfo): String {
        val type = info.courtType?.name.orEmpty()
        return listOf(
            info.stateCode,
            info.districtCode,
            info.courtCode,
            info.caseTypeCode,
            info.caseNumber,
            info.year,
            info.courtName,
            type,
            info.lastStage.orEmpty(),
            info.lastNextDate.orEmpty()
        ).joinToString(DELIMITER)
    }

    private fun deserialize(raw: String): ECourtTrackingInfo? {
        val parts = raw.split(DELIMITER)
        if (parts.size < 10) return null
        val courtType = parts[7].takeIf { it.isNotBlank() }?.let { CourtType.valueOf(it) }
        return ECourtTrackingInfo(
            stateCode = parts[0],
            districtCode = parts[1],
            courtCode = parts[2],
            caseTypeCode = parts[3],
            caseNumber = parts[4],
            year = parts[5],
            courtName = parts[6],
            courtType = courtType,
            lastStage = parts[8].takeIf { it.isNotBlank() },
            lastNextDate = parts[9].takeIf { it.isNotBlank() }
        )
    }

    companion object {
        private const val PREFIX = "ecourt_tracking_"
        private const val DELIMITER = "||"
    }
}
