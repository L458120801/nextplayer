package dev.anilbeesetti.nextplayer.feature.player.service

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize

internal class AssHandlerPlayerListener(
    private val dispatchMediaItemTransition: (MediaItem?, Int) -> Unit,
    private val dispatchTracksChanged: (Tracks) -> Unit,
    private val dispatchVideoSizeChanged: (VideoSize) -> Unit,
    private val dispatchSurfaceSizeChanged: (Int, Int) -> Unit,
) : Player.Listener {
    private var pendingTransition: Pair<MediaItem?, Int>? = null
    private var pendingVideoSize: VideoSize? = null

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        pendingTransition = mediaItem to reason
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        pendingVideoSize = videoSize
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        dispatchSurfaceSizeChanged(width, height)
    }

    override fun onEvents(player: Player, events: Player.Events) {
        val hasTransition = events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)
        val hasTracksChanged = events.contains(Player.EVENT_TRACKS_CHANGED)
        val hasVideoSizeChanged = events.contains(Player.EVENT_VIDEO_SIZE_CHANGED)
        if (!hasTransition && !hasTracksChanged && !hasVideoSizeChanged) return

        if (hasTransition) {
            pendingTransition?.let { (mediaItem, reason) ->
                dispatchMediaItemTransition(mediaItem, reason)
            }
        }
        if (hasTracksChanged) dispatchTracksChanged(player.currentTracks)
        if (hasVideoSizeChanged) pendingVideoSize?.let(dispatchVideoSizeChanged)
        pendingTransition = null
        pendingVideoSize = null
    }
}
