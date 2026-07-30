package com.glztv.app

import android.content.SharedPreferences
import android.os.Build
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object GlzHubManager {
    const val HUB_URL = "https://glzhub.glztech.com"
    const val INSTALLATION_ID = "hub_installation_id"
    const val DEVICE_TOKEN = "hub_device_token"
    const val PAIRING_CODE = "hub_pairing_code"
    const val CONFIG_VERSION = "hub_config_version"
    const val EXPERIENCE_VERSION = "hub_experience_version"
    const val VISIBLE_APPS = "hub_visible_apps"
    const val VISIBLE_APPS_MANAGED = "hub_visible_apps_managed"
    const val GUEST_EXPERIENCE = "hub_guest_experience"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    data class SyncResult(
        val changed: Boolean,
        val guestName: String?,
        val visibleApps: Set<String>,
        val commands: List<AppCommand> = emptyList()
    )

    data class AppCommand(
        val id: String,
        val packageName: String,
        val sourceType: String,
        val sourceUrl: String?
    )

    fun installationId(prefs: SharedPreferences): String {
        prefs.getString(INSTALLATION_ID, null)?.let { return it }
        return UUID.randomUUID().toString().also {
            prefs.edit().putString(INSTALLATION_ID, it).apply()
        }
    }

    fun pairingCode(prefs: SharedPreferences): String? =
        prefs.getString(PAIRING_CODE, null)

    fun isEnrolled(prefs: SharedPreferences): Boolean =
        !prefs.getString(DEVICE_TOKEN, null).isNullOrBlank()

    fun visibleApps(prefs: SharedPreferences): Set<String> =
        prefs.getStringSet(VISIBLE_APPS, emptySet()).orEmpty()

    fun beginEnrollment(
        prefs: SharedPreferences,
        client: OkHttpClient
    ): String {
        val payload = JSONObject()
            .put("installationId", installationId(prefs))
            .put("platform", "Android TV")
            .put("model", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
            .put("appVersion", BuildConfig.VERSION_NAME)
        val response = client.newCall(
            Request.Builder()
                .url("$HUB_URL/api/v1/enrollment")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
        ).execute()
        val text = response.body?.string().orEmpty()
        check(response.isSuccessful) {
            runCatching { JSONObject(text).optString("error") }.getOrNull()
                ?.takeIf(String::isNotBlank) ?: "GLZ Hub returned ${response.code}"
        }
        val result = JSONObject(text)
        val code = result.getString("pairingCode")
        prefs.edit()
            .putString(DEVICE_TOKEN, result.getString("deviceToken"))
            .putString(PAIRING_CODE, code)
            .apply()
        return code
    }

    fun sync(
        prefs: SharedPreferences,
        client: OkHttpClient
    ): SyncResult {
        val token = prefs.getString(DEVICE_TOKEN, null)
            ?: return SyncResult(false, null, visibleApps(prefs))
        val response = client.newCall(
            Request.Builder()
                .url("$HUB_URL/api/v1/devices/config")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
        ).execute()
        if (response.code == 401) {
            // A newly generated token is not recognized by /devices/config until
            // an administrator claims its enrollment. Keep pending credentials so
            // the code remains visible and adoption can finish.
            if (!prefs.getString(PAIRING_CODE, null).isNullOrBlank()) {
                return SyncResult(false, null, visibleApps(prefs))
            }
            prefs.edit()
                .remove(DEVICE_TOKEN)
                .remove(PAIRING_CODE)
                .remove(CONFIG_VERSION)
                .remove(EXPERIENCE_VERSION)
                .apply()
            return SyncResult(true, null, visibleApps(prefs))
        }
        val text = response.body?.string().orEmpty()
        check(response.isSuccessful) { "GLZ Hub returned ${response.code}" }
        val config = JSONObject(text)
        val version = config.optString("version", "0")
        val previousVersion = prefs.all[CONFIG_VERSION]?.toString()
        val experienceVersion = config.optString("experienceVersion", "default")
        val previousExperienceVersion = prefs.getString(EXPERIENCE_VERSION, null)
        val appPackages = config.optJSONArray("visibleApps").toPackageSet()
        if (version == previousVersion && experienceVersion == previousExperienceVersion) {
            return SyncResult(false, null, visibleApps(prefs), commands(prefs, client))
        }

        val editor = prefs.edit()
            .putString(CONFIG_VERSION, version)
            .putString(EXPERIENCE_VERSION, experienceVersion)
            .remove(PAIRING_CODE)
        config.stringOrNull("guestName")?.let { editor.putString("guest_name", it) }
        if (config.has("playlistUrl")) {
            if (config.isNull("playlistUrl")) editor.remove("playlist_url")
            else editor.putString("playlist_url", config.optString("playlistUrl"))
        }
        if (config.has("epgUrl")) {
            if (config.isNull("epgUrl")) editor.remove("epg_url")
            else editor.putString("epg_url", config.optString("epgUrl"))
        }
        config.stringOrNull("themeMode")?.let { editor.putString("theme_mode", it) }
        config.stringOrNull("weatherLocation")?.let { editor.putString("weather_location", it) }
        config.stringOrNull("startDestination")?.let { editor.putString("start_destination", it) }
        if (config.has("captionsEnabled")) editor.putBoolean("captions_enabled", config.optBoolean("captionsEnabled"))
        config.stringOrNull("captionsLanguage")?.let { editor.putString("captions_language", it) }
        if (config.has("autoStart")) editor.putBoolean("auto_start", config.optBoolean("autoStart"))
        if (config.has("resumeLastChannel")) {
            editor.putBoolean("resume_last_channel", config.optBoolean("resumeLastChannel", true))
        }
        config.optJSONObject("guestExperience")?.let {
            editor.putString(GUEST_EXPERIENCE, it.toString())
        }
        config.optJSONObject("requestHeaders")?.let { headers ->
            editor.putString(
                "request_headers",
                headers.keys().asSequence().joinToString("\n") { "$it: ${headers.optString(it)}" }
            )
        }
        editor.putStringSet(VISIBLE_APPS, appPackages)
            .putBoolean(VISIBLE_APPS_MANAGED, true)
            .apply()
        return SyncResult(true, config.stringOrNull("guestName"), appPackages, commands(prefs, client))
    }

    fun commands(prefs: SharedPreferences, client: OkHttpClient): List<AppCommand> {
        val token = prefs.getString(DEVICE_TOKEN, null) ?: return emptyList()
        val response = client.newCall(Request.Builder()
            .url("$HUB_URL/api/v1/devices/commands")
            .header("Authorization", "Bearer $token").get().build()).execute()
        val text = response.body?.string().orEmpty()
        if (!response.isSuccessful) return emptyList()
        val items = JSONObject(text).optJSONArray("commands") ?: return emptyList()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val payload = item.optJSONObject("payload") ?: continue
                val id = item.optString("id")
                val packageName = payload.optString("packageName")
                if (id.isNotBlank() && packageName.isNotBlank()) add(AppCommand(
                    id, packageName, payload.optString("sourceType", "play_store"),
                    payload.stringOrNull("sourceUrl")
                ))
            }
        }
    }

    fun completeCommand(
        prefs: SharedPreferences,
        client: OkHttpClient,
        commandId: String,
        completed: Boolean,
        message: String
    ) {
        val token = prefs.getString(DEVICE_TOKEN, null) ?: return
        val payload = JSONObject().put("status", if (completed) "completed" else "failed").put("message", message)
        client.newCall(Request.Builder()
            .url("$HUB_URL/api/v1/devices/commands/$commandId/result")
            .header("Authorization", "Bearer $token")
            .post(payload.toString().toRequestBody(jsonMediaType)).build()).execute().close()
    }

    fun heartbeat(prefs: SharedPreferences, client: OkHttpClient) {
        val token = prefs.getString(DEVICE_TOKEN, null) ?: return
        val payload = JSONObject().put("appVersion", BuildConfig.VERSION_NAME)
        client.newCall(
            Request.Builder()
                .url("$HUB_URL/api/v1/devices/heartbeat")
                .header("Authorization", "Bearer $token")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
        ).execute().close()
    }

    private fun JSONObject.stringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    private fun JSONArray?.toPackageSet(): Set<String> {
        if (this == null) return emptySet()
        return buildSet {
            for (index in 0 until length()) {
                when (val value = opt(index)) {
                    is String -> value.takeIf(String::isNotBlank)?.let(::add)
                    is JSONObject -> value.optString("packageName").takeIf(String::isNotBlank)?.let(::add)
                }
            }
        }
    }
}
