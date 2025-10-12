package com.github.livingwithhippos.unchained.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

/*
[
{
    "id": "string",
    "filename": "string",
    "original_filename": "string", // Original name of the torrent
    "hash": "string", // SHA1 Hash of the torrent
    "bytes": int, // Size of selected files only
    "original_bytes": int, // Total size of the torrent
    "host": "string", // Host main domain
    "split": int, // Split size of links
    "progress": int, // Possible values: 0 to 100
    "status": "downloaded", // Current status of the torrent: magnet_error, magnet_conversion, waiting_files_selection, queued, downloading, downloaded, error, virus, compressing, uploading, dead
    "added": "string", // jsonDate
    "files": [
    {
        "id": int,
        "path": "string", // Path to the file inside the torrent, starting with "/"
        "bytes": int,
        "selected": int // 0 or 1
    },
    {
        "id": int,
        "path": "string", // Path to the file inside the torrent, starting with "/"
        "bytes": int,
        "selected": int // 0 or 1
    }
    ],
    "links": [
    "string" // Host URL
    ],
    "ended": "string", // !! Only present when finished, jsonDate
    "speed": int, // !! Only present in "downloading", "compressing", "uploading" status
    "seeders": int // !! Only present in "downloading", "magnet_conversion" status
}
]
*/

/**
 * Torrent item: this class is used for both the /torrents and /torrents/info/{id} endpoint even if
 * the returned item are different. For example the info version returns the original file size and
 * the selected files one, while the /torrents one returns only the original file size (even if the
 * docs say otherwise)
 *
 * @property id
 * @property filename
 * @property originalFilename
 * @property hash
 * @property bytes
 * @property originalBytes
 * @property host
 * @property split
 * @property progress
 * @property status
 * @property added
 * @property files
 * @property links
 * @property ended
 * @property speed
 * @property seeders
 * @constructor Create empty Torrent item
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class TorrentItem(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "filename") val filename: String,
    @param:Json(name = "original_filename") val originalFilename: String?,
    @param:Json(name = "hash") val hash: String,
    @param:Json(name = "bytes") val bytes: Long,
    @param:Json(name = "original_bytes") val originalBytes: Long?,
    @param:Json(name = "host") val host: String,
    @param:Json(name = "split") val split: Int,
    @param:Json(name = "progress") val progress: Float,
    @param:Json(name = "status") val status: String,
    @param:Json(name = "added") val added: String,
    @param:Json(name = "files") val files: List<InnerTorrentFile>?,
    @param:Json(name = "links") val links: List<String>,
    @param:Json(name = "ended") val ended: String?,
    @param:Json(name = "speed") val speed: Int?,
    @param:Json(name = "seeders") val seeders: Int?,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class InnerTorrentFile(
    @param:Json(name = "id") val id: Int,
    @param:Json(name = "path") val path: String,
    @param:Json(name = "bytes") val bytes: Long,
    @param:Json(name = "selected") val selected: Int,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class UploadedTorrent(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "uri") val uri: String,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class AvailableHost(
    @param:Json(name = "host") val host: String,
    @param:Json(name = "max_file_size") val maxFileSize: Int,
) : Parcelable
