package com.glztv.app.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.glztv.app.GlzHubManager
import com.glztv.app.TvScreen
import com.glztv.app.data.PreferencesRepository
import com.glztv.app.ui.i18n.GlzStrings
import com.glztv.app.ui.i18n.LocalGlzStrings
import com.glztv.app.ui.theme.AmbientBackground
import com.glztv.app.ui.theme.GlzTheme

@Composable
fun GlzTvApp(deepLinkChannelId: String?, networkPermissionRevision: Int) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PreferencesRepository.FILE_NAME, Context.MODE_PRIVATE)
    }
    var themeMode by remember {
        mutableStateOf(prefs.getString(THEME_MODE, "dark") ?: "dark")
    }
    var appLanguage by remember {
        mutableStateOf(prefs.getString(GlzHubManager.APP_LANGUAGE, "en") ?: "en")
    }
    val strings = remember(appLanguage) { GlzStrings.get(appLanguage) }

    CompositionLocalProvider(LocalGlzStrings provides strings) {
        GlzTheme(themeMode) {
            AmbientBackground {
                TvScreen(
                    themeMode = themeMode,
                    deepLinkChannelId = deepLinkChannelId,
                    networkPermissionRevision = networkPermissionRevision,
                    onThemeMode = {
                        themeMode = it
                        prefs.edit().putString(THEME_MODE, it).apply()
                    },
                    onLanguageChanged = {
                        appLanguage = it
                        prefs.edit().putString(GlzHubManager.APP_LANGUAGE, it).apply()
                    }
                )
            }
        }
    }
}

private const val THEME_MODE = "theme_mode"
