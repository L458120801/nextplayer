package dev.anilbeesetti.nextplayer.core.common.extensions

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FileTest {
    @Test
    fun `get subtitles matches complete media name with language and subtitle qualifiers`() =
        runBlocking {
            val directory = Files.createTempDirectory("subtitle-matching").toFile()
            try {
                val video = File(
                    directory,
                    "Film.2024.mkv",
                ).apply { createNewFile() }
                val expectedSubtitles = setOf(
                    "Film.2024.srt",
                    "Film.2024.CHS.srt",
                    "Film.2024.CHT.forced.ass",
                    "Film.2024.eng.srt",
                    "Film.2024.en-US.srt",
                    "Film.2024.jp.srt",
                    "Film.2024.zh-Hans-CN.srt",
                )
                val ignoredSubtitles = setOf(
                    "Film.srt",
                    "Film.2024.backup.srt",
                    "Film.20240.en.srt",
                    "Film.2024.en.txt",
                )
                (expectedSubtitles + ignoredSubtitles).forEach { subtitleName ->
                    File(
                        directory,
                        subtitleName,
                    ).createNewFile()
                }

                assertEquals(
                    expectedSubtitles,
                    video.getSubtitles().map { it.name }.toSet(),
                )
            } finally {
                directory.deleteRecursively()
            }
        }
}
