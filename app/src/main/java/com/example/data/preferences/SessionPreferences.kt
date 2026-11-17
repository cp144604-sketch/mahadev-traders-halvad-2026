package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mahadev_session_prefs")

data class LocalSessionPrefs(
    val rememberAdmin: Boolean = false,
    val savedAdminMobile: String = "",
    val adminLabel: String = "",
    val lastCustomerMobile: String = "",
    val lastCustomerName: String = ""
)

class SessionPreferences(private val context: Context) {
    private object Keys {
        val REMEMBER_ADMIN = booleanPreferencesKey("remember_admin")
        val SAVED_ADMIN_MOBILE = stringPreferencesKey("saved_admin_mobile")
        val ADMIN_LABEL = stringPreferencesKey("admin_label")
        val LAST_CUSTOMER_MOBILE = stringPreferencesKey("last_customer_mobile")
        val LAST_CUSTOMER_NAME = stringPreferencesKey("last_customer_name")
    }

    val prefsFlow: Flow<LocalSessionPrefs> = context.dataStore.data.map { prefs ->
        LocalSessionPrefs(
            rememberAdmin = prefs[Keys.REMEMBER_ADMIN] ?: false,
            savedAdminMobile = prefs[Keys.SAVED_ADMIN_MOBILE] ?: "",
            adminLabel = prefs[Keys.ADMIN_LABEL] ?: "",
            lastCustomerMobile = prefs[Keys.LAST_CUSTOMER_MOBILE] ?: "",
            lastCustomerName = prefs[Keys.LAST_CUSTOMER_NAME] ?: ""
        )
    }

    suspend fun saveCustomerSession(mobileNumber: String, displayName: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_CUSTOMER_MOBILE] = mobileNumber
            prefs[Keys.LAST_CUSTOMER_NAME] = displayName
        }
    }

    suspend fun saveAdminSession(remember: Boolean, mobileNumber: String, adminLabel: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REMEMBER_ADMIN] = remember
            if (remember) {
                prefs[Keys.SAVED_ADMIN_MOBILE] = mobileNumber
                prefs[Keys.ADMIN_LABEL] = adminLabel
            } else {
                prefs.remove(Keys.SAVED_ADMIN_MOBILE)
                prefs.remove(Keys.ADMIN_LABEL)
            }
        }
    }

    suspend fun clearAdminSession() {
        context.dataStore.edit { prefs ->
            prefs[Keys.REMEMBER_ADMIN] = false
            prefs.remove(Keys.SAVED_ADMIN_MOBILE)
            prefs.remove(Keys.ADMIN_LABEL)
        }
    }

    suspend fun clearAllOnSignOut() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
