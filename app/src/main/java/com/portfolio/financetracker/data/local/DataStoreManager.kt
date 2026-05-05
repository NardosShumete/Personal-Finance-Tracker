package com.portfolio.financetracker.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class DataStoreManager @Inject constructor(private val context: Context) {

    companion object {
        val BIOMETRIC_ENABLED_KEY  = booleanPreferencesKey("biometric_enabled")
        val IS_FIRST_TIME_USER_KEY = booleanPreferencesKey("is_first_time_user")
        val IS_DARK_MODE_KEY       = booleanPreferencesKey("is_dark_mode")
        val IS_ONBOARDED_KEY       = booleanPreferencesKey("is_onboarded")
        val CURRENCY_CODE_KEY      = androidx.datastore.preferences.core.stringPreferencesKey("currency_code")
        val LANGUAGE_CODE_KEY      = androidx.datastore.preferences.core.stringPreferencesKey("language_code")
        // ── Auth / Profile ────────────────────────────────────────────────────
        val USER_UID_KEY           = androidx.datastore.preferences.core.stringPreferencesKey("user_uid")
        val USER_EMAIL_KEY         = androidx.datastore.preferences.core.stringPreferencesKey("user_email")
        val USER_NAME_KEY          = androidx.datastore.preferences.core.stringPreferencesKey("user_name")
        val IS_LOGGED_IN_KEY       = booleanPreferencesKey("is_logged_in")
    }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_ENABLED_KEY] ?: false
    }

    val isFirstTimeUser: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_FIRST_TIME_USER_KEY] ?: true
    }

    val isOnboarded: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_ONBOARDED_KEY] ?: false
    }

    val isDarkModeEnabled: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[IS_DARK_MODE_KEY]
    }

    val currencyCode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CURRENCY_CODE_KEY] ?: "ETB"
    }

    val languageCode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_CODE_KEY] ?: "en"
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }

    suspend fun setFirstTimeUser(isFirstTime: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_TIME_USER_KEY] = isFirstTime
        }
    }

    suspend fun setOnboarded(onboarded: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ONBOARDED_KEY] = onboarded
        }
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE_KEY] = enabled
        }
    }

    suspend fun setCurrencyCode(code: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENCY_CODE_KEY] = code
        }
    }

    suspend fun setLanguageCode(code: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_CODE_KEY] = code
        }
    }

    // ── Auth / Profile ────────────────────────────────────────────────────────

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN_KEY] ?: false
    }

    val userUid: Flow<String> = context.dataStore.data.map { it[USER_UID_KEY] ?: "" }
    val userEmail: Flow<String> = context.dataStore.data.map { it[USER_EMAIL_KEY] ?: "" }
    val userName: Flow<String> = context.dataStore.data.map { it[USER_NAME_KEY] ?: "" }

    suspend fun saveUserProfile(uid: String, email: String, username: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_UID_KEY]     = uid
            prefs[USER_EMAIL_KEY]   = email
            prefs[USER_NAME_KEY]    = username
            prefs[IS_LOGGED_IN_KEY] = true
        }
    }

    suspend fun clearUserProfile() {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_UID_KEY)
            prefs.remove(USER_EMAIL_KEY)
            prefs.remove(USER_NAME_KEY)
            prefs[IS_LOGGED_IN_KEY] = false
        }
    }
}
