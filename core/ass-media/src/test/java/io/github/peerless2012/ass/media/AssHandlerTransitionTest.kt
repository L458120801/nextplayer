package io.github.peerless2012.ass.media

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import io.github.peerless2012.ass.AssTrack
import io.github.peerless2012.ass.media.type.AssRenderType
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AssHandlerTransitionTest {
    @Test
    fun keepsPreloadedTrackAcrossTransitionThenReleasesThePreviousMediaTrack() {
        val handler = AssHandler(AssRenderType.OVERLAY_OPEN_GL)
        val previousTrack = mock(AssTrack::class.java)
        val preparedNextTrack = mock(AssTrack::class.java)
        handler.availableTracks.put("ass-mkv-7:3", previousTrack)
        handler.availableTracks.put("ass-mkv-8:3", preparedNextTrack)

        try {
            handler.onMediaItemTransition(
                MediaItem.Builder().setMediaId("next").build(),
                androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
            )

            assertSame(
                preparedNextTrack,
                handler.availableTracks.getForFormatId("ass-mkv-8:3"),
            )

            handler.onTracksChanged(currentTextTracks("ass-mkv-8:3"))

            assertSame(
                preparedNextTrack,
                handler.availableTracks.getForFormatId("ass-mkv-8:3"),
            )
            verify(previousTrack).release()
        } finally {
            handler.release()
        }
    }

    private fun currentTextTracks(trackId: String): Tracks = Tracks(
        listOf(
            Tracks.Group(
                TrackGroup(
                    trackId,
                    Format.Builder()
                        .setId(trackId)
                        .setSampleMimeType(MimeTypes.TEXT_SSA)
                        .build(),
                ),
                false,
                intArrayOf(C.FORMAT_HANDLED),
                booleanArrayOf(false),
            ),
        ),
    )
}
