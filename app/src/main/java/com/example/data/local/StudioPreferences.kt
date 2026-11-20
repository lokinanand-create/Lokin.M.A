package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.localization.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore: DataStore<Preferences> by preferencesDataStore(name = "ma_ai_studio_prefs")

data class StudioSettingsSnapshot(
    val darkMode: Boolean = true,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val signedInEmail: String? = null,
    val openAiBaseUrl: String = "https://api.openai.com/v1/",
    val openAiModel: String = "gpt-4o-mini",
    val watermarkEnabled: Boolean = true,
    val privacyAnalyticsDisabled: Boolean = true
)

class StudioPreferences(private val context: Context) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val LANGUAGE = stringPreferencesKey("language_code")
        val SIGNED_IN_EMAIL = stringPreferencesKey("signed_in_email")
        val OPENAI_BASE_URL = stringPreferencesKey("openai_base_url")
        val OPENAI_MODEL = stringPreferencesKey("openai_model")
        val WATERMARK_ENABLED = booleanPreferencesKey("watermark_enabled")
        val PRIVACY_MODE = booleanPreferencesKey("privacy_mode")
    }

    val settingsFlow: Flow<StudioSettingsSnapshot> = context.studioDataStore.data.map { prefs ->
        StudioSettingsSnapshot(
            darkMode = prefs[Keys.DARK_MODE] ?: true,
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: "en"),
            signedInEmail = prefs[Keys.SIGNED_IN_EMAIL],
            openAiBaseUrl = prefs[Keys.OPENAI_BASE_URL] ?: "https://api.openai.com/v1/",
            openAiModel = prefs[Keys.OPENAI_MODEL] ?: "gpt-4o-mini",
            watermarkEnabled = prefs[Keys.WATERMARK_ENABLED] ?: true,
            privacyAnalyticsDisabled = prefs[Keys.PRIVACY_MODE] ?: true
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.studioDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setSignedInEmail(email: String?) {
        context.studioDataStore.edit { prefs ->
            if (email == null) prefs.remove(Keys.SIGNED_IN_EMAIL)
            else prefs[Keys.SIGNED_IN_EMAIL] = email
        }
    }

    suspend fun setOpenAiEndpoint(baseUrl: String, model: String) {
        context.studioDataStore.edit {
            it[Keys.OPENAI_BASE_URL] = baseUrl.trim().ifBlank { "https://api.openai.com/v1/" }
            it[Keys.OPENAI_MODEL] = model.trim().ifBlank { "gpt-4o-mini" }
        }
    }

    suspend fun setWatermarkEnabled(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.WATERMARK_ENABLED] = enabled }
    }

    suspend fun setPrivacyMode(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.PRIVACY_MODE] = enabled }
    }
}
