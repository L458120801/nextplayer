package io.github.peerless2012.ass.media

import io.github.peerless2012.ass.AssTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock

class AssTrackRegistryTest {
    @Test
    fun resolvesTrackIdAfterMedia3PeriodPrefixes() {
        val track = mock(AssTrack::class.java)
        val registry = AssTrackRegistry().apply {
            put("ass-mkv-7:3", track)
        }

        assertSame(track, registry.getForFormatId("period:child:ass-mkv-7:3"))
    }

    @Test
    fun keepsPreparedNextTrackAndRemovesPreviousTrackWhenGroupsChange() {
        val previousTrack = mock(AssTrack::class.java)
        val nextTrack = mock(AssTrack::class.java)
        val registry = AssTrackRegistry().apply {
            put("ass-mkv-7:3", previousTrack)
            put("ass-mkv-8:3", nextTrack)
        }

        val removedTracks = registry.retainTrackIds(setOf("period:child:ass-mkv-8:3"))

        assertEquals(listOf(previousTrack), removedTracks)
        assertSame(nextTrack, registry.getForFormatId("ass-mkv-8:3"))
    }

    @Test
    fun doesNotMatchTheSameTrackNumberFromAnotherExtractor() {
        val registry = AssTrackRegistry().apply {
            put("ass-mkv-7:3", mock(AssTrack::class.java))
        }

        assertNull(registry.getForFormatId("period:ass-mkv-8:3"))
    }
}
