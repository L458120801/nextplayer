package dev.anilbeesetti.nextplayer.feature.player.service

import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences

internal fun assFontScaleForSubtitleSize(textSize: Int): Float =
    textSize.toFloat() / PlayerPreferences.DEFAULT_SUBTITLE_TEXT_SIZE

internal class AssFontScaleController<T>(private val applyFontScale: (T, Float) -> Unit) {
    private var currentScale = assFontScaleForSubtitleSize(
        PlayerPreferences.DEFAULT_SUBTITLE_TEXT_SIZE,
    )

    fun updateTextSize(textSize: Int, currentRenderer: T?) {
        currentScale = assFontScaleForSubtitleSize(textSize)
        currentRenderer?.let {
            applyFontScale(
                it,
                currentScale,
            )
        }
    }

    fun onRendererCreated(renderer: T?) {
        renderer?.let {
            applyFontScale(
                it,
                currentScale,
            )
        }
    }
}
