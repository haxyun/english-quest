package com.englishquest.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// Une seule instance de DataStore pour toute l'application.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "english_quest")

/** Sauvegarde locale de la progression (aucun serveur, aucun compte). */
class ProgressStore(context: Context) {
    private val appContext = context.applicationContext
    private val key = stringPreferencesKey("progress_json")

    suspend fun load(): Progress {
        val prefs = appContext.dataStore.data.first()
        val json = prefs[key] ?: return Progress()
        return try {
            ProgressJson.fromJson(json)
        } catch (e: Exception) {
            Progress()
        }
    }

    suspend fun save(p: Progress) {
        val json = ProgressJson.toJson(p)
        appContext.dataStore.edit { it[key] = json }
    }
}
