package ua.ofkkt.offlineshowcase.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Слайд 14: DataStore (Preferences) — прості пари «ключ → значення» + Flow. */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val store: DataStore<Preferences>) {

    val lastSync: Flow<Long> = store.data.map { it[LAST_SYNC] ?: 0L }
    val compactList: Flow<Boolean> = store.data.map { it[COMPACT] ?: false }
    val userName: Flow<String> = store.data.map { it[USER_NAME] ?: "Гість" }

    suspend fun saveLastSyncTime(time: Long) {
        store.edit { it[LAST_SYNC] = time }
    }

    suspend fun setCompactList(value: Boolean) {
        store.edit { it[COMPACT] = value }
    }

    suspend fun setUserName(value: String) {
        store.edit { it[USER_NAME] = value }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }

    private companion object {
        val LAST_SYNC = longPreferencesKey("last_sync")
        val COMPACT = booleanPreferencesKey("compact_list")
        val USER_NAME = stringPreferencesKey("user_name")
    }
}
