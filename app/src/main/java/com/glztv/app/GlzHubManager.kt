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
    private const val ACTIVITY_TYPE = "hub_activity_type"
    private const val ACTIVITY_LABEL = "hub_activity_label"
    private const val ACTIVITY_PACKAGE = "hub_activity_package"
    private const val PREVIOUS_ACTIVITY_TYPE = "hub_previous_activity_type"
    private const val PREVIOUS_ACTIVITY_LABEL = "hub_previous_activity_label"
    private const val PREVIOUS_ACTIVITY_PACKAGE = "hub_previous_activity_package"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    data class SyncResult(
        val changed: Boolean,
        val guestName: String?,
        val visibleApps: Set<String>,
        val commands: List<HubCommand> = emptyList(),
        val forceRefreshTriggered: Boolean = false
    )

    sealed interface HubCommand {
        val id: String
    }

    data class AppCommand(
        override val id: String,
        val packageName: String,
        val sourceType: String,
        val sourceUrl: String?
    ) : HubCommand

    data class ForceRefreshCommand(
        override val id: String
    ) : HubCommand

    fun installationId(prefs: SharedPreferences): String {
        prefs.getString(INSTALLATION_ID, null)?.let { return it }
        return UUID.randomUUID().toString().also {
            prefs.edit().putString(INSTALLATION_ID, it).apply()
        }
    }

    fun pairingCode(prefs: SharedPreferences): String? =
        prefs.getString(PAIRING_CODE, null)

    fun isEnrolled(prefs: SharedPreferences): Boolean =
        !prefs.getString(DEVICE_TOKEN, null).isNullOrBlank() &&
            prefs.getString(PAIRING_CODE, null).isNullOrBlank()

    fun sourceRequestHeaders(
        prefs: SharedPreferences,
        url: String,
        headers: Map<String, String>
    ): Map<String, String> {
        if (url != "$HUB_URL/api/v1/devices/playlist.m3u") return headers
        val token = prefs.getString(DEVICE_TOKEN, null)?.takeIf(String::isNotBlank) ?: return headers
        return headers + ("Authorization" to "Bearer $token")
    }

    fun visibleApps(prefs: SharedPreferences): Set<String> =
        prefs.getStringSet(VISIBLE_APPS, emptySet()).orEmpty()

    fun reportActivity(
        prefs: SharedPreferences,
        type: String,
        label: String? = null,
        packageName: String? = null
    ) {
        prefs.edit()
            .putString(ACTIVITY_TYPE, type)
            .putString(ACTIVITY_LABEL, label?.take(160))
            .putString(ACTIVITY_PACKAGE, packageName?.take(180))
            .apply()
    }

    fun reportLaunchedApp(prefs: SharedPreferences, label: String, packageName: String) {
        prefs.edit()
            .putString(PREVIOUS_ACTIVITY_TYPE, prefs.getString(ACTIVITY_TYPE, "idle"))
            .putString(PREVIOUS_ACTIVITY_LABEL, prefs.getString(ACTIVITY_LABEL, null))
            .putString(PREVIOUS_ACTIVITY_PACKAGE, prefs.getString(ACTIVITY_PACKAGE, null))
            .putString(ACTIVITY_TYPE, "app")
            .putString(ACTIVITY_LABEL, label.take(160))
            .putString(ACTIVITY_PACKAGE, packageName.take(180))
            .apply()
    }

    fun restoreActivityAfterApp(prefs: SharedPreferences): Boolean {
        if (prefs.getString(ACTIVITY_TYPE, null) != "app") return false
        prefs.edit()
            .putString(ACTIVITY_TYPE, prefs.getString(PREVIOUS_ACTIVITY_TYPE, "idle"))
            .putString(ACTIVITY_LABEL, prefs.getString(PREVIOUS_ACTIVITY_LABEL, null))
            .putString(ACTIVITY_PACKAGE, prefs.getString(PREVIOUS_ACTIVITY_PACKAGE, null))
            .remove(PREVIOUS_ACTIVITY_TYPE)
            .remove(PREVIOUS_ACTIVITY_LABEL)
            .remove(PREVIOUS_ACTIVITY_PACKAGE)
            .apply()
        return true
    }

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
        if (config.has("osdTimeoutSeconds")) {
            editor.putInt("osd_timeout_seconds", config.optInt("osdTimeoutSeconds", 8))
        }
        if (config.has("autoUpdate")) {
            editor.putBoolean("auto_update", config.optBoolean("autoUpdate", true))
        }
        if (config.has("wifiOnly")) {
            editor.putBoolean("wifi_only", config.optBoolean("wifiOnly", false))
        }
        val forceRefreshToken = config.stringOrNull("forceRefreshToken")
        val previousForceRefreshToken = prefs.getString("last_force_refresh_token", null)
        var forceRefreshTriggered = false
        if (forceRefreshToken != null && forceRefreshToken != previousForceRefreshToken) {
            editor.putString("last_force_refresh_token", forceRefreshToken)
            forceRefreshTriggered = true
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
        return SyncResult(
            changed = true,
            guestName = config.stringOrNull("guestName"),
            visibleApps = appPackages,
            commands = commands(prefs, client),
            forceRefreshTriggered = forceRefreshTriggered
        )
    }

    fun commands(prefs: SharedPreferences, client: OkHttpClient): List<HubCommand> {
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
                val id = item.optString("id")
                val action = item.optString("action")
                if (id.isBlank()) continue
                if (action == "force_refresh") {
                    add(ForceRefreshCommand(id))
                } else if (action == "install_app") {
                    val payload = item.optJSONObject("payload") ?: continue
                    val packageName = payload.optString("packageName")
                    if (packageName.isNotBlank()) add(AppCommand(
                        id, packageName, payload.optString("sourceType", "play_store"),
                        payload.stringOrNull("sourceUrl")
                    ))
                }
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
        if (!prefs.getString(PAIRING_CODE, null).isNullOrBlank()) return
        val token = prefs.getString(DEVICE_TOKEN, null) ?: return
        val payload = JSONObject()
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("activity", JSONObject()
                .put("type", prefs.getString(ACTIVITY_TYPE, "idle"))
                .put("label", prefs.getString(ACTIVITY_LABEL, null))
                .put("packageName", prefs.getString(ACTIVITY_PACKAGE, null))
            )
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
