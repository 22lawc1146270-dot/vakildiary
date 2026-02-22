package com.vakildiary.app.data.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChecksumStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val KEY_DOC_CHECKSUMS = stringPreferencesKey("doc_checksums")

    suspend fun getChecksumMap(): Map<String, String> {
        val raw = dataStore.data.first()[KEY_DOC_CHECKSUMS] ?: return emptyMap()
        return raw.split("|")
            .mapNotNull { entry ->
                val parts = entry.split("=")
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .toMap()
    }

    suspend fun saveChecksumMap(map: Map<String, String>) {
        val raw = map.entries.joinToString("|") { "${it.key}=${it.value}" }
        dataStore.edit { prefs ->
            prefs[KEY_DOC_CHECKSUMS] = raw
        }
    }
}
