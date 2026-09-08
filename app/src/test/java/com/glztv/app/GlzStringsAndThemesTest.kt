package com.glztv.app

import com.glztv.app.ui.i18n.GlzStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlzStringsAndThemesTest {

    @Test
    fun resolvesEnglishStringsByDefaultAndFallback() {
        val defaultStrings = GlzStrings.get("en")
        assertEquals("en", defaultStrings.languageCode)
        assertEquals("Home", defaultStrings.navHome)
        assertEquals("Guide", defaultStrings.navGuide)
        assertEquals("Settings", defaultStrings.settings)
        assertEquals("Good Morning", defaultStrings.goodMorning)
        assertEquals("Press any button on remote to wake up", defaultStrings.screensaverPrompt)
        assertEquals("Color Theme", defaultStrings.theme)
        assertEquals("App Language", defaultStrings.appLanguage)

        val fallbackStrings = GlzStrings.get(null)
        assertEquals("en", fallbackStrings.languageCode)

        val unknownStrings = GlzStrings.get("fr")
        assertEquals("en", unknownStrings.languageCode)
    }

    @Test
    fun resolvesSpanishStringsForSpanishCodes() {
        val spanishCodes = listOf("es", "ES", "es-ES", "es-MX", "es-PR", "es-US", "spanish", "SPANISH")
        for (code in spanishCodes) {
            val s = GlzStrings.get(code)
            assertEquals("Code $code should resolve to Spanish", "es", s.languageCode)
            assertEquals("Inicio", s.navHome)
            assertEquals("Guía", s.navGuide)
            assertEquals("Radio", s.navRadio)
            assertEquals("Clima", s.navWeather)
            assertEquals("Tú", s.navYou)
            assertEquals("Tú y Apps", s.navYouAndApps)
            assertEquals("Ajustes", s.settings)
            assertEquals("Actualizar", s.refresh)
            assertEquals("Modo Ambiente", s.ambientMode)
            assertEquals("Buenos Días", s.goodMorning)
            assertEquals("Buenas Tardes", s.goodAfternoon)
            assertEquals("Buenas Noches", s.goodEvening)
            assertEquals("Huésped", s.guest)
            assertEquals("Habitación", s.room)
            assertEquals("Salida", s.checkout)
            assertEquals("AHORA", s.now)
            assertEquals("CLIMA", s.weather)
            assertEquals("RED", s.network)
            assertEquals("CONTINUAR", s.continueWatching)
            assertEquals("EN VIVO", s.liveTv)
            assertEquals("APLICACIONES", s.apps)
            assertEquals("Sin conexión", s.offline)
            assertEquals("Presiona cualquier botón en el control para salir", s.screensaverPrompt)
            assertEquals("Tema de Color", s.theme)
            assertEquals("Idioma de la App", s.appLanguage)
            assertEquals("Guardar y Aplicar", s.saveAndApply)
            assertEquals("Cerrar", s.close)
        }
    }

    @Test
    fun glzHubManagerDeclaresAppLanguageConstant() {
        assertEquals("app_language", GlzHubManager.APP_LANGUAGE)
    }
}
