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
    "mimeType": "string", // Mime Type of the file, guessed by the file extension
    "filesize": int, // bytes, 0 if unknown
    "link": "string", // Original link
    "host": "string", // Host main domain
    "chunks": int, // Max Chunks allowed
    "download": "string", // Generated link
    "generated": "string" // jsonDate
},
{
    "id": "string",
    "filename": "string",
    "mimeType": "string",
    "filesize": int,
    "link": "string",
    "host": "string",
    "chunks": int,
    "download": "string",
    "generated": "string",
    "type": "string" // Type of the file (in general, its quality)
}
]
*/

@JsonClass(generateAdapter = true)
@Parcelize
data class DownloadItem(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "filename") val filename: String,
    /** Mime Type of the file, guessed by the file extension */
    @param:Json(name = "mimeType") val mimeType: String?,
    /** bytes, 0 if unknown */
    @param:Json(name = "filesize") val fileSize: Long,
    /** Original link, use "generated" to download this file */
    @param:Json(name = "link") val link: String,
    /** Host main domain */
    @param:Json(name = "host") val host: String,
    @param:Json(name = "host_icon") val hostIcon: String?,
    /** Max Chunks allowed */
    @param:Json(name = "chunks") val chunks: Int,
    @param:Json(name = "crc") val crc: Int?,
    /** Generated link to be used for downloading */
    @param:Json(name = "download") val download: String,
    @param:Json(name = "streamable") val streamable: Int?,
    /** jsonDate */
    @param:Json(name = "generated") val generated: String?,
    @param:Json(name = "type") val type: String?,
    @param:Json(name = "alternative") val alternative: List<Alternative>?,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class Alternative(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "filename") val filename: String,
    @param:Json(name = "download") val download: String,
    @param:Json(name = "mimeType") val mimeType: String?,
    @param:Json(name = "quality") val quality: String?,
) : Parcelable
