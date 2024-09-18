package com.github.livingwithhippos.unchained.utilities

import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.github.livingwithhippos.unchained.data.repository.PluginRepository.Companion.TYPE_UNCHAINED
import java.util.regex.Matcher
import java.util.regex.Pattern


fun parseUriType(uri: Uri, context: Context): UriType {
    when (uri.scheme) {
        SCHEME_MAGNET -> {
            // todo: split and check multiple magnets
            return UriType.MagnetLink(uri.toString())
        }

        SCHEME_CONTENT,
        SCHEME_FILE -> {

            val name = getUriFilename(uri, context)

            if (name != null) {
                if (name.endsWith(TYPE_UNCHAINED, ignoreCase = true)) {
                    return UriType.PluginFile(uri)
                }

                if (name.endsWith(".torrent", ignoreCase = true)) {
                    return UriType.TorrentFile(uri)
                }

                if (CONTAINER_EXTENSION_PATTERN.toRegex().matches(name)) {
                    return UriType.ContainerFile(uri)
                }
            }

            val uriPath = uri.path

            if (uriPath != null) {

                if (uriPath.endsWith(TYPE_UNCHAINED, ignoreCase = true)) {
                    return UriType.PluginFile(uri)
                }

                if (uriPath.endsWith(".torrent", ignoreCase = true)) {
                    return UriType.TorrentFile(uri)
                }

                if (CONTAINER_EXTENSION_PATTERN.toRegex().matches(uriPath)) {
                    return UriType.ContainerFile(uri)
                }

            }

            return UriType.Unknown
        }

        SCHEME_HTTP,
        SCHEME_HTTPS -> {
            // todo: split and check multiple links

            // check if it's a torrent file link or a container file link
            val name = getUriFilename(uri, context)

            if (name != null) {

                if (name.endsWith(".torrent", ignoreCase = true)) {
                    return UriType.TorrentLink(uri.toString())
                }

                if (CONTAINER_EXTENSION_PATTERN.toRegex().matches(name)) {
                    return UriType.ContainerLink(uri.toString())
                }
            }

            val uriPath = uri.path

            if (uriPath != null) {

                if (uriPath.endsWith(TYPE_UNCHAINED, ignoreCase = true)) {
                    return UriType.PluginLink(uri.toString())
                }

                if (uriPath.endsWith(".torrent", ignoreCase = true)) {
                    return UriType.TorrentLink(uri.toString())
                }

                if (CONTAINER_EXTENSION_PATTERN.toRegex().matches(uriPath)) {
                    return UriType.ContainerLink(uri.toString())
                }

            }

            return UriType.Url(uri.toString())
        }
    }

    return UriType.Unknown
}

fun getUriFilename(uri: Uri, context: Context): String? {
    context.contentResolver.query(
        uri,
        arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val displayName = cursor.getString(0)
            return displayName
        }
    }

    return null
}

sealed class UriType {
    data class Url(val link: String) : UriType()
    data class PluginFile(val uri: Uri) : UriType()
    data class PluginLink(val link: String) : UriType()
    data class MagnetLink(val link: String) : UriType()
    data class TorrentFile(val uri: Uri) : UriType()
    data class TorrentLink(val link: String) : UriType()
    data class ContainerFile(val uri: Uri) : UriType()
    data class ContainerLink(val link: String) : UriType()
    data object Unknown : UriType()
}

val torrentNamePattern: Pattern? = Pattern.compile("/([^/]+\\.torrent)\$")

fun getTorrentNameFromLink(link: String): String? = torrentNamePattern?.matcher(link)?.group(1)
