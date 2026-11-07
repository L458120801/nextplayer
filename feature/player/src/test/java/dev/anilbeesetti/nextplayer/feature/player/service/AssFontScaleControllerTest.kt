package dev.anilbeesetti.nextplayer.feature.player.service

import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class AssFontScaleControllerTest {
    @Test
    fun defaultSubtitleSizeKeepsAssFontScaleAtOne() {
        assertEquals(
            1f,
            assFontScaleForSubtitleSize(PlayerPreferences.DEFAULT_SUBTITLE_TEXT_SIZE),
            0f,
        )
    }

    @Test
    fun minimumAndMaximumSubtitleSizesScaleProportionally() {
        assertEquals(
            0.5f,
            assFontScaleForSubtitleSize(10),
            0f,
        )
        assertEquals(
            3f,
            assFontScaleForSubtitleSize(60),
            0f,
        )
    }

    @Test
    fun sizeChangesUpdateCurrentRendererAndApplyToLaterRenderer() {
        val appliedScales = mutableListOf<Pair<String, Float>>()
        val controller = AssFontScaleController<String> { renderer, scale ->
            appliedScales += renderer to scale
        }

        controller.updateTextSize(
            textSize = 40,
            currentRenderer = "current",
        )
        controller.onRendererCreated("created-later")

        assertEquals(
            listOf(
                "current" to 2f,
                "created-later" to 2f,
            ),
            appliedScales,
        )
    }

    @Test
    fun sizeChangedBeforeRendererCreationIsAppliedWhenRendererAppears() {
        val appliedScales = mutableListOf<Pair<String, Float>>()
        val controller = AssFontScaleController<String> { renderer, scale ->
            appliedScales += renderer to scale
        }

        controller.updateTextSize(
            textSize = 30,
            currentRenderer = null,
        )
        controller.onRendererCreated("created-later")

        assertEquals(
            listOf("created-later" to 1.5f),
            appliedScales,
        )
    }
}
