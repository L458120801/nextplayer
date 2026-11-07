package dev.anilbeesetti.nextplayer.feature.player.extensions

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import dev.anilbeesetti.nextplayer.core.common.extensions.convertToUTF8
import dev.anilbeesetti.nextplayer.core.common.extensions.getFilenameFromUri
import dev.anilbeesetti.nextplayer.core.common.extensions.getInitialDirectoryUri
import dev.anilbeesetti.nextplayer.core.common.extensions.getLocalSubtitles
import dev.anilbeesetti.nextplayer.core.common.extensions.getPath
import dev.anilbeesetti.nextplayer.core.common.extensions.isSubtitleFor
import java.io.File
import java.nio.charset.Charset
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Uri.getSubtitleMime(): String = when {
    path?.endsWith(suffix = ".ssa") == true || path?.endsWith(suffix = ".ass") == true -> {
        MimeTypes.TEXT_SSA
    }

    path?.endsWith(suffix = ".vtt") == true -> {
        MimeTypes.TEXT_VTT
    }

    path?.endsWith(suffix = ".ttml") == true || path?.endsWith(suffix = ".xml") == true ||
        path?.endsWith(suffix = ".dfxp") == true -> {
        MimeTypes.APPLICATION_TTML
    }

    else -> {
        MimeTypes.APPLICATION_SUBRIP
    }
}

val Uri.isSchemaContent: Boolean
    get() = ContentResolver.SCHEME_CONTENT.equals(
        other = scheme,
        ignoreCase = true,
    )

suspend fun Context.uriToSubtitleConfiguration(
    uri: Uri,
    subtitleEncoding: String = "",
    isSelected: Boolean = false,
): MediaItem.SubtitleConfiguration {
    val charset = if (subtitleEncoding.isNotEmpty() && Charset.isSupported(subtitleEncoding)) {
        Charset.forName(subtitleEncoding)
    } else {
        null
    }
    val label = getFilenameFromUri(uri = uri)
    val mimeType = uri.getSubtitleMime()
    val utf8ConvertedUri = convertToUTF8(
        uri = uri,
        charset = charset,
    )
    return MediaItem.SubtitleConfiguration.Builder(utf8ConvertedUri).apply {
        setId(uri.toString())
        setMimeType(mimeType)
        setLabel(label)
        if (isSelected) setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
    }.build()
}

internal suspend fun Context.getLocalSubtitleConfigurations(
    videoUri: Uri,
    subtitleEncoding: String,
    excludeSubsList: List<Uri>,
    autoSelectFirst: Boolean,
): List<MediaItem.SubtitleConfiguration> {
    val subtitles = getSubtitleTreeUriFor(videoUri)?.let { treeUri ->
        getSubtitlesFromTree(
            videoUri = videoUri,
            treeUri = treeUri,
            excludeSubsList = excludeSubsList,
        )
    } ?: run {
        val videoPath = getPath(uri = videoUri) ?: return emptyList()
        File(videoPath).getLocalSubtitles(
            context = this,
            excludeSubsList = excludeSubsList,
        )
    }

    return subtitles.mapIndexed { index, subtitleUri ->
        uriToSubtitleConfiguration(
            uri = subtitleUri,
            subtitleEncoding = subtitleEncoding,
            isSelected = autoSelectFirst && index == 0,
        )
    }
}

internal fun Context.getSubtitleTreeUriFor(videoUri: Uri): Uri? {
    val persistedTreeUris = contentResolver.persistedUriPermissions
        .asSequence()
        .filter { permission ->
            permission.isReadPermission && DocumentsContract.isTreeUri(permission.uri)
        }
        .map { it.uri }
        .toList()
    val preferenceKey = subtitleDirectoryPreferenceKey(videoUri)
    val rememberedTreeUri = preferenceKey?.let { key ->
        getSharedPreferences(
            SUBTITLE_ACCESS_PREFERENCES,
            Context.MODE_PRIVATE,
        ).getString(
            key,
            null,
        )
            ?.toUri()
    }

    return rememberedTreeUri?.takeIf { treeUri ->
        treeUri in persistedTreeUris
    } ?: persistedTreeUris.firstOrNull { treeUri ->
        isSubtitleTreeFor(
            videoUri,
            treeUri,
        )
    }
}

internal fun Context.rememberSubtitleTreeUriFor(videoUri: Uri, treeUri: Uri): Boolean {
    val hasPersistedReadAccess = contentResolver.persistedUriPermissions.any { permission ->
        permission.uri == treeUri && permission.isReadPermission
    }
    if (!hasPersistedReadAccess) return false
    val key = subtitleDirectoryPreferenceKey(videoUri) ?: return false
    getSharedPreferences(
        SUBTITLE_ACCESS_PREFERENCES,
        Context.MODE_PRIVATE,
    ).edit()
        .putString(
            key,
            treeUri.toString(),
        )
        .apply()
    return true
}

private fun Context.subtitleDirectoryPreferenceKey(videoUri: Uri): String? {
    val videoPath = getPath(uri = videoUri) ?: return null
    return File(videoPath).absoluteFile.normalize().parentFile?.path
}

private const val SUBTITLE_ACCESS_PREFERENCES = "subtitle_directory_access"

internal fun Context.isSubtitleTreeFor(videoUri: Uri, treeUri: Uri): Boolean {
    val videoDirectoryUri = getInitialDirectoryUri(fileUri = videoUri) ?: return false
    if (treeUri.authority != videoDirectoryUri.authority || !DocumentsContract.isTreeUri(treeUri)) {
        return false
    }
    val targetDocumentId = runCatching {
        DocumentsContract.getDocumentId(videoDirectoryUri)
    }.getOrNull() ?: return false
    val treeDocumentId = runCatching {
        DocumentsContract.getTreeDocumentId(treeUri)
    }.getOrNull() ?: return false
    return relativeDocumentPath(
        treeDocumentId = treeDocumentId,
        targetDocumentId = targetDocumentId,
    ) != null
}

internal fun relativeDocumentPath(
    treeDocumentId: String,
    targetDocumentId: String,
): List<String>? {
    val treeVolume = treeDocumentId.substringBefore(
        delimiter = ':',
        missingDelimiterValue = "",
    )
    val targetVolume = targetDocumentId.substringBefore(
        delimiter = ':',
        missingDelimiterValue = "",
    )
    if (treeVolume.isEmpty() || treeVolume != targetVolume) return null

    val treePath = treeDocumentId.substringAfter(delimiter = ':').trim('/')
    val targetPath = targetDocumentId.substringAfter(delimiter = ':').trim('/')
    val relativePath = when {
        treePath.isEmpty() -> targetPath
        targetPath == treePath -> ""
        targetPath.startsWith(prefix = "$treePath/") -> targetPath.removePrefix("$treePath/")
        else -> return null
    }
    return relativePath.split('/').filter { it.isNotEmpty() }
}

private data class TreeDocument(
    val documentId: String,
    val displayName: String,
    val mimeType: String,
)

private suspend fun Context.getSubtitlesFromTree(
    videoUri: Uri,
    treeUri: Uri,
    excludeSubsList: List<Uri>,
): List<Uri> = withContext(Dispatchers.IO) {
    val videoPath = getPath(uri = videoUri) ?: return@withContext emptyList()
    var currentDocumentId = runCatching {
        DocumentsContract.getTreeDocumentId(treeUri)
    }.getOrNull() ?: return@withContext emptyList()
    var currentDocuments = queryTreeDocuments(
        treeUri = treeUri,
        parentDocumentId = currentDocumentId,
    )
    val videoFile = File(videoPath)
    val videoIsDirectChild = currentDocuments.any { it.displayName == videoFile.name }

    if (!videoIsDirectChild) {
        val videoDirectoryUri = getInitialDirectoryUri(fileUri = videoUri)
            ?: return@withContext emptyList()
        val targetDocumentId = runCatching {
            DocumentsContract.getDocumentId(videoDirectoryUri)
        }.getOrNull() ?: return@withContext emptyList()
        val relativePath = relativeDocumentPath(
            treeDocumentId = currentDocumentId,
            targetDocumentId = targetDocumentId,
        ) ?: return@withContext emptyList()

        for (directoryName in relativePath) {
            val directory = currentDocuments.firstOrNull {
                it.displayName == directoryName &&
                    it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR
            } ?: return@withContext emptyList()
            currentDocumentId = directory.documentId
            currentDocuments = queryTreeDocuments(
                treeUri = treeUri,
                parentDocumentId = currentDocumentId,
            )
        }
    }

    val excludedUris = excludeSubsList.toSet()
    val excludedPaths = excludeSubsList.mapNotNull { getPath(uri = it) }.toSet()
    val mediaName = videoFile.nameWithoutExtension
    currentDocuments.asSequence()
        .filter { document ->
            document.mimeType != DocumentsContract.Document.MIME_TYPE_DIR &&
                File(document.displayName).isSubtitleFor(mediaName)
        }
        .sortedBy { it.displayName.lowercase(Locale.ROOT) }
        .map { document ->
            DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                document.documentId,
            )
        }
        .filter { subtitleUri ->
            subtitleUri !in excludedUris && getPath(uri = subtitleUri) !in excludedPaths
        }
        .toList()
}

private fun Context.queryTreeDocuments(
    treeUri: Uri,
    parentDocumentId: String,
): List<TreeDocument> {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
        treeUri,
        parentDocumentId,
    )
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
    )
    return try {
        contentResolver.query(
            childrenUri,
            projection,
            null,
            null,
            null,
        )?.use { cursor -> cursor.toTreeDocuments() }.orEmpty()
    } catch (_: Exception) {
        emptyList()
    }
}

private fun Cursor.toTreeDocuments(): List<TreeDocument> {
    val documentIdIndex = getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
    val displayNameIndex = getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
    val mimeTypeIndex = getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
    if (documentIdIndex < 0 || displayNameIndex < 0 || mimeTypeIndex < 0) return emptyList()

    return buildList {
        while (moveToNext()) {
            val documentId = getString(documentIdIndex) ?: continue
            val displayName = getString(displayNameIndex) ?: continue
            val mimeType = getString(mimeTypeIndex) ?: continue
            add(
                TreeDocument(
                    documentId = documentId,
                    displayName = displayName,
                    mimeType = mimeType,
                ),
            )
        }
    }
}

@Suppress("DEPRECATION")
fun Bundle.getParcelableUriArray(key: String): ArrayList<out Parcelable>? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayList(
            key,
            Uri::class.java,
        )
    } else {
        getParcelableArrayList(key)
    }
