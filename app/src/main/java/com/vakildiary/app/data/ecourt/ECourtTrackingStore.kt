package com.vakildiary.app.data.ecourt

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vakildiary.app.domain.model.CourtType
import com.vakildiary.app.domain.model.ECourtRecentEntries
import com.vakildiary.app.domain.model.ECourtTrackingInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    fun recentEntries(): Flow<ECourtRecentEntries> {
        return dataStore.data.map { prefs ->
            ECourtRecentEntries(
                stateCodes = decodeList(prefs[KEY_RECENT_STATE_CODES]),
                districtCodes = decodeList(prefs[KEY_RECENT_DISTRICT_CODES]),
                courtCodes = decodeList(prefs[KEY_RECENT_COURT_CODES]),
                caseTypeCodes = decodeList(prefs[KEY_RECENT_CASE_TYPES])
            )
        }
    }

    suspend fun saveRecentEntries(
        stateCode: String,
        districtCode: String,
        courtCode: String,
        caseTypeCode: String
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_RECENT_STATE_CODES] = updateList(prefs[KEY_RECENT_STATE_CODES], stateCode)
            prefs[KEY_RECENT_DISTRICT_CODES] = updateList(prefs[KEY_RECENT_DISTRICT_CODES], districtCode)
            prefs[KEY_RECENT_COURT_CODES] = updateList(prefs[KEY_RECENT_COURT_CODES], courtCode)
            prefs[KEY_RECENT_CASE_TYPES] = updateList(prefs[KEY_RECENT_CASE_TYPES], caseTypeCode)
        }
    }

    suspend fun saveLastCaptcha(captcha: String, csrfMagic: String?) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_CAPTCHA] = captcha
            if (csrfMagic.isNullOrBlank()) {
                prefs.remove(KEY_LAST_CSRF_MAGIC)
            } else {
                prefs[KEY_LAST_CSRF_MAGIC] = csrfMagic
            }
        }
    }

    suspend fun saveLastAppToken(token: String) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_APP_TOKEN] = token
        }
    }

    suspend fun getLastCaptcha(): String? {
        val prefs = dataStore.data.first()
        return prefs[KEY_LAST_CAPTCHA]
    }

    suspend fun getLastCsrfMagic(): String? {
        val prefs = dataStore.data.first()
        return prefs[KEY_LAST_CSRF_MAGIC]
    }

    suspend fun getLastAppToken(): String? {
        val prefs = dataStore.data.first()
        return prefs[KEY_LAST_APP_TOKEN]
    }

    private fun key(caseId: String) = stringPreferencesKey("$PREFIX$caseId")

    private fun serialize(info: ECourtTrackingInfo): String {
        val type = info.courtType?.name.orEmpty()
        return listOf(
            info.stateCode,
            info.districtCode,
            info.courtCode,
            info.establishmentCode.orEmpty(),
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
        val hasEstablishment = parts.size >= 11
        val establishmentCode = if (hasEstablishment) parts[3].takeIf { it.isNotBlank() } else null
        val offset = if (hasEstablishment) 1 else 0
        val courtTypeIndex = 7 + offset
        val courtType = parts.getOrNull(courtTypeIndex)?.takeIf { it.isNotBlank() }?.let { CourtType.valueOf(it) }
        return ECourtTrackingInfo(
            stateCode = parts[0],
            districtCode = parts[1],
            courtCode = parts[2],
            establishmentCode = establishmentCode,
            caseTypeCode = parts[3 + offset],
            caseNumber = parts[4 + offset],
            year = parts[5 + offset],
            courtName = parts[6 + offset],
            courtType = courtType,
            lastStage = parts[8 + offset].takeIf { it.isNotBlank() },
            lastNextDate = parts[9 + offset].takeIf { it.isNotBlank() }
        )
    }

    private fun updateList(existing: String?, value: String): String {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return existing.orEmpty()
        val items = decodeList(existing)
            .filterNot { it.equals(trimmed, ignoreCase = true) }
            .toMutableList()
        items.add(0, trimmed)
        return items.take(RECENT_LIMIT).joinToString(RECENT_DELIMITER)
    }

    private fun decodeList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(RECENT_DELIMITER).map { it.trim() }.filter { it.isNotBlank() }
    }

    companion object {
        private const val PREFIX = "ecourt_tracking_"
        private const val DELIMITER = "||"
        private val KEY_RECENT_STATE_CODES = stringPreferencesKey("ecourt_recent_state_codes")
        private val KEY_RECENT_DISTRICT_CODES = stringPreferencesKey("ecourt_recent_district_codes")
        private val KEY_RECENT_COURT_CODES = stringPreferencesKey("ecourt_recent_court_codes")
        private val KEY_RECENT_CASE_TYPES = stringPreferencesKey("ecourt_recent_case_types")
        private val KEY_LAST_CAPTCHA = stringPreferencesKey("ecourt_last_captcha")
        private val KEY_LAST_CSRF_MAGIC = stringPreferencesKey("ecourt_last_csrf")
        private val KEY_LAST_APP_TOKEN = stringPreferencesKey("ecourt_last_app_token")
        private const val RECENT_DELIMITER = "|"
        private const val RECENT_LIMIT = 6
    }
}
