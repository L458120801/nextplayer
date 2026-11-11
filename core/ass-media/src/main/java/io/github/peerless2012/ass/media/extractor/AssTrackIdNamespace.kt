package io.github.peerless2012.ass.media.extractor

import java.util.concurrent.atomic.AtomicLong

internal class AssTrackIdNamespace {
    private val extractorId = nextExtractorId.getAndIncrement()

    fun trackId(trackId: String): String = "ass-mkv-$extractorId:$trackId"

    private companion object {
        val nextExtractorId = AtomicLong()
    }
}
