package com.github.livingwithhippos.unchained.data.model

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

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

@JsonClass(generateAdapter = true)
data class TorrentItem(
    @Json(name = "id")
    val id: String,
    @Json(name = "filename")
    val filename: String,
    @Json(name = "original_filename")
    val originalFilename: String?,
    @Json(name = "hash")
    val hash: String,
    @Json(name = "bytes")
    val bytes: Long,
    @Json(name = "original_bytes")
    val originalBytes: Long?,
    @Json(name = "host")
    val host: String,
    @Json(name = "split")
    val split: Int,
    @Json(name = "progress")
    val progress: Int,
    @Json(name = "status")
    val status: String,
    @Json(name = "added")
    val added: String,
    @Json(name = "files")
    val files: List<InnerTorrentFile>?,
    @Json(name = "links")
    val links: List<String>,
    @Json(name = "ended")
    val ended: String?,
    @Json(name = "speed")
    val speed: Int?,
    @Json(name = "seeders")
    val seeders: Int?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString(),
        parcel.readString()!!,
        parcel.readLong(),
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.createTypedArrayList(InnerTorrentFile),
        parcel.createStringArrayList() ?: emptyList(),
        parcel.readString(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(filename)
        parcel.writeString(originalFilename)
        parcel.writeString(hash)
        parcel.writeLong(bytes)
        parcel.writeValue(originalBytes)
        parcel.writeString(host)
        parcel.writeInt(split)
        parcel.writeInt(progress)
        parcel.writeString(status)
        parcel.writeString(added)
        parcel.writeTypedList(files)
        parcel.writeStringList(links)
        parcel.writeString(ended)
        parcel.writeValue(speed)
        parcel.writeValue(seeders)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TorrentItem> {
        override fun createFromParcel(parcel: Parcel): TorrentItem {
            return TorrentItem(parcel)
        }

        override fun newArray(size: Int): Array<TorrentItem?> {
            return arrayOfNulls(size)
        }
    }
}

@JsonClass(generateAdapter = true)
data class InnerTorrentFile(
    @Json(name = "id")
    val id: Int,
    @Json(name = "path")
    val path: String,
    @Json(name = "bytes")
    val bytes: Long,
    @Json(name = "selected")
    val selected: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(path)
        parcel.writeLong(bytes)
        parcel.writeInt(selected)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<InnerTorrentFile> {
        override fun createFromParcel(parcel: Parcel): InnerTorrentFile {
            return InnerTorrentFile(parcel)
        }

        override fun newArray(size: Int): Array<InnerTorrentFile?> {
            return arrayOfNulls(size)
        }
    }
}

@JsonClass(generateAdapter = true)
data class UploadedTorrent(
    @Json(name = "id")
    val id: String,
    @Json(name = "uri")
    val uri: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(uri)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UploadedTorrent> {
        override fun createFromParcel(parcel: Parcel): UploadedTorrent {
            return UploadedTorrent(parcel)
        }

        override fun newArray(size: Int): Array<UploadedTorrent?> {
            return arrayOfNulls(size)
        }
    }
}

@JsonClass(generateAdapter = true)
data class AvailableHost(
    @Json(name = "host")
    val host: String,
    @Json(name = "max_file_size")
    val maxFileSize: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(host)
        parcel.writeInt(maxFileSize)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AvailableHost> {
        override fun createFromParcel(parcel: Parcel): AvailableHost {
            return AvailableHost(parcel)
        }

        override fun newArray(size: Int): Array<AvailableHost?> {
            return arrayOfNulls(size)
        }
    }
}

@JsonClass(generateAdapter = true)
data class InstantAvailability(
    val keys: Map<String, Any?>,
)

@JsonClass(generateAdapter = true)
data class RdCache(
    val rd: List<Map<String, TorrentFile>>,
)

@JsonClass(generateAdapter = true)
data class TorrentFile(
    @Json(name = "filename")
    val fileName: String,
    @Json(name = "filesize")
    val fileSize: Long
)
