package dev.anilbeesetti.nextplayer.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPreferencesTest {
    @Test
    fun `existing preferences keep tunneled playback disabled`() {
        val preferences = Json.decodeFromString<PlayerPreferences>(
            string = """{"autoplay":false,"preferredAudioLanguage":"en"}""",
        )

        assertFalse(preferences.enableTunneledPlayback)
        assertFalse(preferences.autoplay)
        assertEquals(
            "en",
            preferences.preferredAudioLanguage,
        )
    }

    @Test
    fun `tunneled playback survives serialization`() {
        val preferences = PlayerPreferences(enableTunneledPlayback = true)
        val restored = Json.decodeFromString<PlayerPreferences>(
            Json.encodeToString(value = preferences),
        )

        assertTrue(restored.enableTunneledPlayback)
        assertEquals(
            preferences,
            restored,
        )
    }

    @Test
    fun `long press background opacity defaults to opaque and survives serialization`() {
        val defaultPreferences = Json.decodeFromString<PlayerPreferences>("{}")
        assertEquals(
            PlayerPreferences.DEFAULT_LONG_PRESS_BACKGROUND_OPACITY,
            defaultPreferences.longPressBackgroundOpacity,
        )

        listOf(
            0f,
            0.5f,
            1f,
        ).forEach { opacity ->
            val preferences = PlayerPreferences(longPressBackgroundOpacity = opacity)
            val restored = Json.decodeFromString<PlayerPreferences>(
                Json.encodeToString(value = preferences),
            )
            assertEquals(
                opacity,
                restored.longPressBackgroundOpacity,
            )
        }
    }
}
