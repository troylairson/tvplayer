package com.iptvplayer.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.credentialsDataStore: DataStore<Preferences> by preferencesDataStore("credentials")

data class XtreamCredentials(
    val serverUrl: String,
    val username: String,
    val password: String,
) {
    val isComplete get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    fun playerApiUrl() = "${serverUrl.trimEnd('/')}/player_api.php"
    fun streamUrl(streamId: Int, extension: String = "m3u8") =
        "${serverUrl.trimEnd('/')}/live/$username/$password/$streamId.$extension"
}

@Singleton
class CredentialsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.credentialsDataStore

    private val KEY_SERVER = stringPreferencesKey("server_url")
    private val KEY_USER   = stringPreferencesKey("username")
    private val KEY_PASS   = stringPreferencesKey("password")

    val credentials: Flow<XtreamCredentials?> = store.data.map { prefs ->
        val url  = prefs[KEY_SERVER] ?: return@map null
        val user = prefs[KEY_USER]   ?: return@map null
        val pass = prefs[KEY_PASS]   ?: return@map null
        if (url.isBlank() || user.isBlank() || pass.isBlank()) null
        else XtreamCredentials(url, user, pass)
    }

    suspend fun save(serverUrl: String, username: String, password: String) {
        store.edit { prefs ->
            prefs[KEY_SERVER] = serverUrl.trimEnd('/')
            prefs[KEY_USER]   = username
            prefs[KEY_PASS]   = password
        }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }
}
