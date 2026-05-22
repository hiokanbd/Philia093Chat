package com.philia093.chat.settings

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "xilian_settings")

object AppSettings {

    // Keys
    private val KEY_API_KEY = stringPreferencesKey("api_key")
    private val KEY_API_BASE = stringPreferencesKey("api_base")
    private val KEY_BG_URI = stringPreferencesKey("bg_uri")
    private val KEY_BG_BRIGHTNESS = floatPreferencesKey("bg_brightness")
    private val KEY_BUBBLE_ALPHA = floatPreferencesKey("bubble_alpha")
    private val KEY_FONT_URL = stringPreferencesKey("font_url")
    private val KEY_FONT_NAME = stringPreferencesKey("font_name")
    private val KEY_AVATAR_URI = stringPreferencesKey("avatar_uri")

    // Defaults
    const val DEFAULT_API_BASE = "https://api.deepseek.com"
    const val DEFAULT_BG_BRIGHTNESS = 0.6f
    const val DEFAULT_BUBBLE_ALPHA = 0.85f

    // ── API Key ──
    fun apiKeyFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_API_KEY] ?: "" }

    suspend fun setApiKey(context: Context, key: String) {
        context.dataStore.edit { it[KEY_API_KEY] = key }
    }

    // ── API Base URL (DeepSeek official only) ──
    fun apiBaseFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_API_BASE] ?: DEFAULT_API_BASE }

    suspend fun setApiBase(context: Context, url: String) {
        val clean = url.trim().trimEnd('/')
        if (clean == "https://api.deepseek.com" || clean == "https://api.deepseek.com/v1") {
            context.dataStore.edit { it[KEY_API_BASE] = clean }
        }
    }

    // ── Background Image ──
    fun bgUriFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_BG_URI] ?: "" }

    suspend fun setBgUri(context: Context, uri: String) {
        context.dataStore.edit { it[KEY_BG_URI] = uri }
    }

    // ── Background Brightness (0.1 - 1.0) ──
    fun bgBrightnessFlow(context: Context): Flow<Float> =
        context.dataStore.data.map { it[KEY_BG_BRIGHTNESS] ?: DEFAULT_BG_BRIGHTNESS }

    suspend fun setBgBrightness(context: Context, value: Float) {
        context.dataStore.edit { it[KEY_BG_BRIGHTNESS] = value.coerceIn(0.1f, 1.0f) }
    }

    // ── Bubble Transparency (0.2 - 1.0) ──
    fun bubbleAlphaFlow(context: Context): Flow<Float> =
        context.dataStore.data.map { it[KEY_BUBBLE_ALPHA] ?: DEFAULT_BUBBLE_ALPHA }

    suspend fun setBubbleAlpha(context: Context, value: Float) {
        context.dataStore.edit { it[KEY_BUBBLE_ALPHA] = value.coerceIn(0.2f, 1.0f) }
    }

    // ── Font ──
    fun fontUrlFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_FONT_URL] ?: "" }

    fun fontNameFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_FONT_NAME] ?: "默认" }

    suspend fun setFont(context: Context, name: String, url: String) {
        context.dataStore.edit {
            it[KEY_FONT_NAME] = name
            it[KEY_FONT_URL] = url
        }
    }

    // ── Avatar (in-app displayed icon) ──
    fun avatarUriFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_AVATAR_URI] ?: "" }

    suspend fun setAvatarUri(context: Context, uri: String) {
        context.dataStore.edit { it[KEY_AVATAR_URI] = uri }
    }
}
