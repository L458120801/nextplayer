package dev.anilbeesetti.nextplayer.feature.player

import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import dev.anilbeesetti.nextplayer.feature.player.extensions.toContentScale
import dev.anilbeesetti.nextplayer.feature.player.state.PictureInPictureState
import dev.anilbeesetti.nextplayer.feature.player.state.VideoZoomAndContentScaleState
import dev.anilbeesetti.nextplayer.feature.player.ui.AssSubtitleOverlay
import dev.anilbeesetti.nextplayer.feature.player.ui.ShutterView
import dev.anilbeesetti.nextplayer.feature.player.ui.SubtitleConfiguration
import dev.anilbeesetti.nextplayer.feature.player.ui.SubtitleView
import io.github.peerless2012.ass.media.AssHandler

@OptIn(UnstableApi::class)
@Composable
fun PlayerContentFrame(
    modifier: Modifier = Modifier,
    player: Player,
    assHandler: AssHandler?,
    pictureInPictureState: PictureInPictureState,
    videoZoomAndContentScaleState: VideoZoomAndContentScaleState,
    subtitleConfiguration: SubtitleConfiguration,
) {
    val presentationState = rememberPresentationState(player = player)
    val videoContentModifier = Modifier.resizeWithContentScale(
        contentScale = videoZoomAndContentScaleState.videoContentScale.toContentScale(),
        sourceSizeDp = presentationState.videoSizeDp?.let { size ->
            size.copy(
                width = with(LocalDensity.current) { size.width.toDp().value },
                height = with(LocalDensity.current) { size.height.toDp().value },
            )
        },
    )
    val applyVideoTransform: GraphicsLayerScope.() -> Unit = {
        scaleX = videoZoomAndContentScaleState.zoom
        scaleY = videoZoomAndContentScaleState.zoom
        translationX = videoZoomAndContentScaleState.offset.x
        translationY = videoZoomAndContentScaleState.offset.y
    }

    Box(modifier.fillMaxSize()) {
        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = videoContentModifier
                .onGloballyPositioned {
                    val bounds = it.boundsInWindow()
                    val rect = Rect(
                        bounds.left.toInt(),
                        bounds.top.toInt(),
                        bounds.right.toInt(),
                        bounds.bottom.toInt(),
                    )
                    pictureInPictureState.setVideoViewRect(rect = rect)
                }
                .graphicsLayer(block = applyVideoTransform),
        )

        SubtitleView(
            player = player,
            isInPictureInPictureMode = pictureInPictureState.isInPictureInPictureMode,
            configuration = subtitleConfiguration,
        )

        assHandler?.let { handler ->
            AssSubtitleOverlay(
                modifier = videoContentModifier.graphicsLayer(block = applyVideoTransform),
                assHandler = handler,
            )
        }

        if (presentationState.coverSurface) {
            ShutterView()
        }
    }
}
