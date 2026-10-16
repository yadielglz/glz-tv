package com.glztv.app.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * Exposes SharedPreferences data securely to apps signed with the same developer certificate
 * to allow seamless data and sync migration between package names (com.glztv.app <-> glz.tv.app).
 */
class MigrationProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val ctx = context ?: return null
        val prefs = ctx.getSharedPreferences(PreferencesRepository.FILE_NAME, Context.MODE_PRIVATE)
        val allEntries = prefs.all
        val json = JSONObject()
        allEntries.forEach { (key, value) ->
            if (value is Set<*>) {
                val arr = JSONArray()
                value.forEach { arr.put(it) }
                json.put(key, arr)
            } else {
                json.put(key, value)
            }
        }

        val cursor = MatrixCursor(arrayOf("data"))
        cursor.addRow(arrayOf(json.toString()))
        return cursor
    }

    override fun getType(uri: Uri): String = "application/json"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
