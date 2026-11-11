package io.github.peerless2012.ass.media

import io.github.peerless2012.ass.AssTrack

internal class AssTrackRegistry {
    private val tracks = linkedMapOf<String, AssTrack>()

    @get:Synchronized
    val isEmpty: Boolean
        get() = tracks.isEmpty()

    @Synchronized
    fun getForFormatId(formatId: String?): AssTrack? {
        if (formatId == null) return null
        return resolveTrackId(formatId)?.let(tracks::get)
    }

    @Synchronized
    fun put(trackId: String, track: AssTrack): AssTrack? = tracks.put(trackId, track)

    @Synchronized
    fun retainTrackIds(trackIds: Set<String>): List<AssTrack> {
        val currentTrackIds = trackIds.mapNotNullTo(mutableSetOf(), ::resolveTrackId)
        val removedTracks = mutableListOf<AssTrack>()
        val iterator = tracks.iterator()
        while (iterator.hasNext()) {
            val (trackId, track) = iterator.next()
            if (trackId !in currentTrackIds) {
                removedTracks += track
                iterator.remove()
            }
        }
        return removedTracks
    }

    @Synchronized
    fun readTrackDialogue(
        trackId: String?,
        start: Long,
        duration: Long,
        data: ByteArray,
        offset: Int,
        length: Int,
    ) {
        val track = getForFormatId(trackId) ?: return
        track.readChunk(start, duration, data, offset, length)
    }

    @Synchronized
    fun releaseAll() {
        tracks.values.forEach(AssTrack::release)
        tracks.clear()
    }

    private fun resolveTrackId(formatId: String): String? {
        if (formatId in tracks) return formatId
        return tracks.keys.firstOrNull { trackId -> formatId.endsWith(":$trackId") }
    }

    companion object {
        fun trackIdCandidates(formatId: String): Set<String> = setOf(formatId)
    }
}
