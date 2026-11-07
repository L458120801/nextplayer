package dev.anilbeesetti.nextplayer.feature.player.service

import io.github.peerless2012.ass.AssRender
import io.github.peerless2012.ass.media.AssHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AssSubtitleRuntime {
    private val mutableHandler = MutableStateFlow<AssHandler?>(value = null)
    private val fontScaleController = AssFontScaleController<AssRender> { renderer, scale ->
        renderer.setFontScale(scale)
    }

    val handler: StateFlow<AssHandler?> = mutableHandler.asStateFlow()

    fun publish(handler: AssHandler) {
        handler.renderCallback = fontScaleController::onRendererCreated
        fontScaleController.onRendererCreated(handler.render)
        mutableHandler.value = handler
    }

    fun updateSubtitleTextSize(textSize: Int) {
        fontScaleController.updateTextSize(
            textSize = textSize,
            currentRenderer = mutableHandler.value?.render,
        )
    }

    fun clear(handler: AssHandler) {
        handler.renderCallback = null
        if (mutableHandler.value === handler) {
            mutableHandler.value = null
        }
    }
}
