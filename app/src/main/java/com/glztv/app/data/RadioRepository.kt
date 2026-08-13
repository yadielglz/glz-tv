package com.glztv.app.data

import com.glztv.app.RadioCatalogManager
import com.glztv.app.RadioStation
import okhttp3.OkHttpClient

class RadioRepository(
    private val preferences: PreferencesRepository,
    private val client: OkHttpClient
) {
    fun load(): RadioCatalogManager.Result =
        RadioCatalogManager.load(preferences.sharedPreferences, client)
}
