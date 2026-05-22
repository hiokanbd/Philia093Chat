package com.philia093.chat.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ChatClient {

    private const val BASE_URL = "http://127.0.0.1:8010"

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    data class ChatResult(
        val reply: String,
        val error: String? = null
    )

    suspend fun send(message: String, apiKey: String = ""): ChatResult = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("message", message)
                put("user_id", "android")
                if (apiKey.isNotBlank()) {
                    put("deepseek_key", apiKey)
                }
            }

            val request = Request.Builder()
                .url("$BASE_URL/api/chat")
                .post(body.toString().toRequestBody(JSON))
                .build()

            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")

            if (json.has("error")) {
                ChatResult("", json.getString("error"))
            } else {
                ChatResult(json.optString("reply", ""))
            }
        } catch (e: java.net.ConnectException) {
            ChatResult("", "AI 还没醒来呢……\n请先在 Termux 里运行 bash ~/xilian.sh 启动服务哦 ♪")
        } catch (e: Exception) {
            ChatResult("", "连接出错了：${e.message}")
        }
    }

    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/health")
                .get()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    data class CharacterInfo(
        val name: String,
        val displayName: String,
        val description: String,
        val avatarLetter: String
    )

    data class CharacterListResult(
        val characters: List<CharacterInfo>,
        val active: String?
    )

    suspend fun getCharacters(): CharacterListResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/characters")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")
            val arr = json.optJSONArray("characters") ?: org.json.JSONArray()
            val chars = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                CharacterInfo(
                    name = obj.optString("name", ""),
                    displayName = obj.optString("display_name", ""),
                    description = obj.optString("description", ""),
                    avatarLetter = obj.optString("avatar_letter", "?")
                )
            }
            CharacterListResult(chars, json.optString("active", null))
        } catch (_: Exception) {
            CharacterListResult(emptyList(), null)
        }
    }

    suspend fun switchCharacter(name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply { put("name", name) }
            val request = Request.Builder()
                .url("$BASE_URL/api/characters/switch")
                .post(body.toString().toRequestBody(JSON))
                .build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")
            json.optString("status") == "ok"
        } catch (_: Exception) {
            false
        }
    }
}
