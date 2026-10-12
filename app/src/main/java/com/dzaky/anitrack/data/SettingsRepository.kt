package com.dzaky.anitrack.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode(val label: String) {
    System("System default"), Light("Light"), Dark("Dark"), Black("Pure black"),
}

@Serializable
enum class LibraryLayout(val label: String) { List("List"), Grid("Cover grid") }

@Serializable
enum class CatalogSource(val label: String) {
    Automatic("Automatic"), Jikan("Jikan / MyAnimeList"), AniList("AniList"),
}

@Serializable
data class AppPreferences(
    val theme: ThemeMode = ThemeMode.System,
    val incognito: Boolean = false,
    val layout: LibraryLayout = LibraryLayout.List,
    val source: CatalogSource = CatalogSource.Automatic,
)

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(private val store: DataStore<Preferences>) {
    private val themeKey = stringPreferencesKey("theme")
    private val incognitoKey = booleanPreferencesKey("incognito")
    private val layoutKey = stringPreferencesKey("library_layout")
    private val sourceKey = stringPreferencesKey("catalog_source")

    val preferences = store.data.catch { error ->
        if (error is IOException) emit(mutablePreferencesOf(incognitoKey to true)) else throw error
    }.map { p ->
        AppPreferences(
            ThemeMode.entries.firstOrNull { it.name == p[themeKey] } ?: ThemeMode.System,
            p[incognitoKey] ?: false,
            LibraryLayout.entries.firstOrNull { it.name == p[layoutKey] } ?: LibraryLayout.List,
            CatalogSource.entries.firstOrNull { it.name == p[sourceKey] } ?: CatalogSource.Automatic,
        )
    }
    val theme = preferences.map { it.theme }

    // A failed preference read must not record history while privacy state is unknown.
    suspend fun historyAllowed() = store.data.first()[incognitoKey] != true
    suspend fun setTheme(mode: ThemeMode) { store.edit { it[themeKey] = mode.name } }
    suspend fun setIncognito(enabled: Boolean) { store.edit { it[incognitoKey] = enabled } }
    suspend fun setLayout(layout: LibraryLayout) { store.edit { it[layoutKey] = layout.name } }
    suspend fun setSource(source: CatalogSource) { store.edit { it[sourceKey] = source.name } }

    suspend fun restore(settings: AppPreferences) {
        store.edit {
            it[themeKey] = settings.theme.name
            it[incognitoKey] = settings.incognito
            it[layoutKey] = settings.layout.name
            it[sourceKey] = settings.source.name
        }
    }
}
