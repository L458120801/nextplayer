package dev.anilbeesetti.nextplayer.feature.player.service

import android.content.Context
import android.os.Looper
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.text.TextOutput
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderManager
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import io.github.peerless2012.ass.media.AssHandler
import io.github.peerless2012.ass.media.render.AssRenderer
import java.util.ArrayList

@UnstableApi
internal class NextAssRenderersFactory(
    context: Context,
    decoderManager: DecoderManager,
    private val assHandler: AssHandler,
) : NextRenderersFactory(context) {
    init {
        setDecoderManager(decoderManager)
    }

    override fun buildTextRenderers(
        context: Context,
        output: TextOutput,
        outputLooper: Looper,
        extensionRendererMode: Int,
        out: ArrayList<Renderer>,
    ) {
        super.buildTextRenderers(
            context,
            output,
            outputLooper,
            extensionRendererMode,
            out,
        )
        out.add(AssRenderer(assHandler))
    }
}
