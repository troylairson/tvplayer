package com.tvplayer.launcher.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tvplayer.launcher.domain.normalizeBaseUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore("launcher_settings")

/** Tiles that open another app; which app is chosen in launcher settings. */
enum class AppTile(val label: String, val defaultPackage: String?) {
    LIVE_TV("Live TV", "com.iptvplayer.app"),
    NOSTALGIA("NostalgiaTV", null),
    MOVIES("Movies", null),
    MUSIC("Music", null),
}

data class HomeAssistantConfig(
    val baseUrl: String,
    val token: String,
) {
    val isComplete: Boolean get() = baseUrl.isNotBlank() && token.isNotBlank()
    val bearer: String get() = "Bearer $token"
    fun url(path: String): String = baseUrl.trimEnd('/') + path
}

/** Home Assistant entity ids behind the Spa tile/screen and the header weather. */
data class EntityConfig(
    val spaClimate: String = "climate.spa",
    val spaBubbles: String = "climate.spa_bubbles",
    val weather: String = "weather.forecast_home",
)

data class LauncherConfig(
    val homeAssistant: HomeAssistantConfig,
    val entities: EntityConfig,
    val tileApps: Map<AppTile, String?>,
)

@Singleton
class LauncherSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val store = context.launcherDataStore

    private val KEY_HA_URL   = stringPreferencesKey("ha_url")
    private val KEY_HA_TOKEN = stringPreferencesKey("ha_token")
    // v2 keys hold only values that differ from EntityConfig's defaults, so changing a default
    // in a new build reaches devices that never customised it. v1 stored every field.
    private val KEY_SPA_CLIMATE = stringPreferencesKey("entity_v2_spa_climate")
    private val KEY_SPA_BUBBLES = stringPreferencesKey("entity_v2_spa_bubbles")
    private val KEY_WEATHER     = stringPreferencesKey("entity_v2_weather")
    private val LEGACY_ENTITY_KEYS = listOf(
        "entity_spa_climate", "entity_spa_jets_1", "entity_spa_jets_2",
        "entity_spa_lights", "entity_spa_waterfall", "entity_weather",
    ).map { stringPreferencesKey(it) }

    private fun tileKey(tile: AppTile) = stringPreferencesKey("tile_app_${tile.name.lowercase()}")

    val config: Flow<LauncherConfig> = store.data.map { prefs ->
        val defaults = EntityConfig()
        LauncherConfig(
            homeAssistant = HomeAssistantConfig(
                baseUrl = prefs[KEY_HA_URL] ?: "",
                token = prefs[KEY_HA_TOKEN] ?: "",
            ),
            entities = EntityConfig(
                spaClimate = prefs[KEY_SPA_CLIMATE] ?: defaults.spaClimate,
                spaBubbles = prefs[KEY_SPA_BUBBLES] ?: defaults.spaBubbles,
                weather    = prefs[KEY_WEATHER] ?: defaults.weather,
            ),
            // An empty string means "explicitly none"; missing means "use the default"
            tileApps = AppTile.entries.associateWith { tile ->
                (prefs[tileKey(tile)] ?: tile.defaultPackage)?.takeIf { it.isNotBlank() }
            },
        )
    }

    suspend fun saveHomeAssistant(baseUrl: String, token: String, entities: EntityConfig) {
        store.edit { prefs ->
            prefs[KEY_HA_URL]   = normalizeBaseUrl(baseUrl)
            prefs[KEY_HA_TOKEN] = token.trim()
            val defaults = EntityConfig()
            prefs.putUnlessDefault(KEY_SPA_CLIMATE, entities.spaClimate, defaults.spaClimate)
            prefs.putUnlessDefault(KEY_SPA_BUBBLES, entities.spaBubbles, defaults.spaBubbles)
            prefs.putUnlessDefault(KEY_WEATHER, entities.weather, defaults.weather)
            LEGACY_ENTITY_KEYS.forEach { prefs.remove(it) }
        }
    }

    /** Blank or default → remove the key, so the built-in default applies. */
    private fun MutablePreferences.putUnlessDefault(key: Preferences.Key<String>, value: String, default: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed == default) remove(key) else this[key] = trimmed
    }

    suspend fun setTileApp(tile: AppTile, packageName: String?) {
        store.edit { it[tileKey(tile)] = packageName ?: "" }
    }
}
