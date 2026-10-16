package com.glztv.app.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles automatic data and sync migration from legacy packages (e.g. com.glztv.app)
 * into glz.tv.app, preserving GLZ Hub device tokens, pairing, preferences, and playlists.
 */
object DataMigrationManager {
    private const val TAG = "DataMigration"
    const val LEGACY_PACKAGE = "com.glztv.app"
    const val KEY_MIGRATION_COMPLETED = "migration_from_legacy_completed"

    data class MigrationResult(
        val migrated: Boolean,
        val keysImported: Int = 0,
        val legacyPackageFound: Boolean = false,
        val error: String? = null
    )

    fun isLegacyAppInstalled(context: Context): Boolean {
        return runCatching {
            context.packageManager.getPackageInfo(LEGACY_PACKAGE, 0)
            true
        }.getOrDefault(false)
    }

    fun isMigrationNeeded(prefs: SharedPreferences, context: Context): Boolean {
        if (prefs.getBoolean(KEY_MIGRATION_COMPLETED, false)) return false
        // If device token already exists, migration is not required
        if (!prefs.getString("hub_device_token", null).isNullOrBlank()) return false
        return isLegacyAppInstalled(context)
    }

    fun performMigration(context: Context, prefs: SharedPreferences): MigrationResult {
        if (!isLegacyAppInstalled(context)) {
            return MigrationResult(migrated = false, legacyPackageFound = false)
        }

        val uri = Uri.parse("content://$LEGACY_PACKAGE.migration/prefs")
        return runCatching {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor == null || !cursor.moveToFirst()) {
                cursor?.close()
                return MigrationResult(
                    migrated = false,
                    legacyPackageFound = true,
                    error = "Could not query legacy MigrationProvider from $LEGACY_PACKAGE"
                )
            }

            val jsonString = cursor.getString(0)
            cursor.close()
            val json = JSONObject(jsonString)
            val editor = prefs.edit()
            var count = 0

            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key == KEY_MIGRATION_COMPLETED) continue
                when (val value = json.get(key)) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is String -> editor.putString(key, value)
                    is JSONArray -> {
                        val set = buildSet {
                            for (i in 0 until value.length()) add(value.getString(i))
                        }
                        editor.putStringSet(key, set)
                    }
                }
                count++
            }

            editor.putBoolean(KEY_MIGRATION_COMPLETED, true)
            editor.apply()
            Log.i(TAG, "Successfully migrated $count preference keys from $LEGACY_PACKAGE")
            MigrationResult(migrated = true, keysImported = count, legacyPackageFound = true)
        }.getOrElse { e ->
            Log.w(TAG, "Migration from $LEGACY_PACKAGE failed or provider not ready", e)
            MigrationResult(migrated = false, legacyPackageFound = true, error = e.message)
        }
    }
}
