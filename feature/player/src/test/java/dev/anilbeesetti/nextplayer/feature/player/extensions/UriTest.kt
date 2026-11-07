package dev.anilbeesetti.nextplayer.feature.player.extensions

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class UriTest {
    @Test
    fun assExternalSubtitleKeepsTheSsaMimeType() {
        assertEquals(
            MimeTypes.TEXT_SSA,
            Uri.parse("file:///storage/emulated/0/Film.chs.ass").getSubtitleMime(),
        )
        assertEquals(
            MimeTypes.APPLICATION_SUBRIP,
            Uri.parse("file:///storage/emulated/0/Film.srt").getSubtitleMime(),
        )
    }

    @Test
    fun relativeDocumentPathFindsVideoFolderUnderGrantedTree() {
        assertEquals(
            listOf(
                "Anime",
                "Season 1",
            ),
            relativeDocumentPath(
                treeDocumentId = "primary:Videos",
                targetDocumentId = "primary:Videos/Anime/Season 1",
            ),
        )
        assertEquals(
            emptyList<String>(),
            relativeDocumentPath(
                treeDocumentId = "primary:Videos/Anime/Season 1",
                targetDocumentId = "primary:Videos/Anime/Season 1",
            ),
        )
        assertEquals(
            null,
            relativeDocumentPath(
                treeDocumentId = "primary:Videos/Other",
                targetDocumentId = "primary:Videos/Anime",
            ),
        )
    }

    @Test
    fun localSubtitleScanUsesVideoUriAndAutoSelectsFirstSortedMatchUnlessSelectionIsRemembered() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication()
            val directory = File(
                context.cacheDir,
                "local-subtitle-uri-test-${System.nanoTime()}",
            ).apply { mkdirs() }
            try {
                val videoName =
                    "[DMG] 冴えない彼女の育てかた 第01話「間違いだらけのプロローグ」 " +
                        "[BDRip][HEVC_FLAC][1080P_Ma10P](7EB2FB58)"
                val video = File(
                    directory,
                    "$videoName.mkv",
                ).apply { createNewFile() }
                val chsSubtitle = File(
                    directory,
                    "$videoName.chs.ass",
                ).apply {
                    writeText("[Script Info]\nTitle: Chinese subtitle\n")
                }
                val chtSubtitle = File(
                    directory,
                    "$videoName.cht.ass",
                ).apply {
                    writeText(
                        "[Script Info]\nTitle: Traditional Chinese subtitle\n",
                    )
                }
                val videoUri = Uri.fromFile(video)
                val persistedVideoStatePath = videoUri.toString()

                assertNotEquals(
                    video.absolutePath,
                    persistedVideoStatePath,
                )

                val autoSelected = context.getLocalSubtitleConfigurations(
                    videoUri = videoUri,
                    subtitleEncoding = "",
                    excludeSubsList = emptyList(),
                    autoSelectFirst = true,
                )
                assertEquals(
                    listOf(
                        Uri.fromFile(chsSubtitle).toString(),
                        Uri.fromFile(chtSubtitle).toString(),
                    ),
                    autoSelected.map { it.id },
                )
                assertEquals(
                    listOf(
                        C.SELECTION_FLAG_DEFAULT,
                        0,
                    ),
                    autoSelected.map { it.selectionFlags },
                )

                val rememberedSelection = context.getLocalSubtitleConfigurations(
                    videoUri = videoUri,
                    subtitleEncoding = "",
                    excludeSubsList = emptyList(),
                    autoSelectFirst = false,
                )
                assertTrue(rememberedSelection.all { it.selectionFlags == 0 })
            } finally {
                directory.deleteRecursively()
            }
        }
}
