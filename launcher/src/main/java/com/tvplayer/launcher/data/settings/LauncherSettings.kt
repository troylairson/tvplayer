package com.tvplayer.launcher.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
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
    val spaJets1: String = "switch.spa_jets_1",
    val spaJets2: String = "switch.spa_jets_2",
    val spaLights: String = "light.spa",
    val spaWaterfall: String = "switch.spa_waterfall",
    val weather: String = "weather.home",
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
    private val KEY_SPA_CLIMATE   = stringPreferencesKey("entity_spa_climate")
    private val KEY_SPA_JETS_1    = stringPreferencesKey("entity_spa_jets_1")
    private val KEY_SPA_JETS_2    = stringPreferencesKey("entity_spa_jets_2")
    private val KEY_SPA_LIGHTS    = stringPreferencesKey("entity_spa_lights")
    private val KEY_SPA_WATERFALL = stringPreferencesKey("entity_spa_waterfall")
    private val KEY_WEATHER       = stringPreferencesKey("entity_weather")

    private fun tileKey(tile: AppTile) = stringPreferencesKey("tile_app_${tile.name.lowercase()}")

    val config: Flow<LauncherConfig> = store.data.map { prefs ->
        val defaults = EntityConfig()
        LauncherConfig(
            homeAssistant = HomeAssistantConfig(
                baseUrl = prefs[KEY_HA_URL] ?: "",
                token = prefs[KEY_HA_TOKEN] ?: "",
            ),
            entities = EntityConfig(
                spaClimate   = prefs[KEY_SPA_CLIMATE] ?: defaults.spaClimate,
                spaJets1     = prefs[KEY_SPA_JETS_1] ?: defaults.spaJets1,
                spaJets2     = prefs[KEY_SPA_JETS_2] ?: defaults.spaJets2,
                spaLights    = prefs[KEY_SPA_LIGHTS] ?: defaults.spaLights,
                spaWaterfall = prefs[KEY_SPA_WATERFALL] ?: defaults.spaWaterfall,
                weather      = prefs[KEY_WEATHER] ?: defaults.weather,
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
            prefs[KEY_SPA_CLIMATE]   = entities.spaClimate.trim()
            prefs[KEY_SPA_JETS_1]    = entities.spaJets1.trim()
            prefs[KEY_SPA_JETS_2]    = entities.spaJets2.trim()
            prefs[KEY_SPA_LIGHTS]    = entities.spaLights.trim()
            prefs[KEY_SPA_WATERFALL] = entities.spaWaterfall.trim()
            prefs[KEY_WEATHER]       = entities.weather.trim()
        }
    }

    suspend fun setTileApp(tile: AppTile, packageName: String?) {
        store.edit { it[tileKey(tile)] = packageName ?: "" }
    }
}
