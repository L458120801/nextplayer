package dev.anilbeesetti.nextplayer.core.common.extensions

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun File.getSubtitles(): List<File> = withContext(Dispatchers.IO) {
    val mediaName = nameWithoutExtension
    val parentDir = parentFile ?: File(".")
    parentDir.listFiles()
        ?.asSequence()
        ?.filter { it.isFile && it.extension.lowercase(Locale.ROOT) in subtitleExtensions }
        ?.filter { subtitle -> subtitle.nameWithoutExtension.isSubtitleFor(mediaName) }
        ?.sortedBy { it.name.lowercase(Locale.ROOT) }
        ?.toList()
        .orEmpty()
}

private val subtitleExtensions = setOf(
    "srt",
    "ssa",
    "ass",
    "vtt",
    "ttml",
)
private val subtitleFlags = setOf(
    "forced",
    "default",
    "sdh",
    "cc",
    "hi",
    "commentary",
)
private val subtitleLanguageCodes = setOf(
    "ara",
    "ben",
    "bul",
    "cat",
    "ces",
    "chi",
    "chs",
    "cht",
    "dan",
    "deu",
    "dut",
    "ell",
    "eng",
    "fas",
    "fin",
    "fre",
    "fra",
    "ger",
    "gre",
    "heb",
    "hin",
    "hrv",
    "hun",
    "ind",
    "ita",
    "jpn",
    "jp",
    "kan",
    "kor",
    "kr",
    "mal",
    "mar",
    "msa",
    "may",
    "nld",
    "nor",
    "per",
    "pol",
    "por",
    "ron",
    "rum",
    "rus",
    "slk",
    "slo",
    "spa",
    "srp",
    "swe",
    "tam",
    "tel",
    "tha",
    "tgl",
    "tur",
    "ukr",
    "urd",
    "vie",
    "zho",
    "und",
)
private val subtitleLanguageTagPattern = Regex(
    pattern = "[a-z]{2,3}(?:-[a-z0-9]{2,8})*",
    option = RegexOption.IGNORE_CASE,
)
private val isoLanguageCodes = Locale.getISOLanguages().mapTo(mutableSetOf()) {
    it.lowercase(Locale.ROOT)
}
private val supportedSubtitleLanguageCodes = isoLanguageCodes + subtitleLanguageCodes

private fun String.isSubtitleFor(mediaName: String): Boolean {
    val isExactName = equals(
        other = mediaName,
        ignoreCase = true,
    )
    if (isExactName) return true
    val startsWithMediaName = startsWith(
        prefix = "$mediaName.",
        ignoreCase = true,
    )
    if (!startsWithMediaName) return false

    val qualifiers = substring(startIndex = mediaName.length + 1).split('.')
    return qualifiers.isNotEmpty() && qualifiers.all { qualifier ->
        val normalizedQualifier = qualifier.lowercase(Locale.ROOT)
        val isLanguageTag = normalizedQualifier.matches(
            subtitleLanguageTagPattern,
        ) && normalizedQualifier.substringBefore('-') in supportedSubtitleLanguageCodes
        normalizedQualifier in subtitleFlags || isLanguageTag
    }
}

suspend fun File.getLocalSubtitles(
    context: Context,
    excludeSubsList: List<Uri> = emptyList(),
): List<Uri> = withContext(Dispatchers.Default) {
    val excludeSubsPathSet = excludeSubsList.mapNotNull { context.getPath(uri = it) }.toSet()

    getSubtitles().mapNotNull { file ->
        if (file.path !in excludeSubsPathSet) {
            file.toUri()
        } else {
            null
        }
    }
}

fun String.getThumbnail(): File? {
    val filePathWithoutExtension = this.substringBeforeLast(delimiter = ".")
    val imageExtensions = listOf(
        "png",
        "jpg",
        "jpeg",
    )
    for (imageExtension in imageExtensions) {
        val file = File("$filePathWithoutExtension.$imageExtension")
        if (file.exists()) return file
    }
    return null
}

fun File.isSubtitle(): Boolean {
    val subtitleExtensions = listOf(
        "srt",
        "ssa",
        "ass",
        "vtt",
        "ttml",
    )
    return extension.lowercase() in subtitleExtensions
}

fun File.deleteFiles() {
    try {
        listFiles()?.onEach { it.delete() }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

val File.prettyName: String
    get() = name.takeIf { parent != "/storage/emulated" } ?: "Internal Storage"
