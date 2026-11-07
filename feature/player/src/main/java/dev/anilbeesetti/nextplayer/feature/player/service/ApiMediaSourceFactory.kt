package dev.anilbeesetti.nextplayer.feature.player.service

import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.source.MediaSource
import dev.anilbeesetti.nextplayer.feature.player.utils.apiHeaders

/** Creates independent sources so headers follow an item, including manifests, segments and subtitles. */
@UnstableApi
internal class ApiMediaSourceFactory(
    private val dataSourceFactory: DataSource.Factory,
    private val createMediaSourceFactory: (DataSource.Factory) -> MediaSource.Factory,
) : MediaSource.Factory by createMediaSourceFactory(dataSourceFactory) {
    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val requestDataSourceFactory = RequestHeadersDataSourceFactory(
            dataSourceFactory,
            mediaItem.apiHeaders,
        )
        return createMediaSourceFactory(requestDataSourceFactory)
            .createMediaSource(mediaItem)
    }
}

@UnstableApi
internal class RequestHeadersDataSourceFactory(
    private val delegate: DataSource.Factory,
    private val headers: Map<String, String>,
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        val source = delegate.createDataSource()
        return object : DataSource by source {
            override fun open(dataSpec: DataSpec): Long = source.open(
                if (dataSpec.uri.scheme in listOf(
                        "http",
                        "https",
                    )
                ) {
                    dataSpec.buildUpon().setHttpRequestHeaders(
                        headers + dataSpec.httpRequestHeaders,
                    ).build()
                } else {
                    dataSpec
                },
            )
        }
    }
}
