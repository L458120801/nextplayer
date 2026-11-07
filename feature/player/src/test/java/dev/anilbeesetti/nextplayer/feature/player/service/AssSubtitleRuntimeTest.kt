package dev.anilbeesetti.nextplayer.feature.player.service

import io.github.peerless2012.ass.media.AssHandler
import io.github.peerless2012.ass.media.type.AssRenderType
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AssSubtitleRuntimeTest {
    @Test
    fun handlerIsSharedWithThePlayerScreenUntilTheServiceReleasesIt() {
        val runtime = AssSubtitleRuntime
        val handler = AssHandler(AssRenderType.OVERLAY_OPEN_GL)

        runtime.publish(handler)

        assertSame(
            handler,
            runtime.handler.value,
        )

        runtime.clear(handler)

        assertNull(runtime.handler.value)
        handler.release()
    }

    @Test
    fun clearingAnOldHandlerDoesNotHideTheCurrentServiceHandler() {
        val runtime = AssSubtitleRuntime
        val oldHandler = AssHandler(AssRenderType.OVERLAY_OPEN_GL)
        val currentHandler = AssHandler(AssRenderType.OVERLAY_OPEN_GL)

        runtime.publish(oldHandler)
        runtime.publish(currentHandler)
        runtime.clear(oldHandler)

        assertSame(
            currentHandler,
            runtime.handler.value,
        )

        runtime.clear(currentHandler)
        oldHandler.release()
        currentHandler.release()
    }
}
