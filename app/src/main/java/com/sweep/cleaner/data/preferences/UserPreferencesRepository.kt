package com.sweep.cleaner.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sweep_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val TUTORIAL_SEEN = booleanPreferencesKey("tutorial_seen")
        val SCHEDULED_SCAN_HOUR = intPreferencesKey("scheduled_scan_hour")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val LAST_SAF_TREE_URI = stringPreferencesKey("last_saf_tree_uri")
        val LAST_SAF_FOLDER_NAME = stringPreferencesKey("last_saf_folder_name")
        val LAST_JUNK_CLEAN_TIME = longPreferencesKey("last_junk_clean_time")
        val LAST_JUNK_CLEAN_BYTES = longPreferencesKey("last_junk_clean_bytes")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "light"
    }

    val tutorialSeen: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TUTORIAL_SEEN] ?: false
    }

    val scheduledScanHour: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SCHEDULED_SCAN_HOUR] ?: 9
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: false
    }

    val lastSafTreeUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_SAF_TREE_URI]
    }

    val lastSafFolderName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_SAF_FOLDER_NAME]
    }

    val lastJunkCleanTime: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_JUNK_CLEAN_TIME] ?: 0L
    }

    val lastJunkCleanBytes: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_JUNK_CLEAN_BYTES] ?: 0L
    }

    suspend fun setLastJunkClean(timestampMs: Long, reclaimedBytes: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_JUNK_CLEAN_TIME] = timestampMs
            preferences[PreferencesKeys.LAST_JUNK_CLEAN_BYTES] = reclaimedBytes
        }
    }

    suspend fun setLastSafTree(uriString: String?, folderName: String?) {
        context.dataStore.edit { preferences ->
            if (uriString != null) {
                preferences[PreferencesKeys.LAST_SAF_TREE_URI] = uriString
            } else {
                preferences.remove(PreferencesKeys.LAST_SAF_TREE_URI)
            }
            if (folderName != null) {
                preferences[PreferencesKeys.LAST_SAF_FOLDER_NAME] = folderName
            } else {
                preferences.remove(PreferencesKeys.LAST_SAF_FOLDER_NAME)
            }
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun setTutorialSeen(seen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TUTORIAL_SEEN] = seen
        }
    }

    suspend fun setScheduledScanHour(hour: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCHEDULED_SCAN_HOUR] = hour
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
}
