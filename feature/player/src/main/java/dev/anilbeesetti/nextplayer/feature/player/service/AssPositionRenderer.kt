package dev.anilbeesetti.nextplayer.feature.player.service

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.NoSampleRenderer

@OptIn(UnstableApi::class)
internal class AssPositionRenderer(
    private val updateVideoTime: (Long) -> Unit,
) : NoSampleRenderer() {
    private var rendererOffsetUs: Long? = null

    override fun getName(): String = "AssPositionRenderer"

    public override fun onRendererOffsetChanged(offsetUs: Long) {
        rendererOffsetUs = offsetUs
    }

    override fun render(positionUs: Long, elapsedRealtimeUs: Long) {
        val rendererOffsetUs = checkNotNull(rendererOffsetUs) {
            "ASS renderer has no media period offset"
        }
        updateVideoTime(positionUs - rendererOffsetUs)
    }
}
