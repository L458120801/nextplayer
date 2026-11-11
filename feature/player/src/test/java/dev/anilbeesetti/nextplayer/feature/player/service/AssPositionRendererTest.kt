package dev.anilbeesetti.nextplayer.feature.player.service

import org.junit.Assert.assertEquals
import org.junit.Test

class AssPositionRendererTest {
    @Test
    fun convertsInitialRendererPositionToMediaPosition() {
        val positions = mutableListOf<Long>()
        val renderer = AssPositionRenderer(positions::add)
        val initialOffsetUs = 1_000_000_000_000L
        val mediaPositionUs = 12_000_000L

        renderer.onRendererOffsetChanged(initialOffsetUs)
        renderer.render(initialOffsetUs + mediaPositionUs, elapsedRealtimeUs = 0L)

        assertEquals(listOf(mediaPositionUs), positions)
    }

    @Test
    fun convertsAutomaticNextPeriodPositionWithAccumulatedPreviousDuration() {
        val positions = mutableListOf<Long>()
        val renderer = AssPositionRenderer(positions::add)
        val initialOffsetUs = 1_000_000_000_000L
        val previousMediaDurationUs = 1_800_000_000L
        val nextMediaPositionUs = 250_000L
        val nextPeriodOffsetUs = initialOffsetUs + previousMediaDurationUs

        renderer.onRendererOffsetChanged(nextPeriodOffsetUs)
        renderer.render(nextPeriodOffsetUs + nextMediaPositionUs, elapsedRealtimeUs = 0L)

        assertEquals(listOf(nextMediaPositionUs), positions)
    }

    @Test
    fun usesResetPeriodOffsetAfterManualSeek() {
        val positions = mutableListOf<Long>()
        val renderer = AssPositionRenderer(positions::add)
        val initialOffsetUs = 1_000_000_000_000L
        val previousMediaDurationUs = 1_800_000_000L
        val seekPositionUs = 30_000_000L

        renderer.onRendererOffsetChanged(initialOffsetUs + previousMediaDurationUs)
        renderer.onRendererOffsetChanged(initialOffsetUs)
        renderer.render(initialOffsetUs + seekPositionUs, elapsedRealtimeUs = 0L)

        assertEquals(listOf(seekPositionUs), positions)
    }
}
