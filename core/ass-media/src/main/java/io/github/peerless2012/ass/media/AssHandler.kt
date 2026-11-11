package io.github.peerless2012.ass.media

import android.os.Handler
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.MimeTypes.TEXT_SSA
import androidx.media3.common.Player.Listener
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import io.github.peerless2012.ass.AssRender
import io.github.peerless2012.ass.AssTrack
import io.github.peerless2012.ass.Ass
import io.github.peerless2012.ass.media.parser.AssHeaderParser
import io.github.peerless2012.ass.media.render.AssOverlayManager
import io.github.peerless2012.ass.media.type.AssRenderType

/**
 * Handles ASS subtitle rendering and integration with ExoPlayer.
 *
 * This class listens to ExoPlayer events and manages the creation, selection, and rendering of ASS
 * subtitle tracks.
 * @param renderType The subtitle render type.
 */
@OptIn(UnstableApi::class)
class AssHandler(
    val renderType: AssRenderType,
    val config: AssHandlerConfig = AssHandlerConfig()
    ) : Listener {

    /** The ASS instance used for creating tracks and renderers. This is lazy to avoid loading
     * libass if the played media does not have ASS tracks. */
    private val assDelegate = lazy { Ass() }
    val ass by assDelegate

    /** The current ASS renderer. It's created as soon as a ASS track is detected. */
    var render: AssRender? = null
        private set

    /**
     * AssRender changed callback
     */
    var renderCallback: ((AssRender?) -> Unit)? = null

    /** The currently selected ASS track. */
    var track: AssTrack? = null
        private set

    /** ASS tracks prepared by each extractor until the current media's track groups are known. */
    internal val availableTracks = AssTrackRegistry()

    /** Fonts encountered before any ASS track was created. Flushed in [createTrack]. */
    private val pendingFonts = mutableListOf<Pair<String, ByteArray>>()

    /** The size of the video track. */
    var videoSize = Size.ZERO
        private set

    /** The size of the surface on which subtitles are rendered. */
    var surfaceSize = Size.ZERO
        private set

    /**  The video frame time. (default is 24fps)  */
    val videoFramePeriod = 3

    private var videoFrameIndex = 0

    var videoTime = -1L
        set(value) {
            if (field == value) {
                return
            }
            field = value
            if (videoFrameIndex == 0) {
                videoTimeCallback?.invoke(value)
            }
            videoFrameIndex++
            if (videoFrameIndex >= videoFramePeriod) {
                videoFrameIndex %= videoFramePeriod
            }
        }

    var videoTimeCallback: ((Long) -> Unit)? = null

    /** The overlay manager for toggling the effects renderer. */
    private var overlayManager: AssOverlayManager? = null

    /** The current selected ass format. */
    private var format: Format? = null

    /** The playback control thread handler. */
    private lateinit var handler: Handler

    /**
     * Initializes the handler with the provided ExoPlayer instance.
     * @param player The ExoPlayer instance to attach to.
     */
    fun init(player: ExoPlayer) {
        player.addListener(this)
        handler = Handler(player.applicationLooper)
        if (renderType == AssRenderType.EFFECTS_CANVAS || renderType == AssRenderType.EFFECTS_OPEN_GL) {
            overlayManager = AssOverlayManager(this, player, renderType == AssRenderType.EFFECTS_OPEN_GL)
        }
    }

    /**
     * Handles transitions between media items in the player and resets everything to the initial
     * state.
     */
    @Synchronized
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        Log.i("AssHandler", "onMediaItemTransition: item = ${mediaItem}, reason = $reason")
        format = null
        track = null
        render?.setTrack(null)
        videoSize = Size.ZERO
        videoTime = -1
        videoFrameIndex = 0
        overlayManager?.disable()
    }

    /**
     * Handles changes to the tracks available in the current media.
     * Configures the selected ASS track if available.
     * @param tracks The selected tracks.
     */
    @Synchronized
    override fun onTracksChanged(tracks: Tracks) {
        Log.i("AssHandler", "onTracksChanged $tracks")

        if (tracks.groups.isNotEmpty()) {
            val currentTrackIds = tracks.groups
                .flatMap { group ->
                    (0 until group.length).mapNotNull { index ->
                        group.getTrackFormat(index).takeIf { format ->
                            format.sampleMimeType == TEXT_SSA || format.codecs == TEXT_SSA
                        }?.id
                    }
                }
                .flatMapTo(mutableSetOf(), AssTrackRegistry::trackIdCandidates)
            val removedTracks = availableTracks.retainTrackIds(currentTrackIds)
            if (removedTracks.any { it === track }) {
                track = null
                render?.setTrack(null)
            }
            removedTracks.forEach(AssTrack::release)
        }

        val selectedVideoTrack = getSelectedVideoTrack(tracks)
        if (selectedVideoTrack != null) {
            setVideoSize(selectedVideoTrack.width, selectedVideoTrack.height)
        }

        format = getSelectedAssTrack(tracks)
        if (format == null) {
            Log.i("AssHandler", "subtitle track disabled")
            track = null
            render?.setTrack(null)
            overlayManager?.disable()
            return
        }

        updateTrack()
    }

    private fun updateTrack() {
        val track = availableTracks.getForFormatId(format?.id)
        if (track == null || this.track == track) return

        Log.i("AssHandler", "subtitle track changed to $format")
        this.track = track
        val render = requireNotNull(render)
        render.setStorageSize(videoSize.width, videoSize.height)
        if (renderType == AssRenderType.OVERLAY_CANVAS || renderType == AssRenderType.OVERLAY_OPEN_GL) {
            val renderSize = computeRenderSize(surfaceSize.width, surfaceSize.height)
            render.setFrameSize(renderSize.width, renderSize.height)
        } else {
            val renderSize = computeRenderSize(videoSize.width, videoSize.height)
            render.setFrameSize(renderSize.width, renderSize.height)
        }
        render.setTrack(track)

        // Player func call need in create thread.
        overlayManager?.let {
            handler.post { it.enable(render) }
        }
    }

    /**
     * Handles changes to the surface size for video playback.
     * Notifies the callback if the size has changed.
     * @param width The new width of the surface.
     * @param height The new height of the surface.
     */
    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        super.onSurfaceSizeChanged(width, height)
        Log.i("AssHandler", "onSurfaceSizeChanged: width = $width, height = $height")
        if (surfaceSize.width == width && surfaceSize.height == height) return
        surfaceSize = Size(width, height)
        if ((renderType == AssRenderType.OVERLAY_CANVAS || renderType == AssRenderType.OVERLAY_OPEN_GL) && surfaceSize.isValid) {
            val renderSize = computeRenderSize(width, height)
            if (renderSize.width != width || renderSize.height != height) {
                Log.i("AssHandler", "Downscaling render: ${width}x${height} -> ${renderSize.width}x${renderSize.height} (maxPixels=${config.maxRenderPixels})")
            }
            render?.setFrameSize(renderSize.width, renderSize.height)
        }
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        super.onVideoSizeChanged(videoSize)
        Log.i("AssHandler", "onVideoSizeChanged: width = ${videoSize.width}, height = ${videoSize.height}")
        setVideoSize(videoSize.width, videoSize.height)
    }

    /**
     * Updates the video size for the ASS renderer. Called as soon as the video size is known in
     * order to properly render subtitles.
     * @param width The width of the video.
     * @param height The height of the video.
     */
    fun setVideoSize(width: Int, height: Int) {
        Log.i("AssHandler", "setVideoSize: width = $width, height = $height")
        videoSize = Size(width, height)
        if (!videoSize.isValid) return

        render?.let { activeRender ->
            activeRender.setStorageSize(width, height)
            if (renderType != AssRenderType.OVERLAY_CANVAS && renderType != AssRenderType.OVERLAY_OPEN_GL) {
                val renderSize = computeRenderSize(width, height)
                activeRender.setFrameSize(renderSize.width, renderSize.height)
            }
        }
    }

    /**
     * Returns true if the current media has ASS tracks, false otherwise.
     */
    fun hasTracks(): Boolean {
        return !availableTracks.isEmpty
    }

    /**
     * Adds a font to the ASS library. If no tracks have been created yet, the font is buffered
     * and will be added when the first track is created via [createTrack].
     */
    @Synchronized
    fun addFont(name: String, data: ByteArray) {
        if (hasTracks()) {
            ass.addFont(name, data)
        } else {
            pendingFonts.add(name to data)
        }
    }

    /**
     * Creates a new ASS track from the given format and saves it in the [availableTracks].
     * The renderer and libass are also created if needed.
     * @param format The format of the ASS track.
     * @return The created ASS track.
     */
    @Synchronized
    fun createTrack(format: Format): AssTrack {
        Log.i("AssHandler", "createTrack: format = $format")
        // Ensure the renderer is created before creating tracks.
        createRenderIfNeeded()

        // Flush any fonts that were buffered before the first track was created.
        if (pendingFonts.isNotEmpty()) {
            for ((name, data) in pendingFonts) {
                ass.addFont(name, data)
            }
            pendingFonts.clear()
        }

        val track = ass.createTrack()
        if (format.initializationData.size > 0) {
            val header = AssHeaderParser.parse(format, renderType != AssRenderType.CUES)
            track.readBuffer(header)
        }
        val previousTrack = availableTracks.put(requireNotNull(format.id), track)
        if (previousTrack === this.track) {
            this.track = null
            render?.setTrack(null)
        }
        previousTrack?.release()

        updateTrack()

        return track
    }

    /**
     * Ensures the ASS renderer is created if it does not already exist.
     */
    private fun createRenderIfNeeded() {
        if (render != null) return
        Log.i("AssHandler", "createRender")
        render = ass.createRender().also { render ->
            if (videoSize.isValid) {
                render.setStorageSize(videoSize.width, videoSize.height)
            }
            if (videoSize.isValid) {
                val renderSize = computeRenderSize(videoSize.width, videoSize.height)
                render.setFrameSize(renderSize.width, renderSize.height)
            }
            if (renderType == AssRenderType.OVERLAY_CANVAS || renderType == AssRenderType.OVERLAY_OPEN_GL) {
                if (surfaceSize.isValid) {
                    val renderSize = computeRenderSize(surfaceSize.width, surfaceSize.height)
                    render.setFrameSize(renderSize.width, renderSize.height)
                }
            } else {
                if (videoSize.isValid) {
                    val renderSize = computeRenderSize(videoSize.width, videoSize.height)
                    render.setFrameSize(renderSize.width, renderSize.height)
                }
            }
            Log.i("AssHandler", "Ass cacheSize: ${config.cacheSize}MB")
            Log.i("AssHandler", "Ass glyphSize: ${config.glyphSize}")
            render.setCacheLimit(config.glyphSize, config.cacheSize)
        }
        renderCallback?.invoke(render)
    }

    /**
     * Reads a dialogue into the track of the given [trackId].
     * Thread-safe: AssTrack.readChunk internally acquires the shared libass lock.
     */
    fun readTrackDialogue(
        trackId: String?,
        start: Long,
        duration: Long,
        data: ByteArray,
        offset: Int = 0,
        length: Int = data.size
    ) {
        availableTracks.readTrackDialogue(trackId, start, duration, data, offset, length)
    }

    /**
     * Retrieves the selected video track, if any.
     */
    private fun getSelectedVideoTrack(tracks: Tracks): Format? {
        return tracks.groups.find { group ->
            if (group.isSelected) {
                (0 until group.length).any { index ->
                    val track = group.getTrackFormat(index)
                    MimeTypes.isVideo(track.sampleMimeType)
                }
            } else {
                false
            }
        }?.getTrackFormat(0)
    }

    /**
     * Retrieves the ID of the selected ASS track, if any.
     * @param tracks The selected tracks.
     * @return The ID of the selected ASS track, or null if none.
     */
    private fun getSelectedAssTrack(tracks: Tracks): Format? {
        return tracks.groups.find { group ->
            if (group.isSelected) {
                (0 until group.length).any { index ->
                    val track = group.getTrackFormat(index)
                    track.sampleMimeType == TEXT_SSA || track.codecs == TEXT_SSA
                }
            } else {
                false
            }
        }?.getTrackFormat(0)
    }

    /**
     * Releases all native resources held by this handler.
     */
    fun release() {
        videoTimeCallback = null
        overlayManager?.disable()
        render?.release()
        render = null
        availableTracks.releaseAll()
        track = null
        pendingFonts.clear()
        if (assDelegate.isInitialized()) {
            ass.release()
        }
    }

    /**
     * Checks if the size is valid (both width and height are greater than 0).
     */
    private val Size.isValid
        get() = width > 0 && height > 0

    /**
     * Computes the actual render size, downscaling proportionally if the frame
     * exceeds [AssHandlerConfig.maxRenderPixels].
     *
     * The result is aligned to even numbers for consistent libass internal layout.
     *
     * @param width  Target frame width (surface or video size)
     * @param height Target frame height
     * @return The (possibly downscaled) render size
     */
    fun computeRenderSize(width: Int, height: Int): Size {
        if (renderType == AssRenderType.CUES) return Size(width, height)

        val max = config.maxRenderPixels
        if (max <= 0) return Size(width, height)
        val pixels = width.toLong() * height
        if (pixels <= max) return Size(width, height)
        val scale = Math.sqrt(max.toDouble() / pixels).toFloat()
        // Align to even numbers for libass internal layout
        val w = ((width * scale).toInt() and 0x7FFFFFFE).coerceAtLeast(2)
        val h = ((height * scale).toInt() and 0x7FFFFFFE).coerceAtLeast(2)
        return Size(w, h)
    }
}
