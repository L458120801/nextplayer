package dev.anilbeesetti.nextplayer.feature.player.service

import androidx.media3.common.C
import androidx.media3.common.FlagSet
import androidx.media3.common.Format
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AssHandlerPlayerListenerTest {
    @Test
    fun forwardsTransitionBeforeCurrentTrackGroups() {
        val player = ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build()
        try {
            val nextTracks = tracks("next")
            val playerWithCurrentTracks = object : ForwardingPlayer(player) {
                override fun getCurrentTracks(): Tracks = nextTracks
            }
            val dispatches = mutableListOf<String>()
            val listener = listener(dispatches)
            val nextItem = MediaItem.Builder().setMediaId("next").build()

            listener.onMediaItemTransition(
                nextItem,
                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
            )
            listener.onEvents(
                playerWithCurrentTracks,
                playerEvents(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_TRACKS_CHANGED,
                ),
            )

            assertEquals(listOf("transition:next", "tracks:next"), dispatches)
        } finally {
            player.release()
        }
    }

    @Test
    fun forwardsVideoSizeAfterTransitionAndTrackChangesInTheSameBatch() {
        val player = ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build()
        try {
            val nextTracks = tracks("next")
            val playerWithCurrentTracks = object : ForwardingPlayer(player) {
                override fun getCurrentTracks(): Tracks = nextTracks
            }
            val dispatches = mutableListOf<String>()
            val listener = listener(dispatches)
            val nextItem = MediaItem.Builder().setMediaId("next").build()
            val nextVideoSize = VideoSize(1920, 1080)

            listener.onMediaItemTransition(
                nextItem,
                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
            )
            listener.onVideoSizeChanged(nextVideoSize)

            assertEquals(emptyList<String>(), dispatches)

            listener.onEvents(
                playerWithCurrentTracks,
                playerEvents(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_TRACKS_CHANGED,
                    Player.EVENT_VIDEO_SIZE_CHANGED,
                ),
            )

            assertEquals(
                listOf("transition:next", "tracks:next", "videoSize:1920x1080"),
                dispatches,
            )
        } finally {
            player.release()
        }
    }

    @Test
    fun forwardsVideoSizeForAnIndependentVideoSizeEvent() {
        val player = ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build()
        try {
            val dispatches = mutableListOf<String>()
            val listener = listener(dispatches)
            val videoSize = VideoSize(1280, 720)

            listener.onVideoSizeChanged(videoSize)
            listener.onEvents(player, playerEvents(Player.EVENT_VIDEO_SIZE_CHANGED))

            assertEquals(listOf("videoSize:1280x720"), dispatches)
        } finally {
            player.release()
        }
    }

    @Test
    fun waitsForTrackChangeInsteadOfForwardingThePreviousSnapshot() {
        val player = ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build()
        try {
            val nextTracks = tracks("next")
            val playerWithCurrentTracks = object : ForwardingPlayer(player) {
                override fun getCurrentTracks(): Tracks = nextTracks
            }
            val dispatches = mutableListOf<String>()
            val listener = listener(dispatches)

            listener.onMediaItemTransition(
                MediaItem.Builder().setMediaId("next").build(),
                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
            )
            listener.onEvents(
                playerWithCurrentTracks,
                playerEvents(Player.EVENT_MEDIA_ITEM_TRANSITION),
            )
            assertEquals(listOf("transition:next"), dispatches)

            listener.onEvents(
                playerWithCurrentTracks,
                playerEvents(Player.EVENT_TRACKS_CHANGED),
            )

            assertEquals(listOf("transition:next", "tracks:next"), dispatches)
        } finally {
            player.release()
        }
    }

    private fun listener(dispatches: MutableList<String>) = AssHandlerPlayerListener(
        dispatchMediaItemTransition = { item, _ -> dispatches += "transition:${item?.mediaId}" },
        dispatchTracksChanged = { tracks ->
            dispatches += "tracks:${tracks.groups.single().mediaTrackGroup.id}"
        },
        dispatchVideoSizeChanged = { size -> dispatches += "videoSize:${size.width}x${size.height}" },
        dispatchSurfaceSizeChanged = { _, _ -> },
    )

    private fun tracks(id: String) = Tracks(
        listOf(
            Tracks.Group(
                TrackGroup(
                    id,
                    Format.Builder()
                        .setSampleMimeType(MimeTypes.APPLICATION_SUBRIP)
                        .build(),
                ),
                false,
                intArrayOf(C.FORMAT_HANDLED),
                booleanArrayOf(false),
            ),
        ),
    )

    private fun playerEvents(vararg events: Int) = Player.Events(
        FlagSet.Builder().apply { events.forEach(::add) }.build(),
    )
}
