package io.github.peerless2012.ass.media

import androidx.media3.common.VideoSize
import io.github.peerless2012.ass.AssRender
import io.github.peerless2012.ass.media.type.AssRenderType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AssHandlerRenderSizeTest {
    @Test
    fun cuesKeepsFullFrameSizeWhenPixelLimitIsConfigured() {
        val handler = AssHandler(
            AssRenderType.CUES,
            AssHandlerConfig(maxRenderPixels = 2_073_600),
        )
        val overlayHandler = AssHandler(
            AssRenderType.OVERLAY_CANVAS,
            AssHandlerConfig(maxRenderPixels = 2_073_600),
        )

        val cuesSize = handler.computeRenderSize(3840, 2160)
        val overlaySize = overlayHandler.computeRenderSize(3840, 2160)

        assertEquals(3840, cuesSize.width)
        assertEquals(2160, cuesSize.height)
        assertEquals(1920, overlaySize.width)
        assertEquals(1080, overlaySize.height)
    }

    @Test
    fun videoSizeChangeUpdatesCuesRendererStorageAndFrameSizes() {
        val handler = AssHandler(
            AssRenderType.CUES,
            AssHandlerConfig(maxRenderPixels = 2_073_600),
        )
        val render = mock(AssRender::class.java)
        setRender(handler, render)

        try {
            handler.onVideoSizeChanged(VideoSize(3840, 2160))

            verify(render).setStorageSize(3840, 2160)
            verify(render).setFrameSize(3840, 2160)
        } finally {
            handler.release()
        }
    }

    @Test
    fun videoSizeChangeDoesNotReplaceOverlayFrameSize() {
        val handler = AssHandler(
            AssRenderType.OVERLAY_CANVAS,
            AssHandlerConfig(maxRenderPixels = 2_073_600),
        )
        val render = mock(AssRender::class.java)
        setRender(handler, render)

        try {
            handler.onSurfaceSizeChanged(1280, 720)
            handler.onVideoSizeChanged(VideoSize(3840, 2160))

            verify(render).setStorageSize(3840, 2160)
            verify(render).setFrameSize(1280, 720)
            verify(render, never()).setFrameSize(3840, 2160)
        } finally {
            handler.release()
        }
    }

    private fun setRender(handler: AssHandler, render: AssRender) {
        AssHandler::class.java.getDeclaredField("render").apply {
            isAccessible = true
            set(handler, render)
        }
    }
}
