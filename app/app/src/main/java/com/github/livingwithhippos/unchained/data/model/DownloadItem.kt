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
data class DownloadItem(
    @Json(name = "id")
    val id: String,
    @Json(name = "filename")
    val filename: String,
    @Json(name = "mimeType")
    val mimeType: String?,
    @Json(name = "filesize")
    val fileSize: Long,
    @Json(name = "link")
    val link: String,
    @Json(name = "host")
    val host: String,
    @Json(name = "host_icon")
    val hostIcon: String?,
    @Json(name = "chunks")
    val chunks: Int,
    @Json(name = "crc")
    val crc: Int?,
    @Json(name = "download")
    val download: String,
    @Json(name = "streamable")
    val streamable: Int?,
    @Json(name = "generated")
    val generated: String?,
    @Json(name = "type")
    val type: String?,
    @Json(name = "alternative")
    val alternative: List<Alternative>?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readLong(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString(),
        parcel.readInt(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString()!!,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString(),
        parcel.readString(),
        mutableListOf<Alternative>().also { parcel.readTypedList(it, Alternative.CREATOR) }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(filename)
        parcel.writeString(mimeType)
        parcel.writeLong(fileSize)
        parcel.writeString(link)
        parcel.writeString(host)
        parcel.writeString(hostIcon)
        parcel.writeInt(chunks)
        parcel.writeValue(crc)
        parcel.writeString(download)
        parcel.writeValue(streamable)
        parcel.writeString(generated)
        parcel.writeString(type)
        parcel.writeTypedList(alternative)
    }

    override fun describeContents(): Int {
        return id.hashCode()
    }

    companion object CREATOR : Parcelable.Creator<DownloadItem> {
        override fun createFromParcel(parcel: Parcel): DownloadItem {
            return DownloadItem(parcel)
        }

        override fun newArray(size: Int): Array<DownloadItem?> {
            return arrayOfNulls(size)
        }
    }
}

@JsonClass(generateAdapter = true)
data class Alternative(
    @Json(name = "id")
    val id: String,
    @Json(name = "filename")
    val filename: String,
    @Json(name = "download")
    val download: String,
    @Json(name = "mimeType")
    val mimeType: String?,
    @Json(name = "quality")
    val quality: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(filename)
        parcel.writeString(download)
        parcel.writeString(mimeType)
        parcel.writeString(quality)
    }

    override fun describeContents(): Int {
        return id.hashCode()
    }

    companion object CREATOR : Parcelable.Creator<Alternative> {
        override fun createFromParcel(parcel: Parcel): Alternative {
            return Alternative(parcel)
        }

        override fun newArray(size: Int): Array<Alternative?> {
            return arrayOfNulls(size)
        }
    }
}
