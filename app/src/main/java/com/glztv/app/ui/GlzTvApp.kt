package com.glztv.app.ui

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.glztv.app.TvScreen
import com.glztv.app.data.PreferencesRepository
import com.glztv.app.ui.theme.GlzTheme

@Composable
fun GlzTvApp(deepLinkChannelId: String?, networkPermissionRevision: Int) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PreferencesRepository.FILE_NAME, Context.MODE_PRIVATE)
    }
    var themeMode by remember {
        mutableStateOf(prefs.getString(THEME_MODE, "adaptive") ?: "adaptive")
    }
    GlzTheme(themeMode) {
        Surface(Modifier.fillMaxSize()) {
            TvScreen(themeMode, deepLinkChannelId, networkPermissionRevision) {
                themeMode = it
                prefs.edit().putString(THEME_MODE, it).apply()
            }
        }
    }
}

private const val THEME_MODE = "theme_mode"
