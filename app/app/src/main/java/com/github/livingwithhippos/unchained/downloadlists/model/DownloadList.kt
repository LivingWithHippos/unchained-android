package com.github.livingwithhippos.unchained.downloadlists.model
import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.JsonClass

import com.squareup.moshi.Json
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

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
data class DownloadList(
    val downloads: List<DownloadItem>
)

@JsonClass(generateAdapter = true)
data class DownloadItem(
    @Json(name = "id")
    val id: String,
    @Json(name = "filename")
    val filename: String,
    @Json(name = "mimeType")
    val mimeType: String?,
    @Json(name = "filesize")
    val filesize: Long,
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
    val type: String?
): Parcelable {
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
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(filename)
        parcel.writeString(mimeType)
        parcel.writeLong(filesize)
        parcel.writeString(link)
        parcel.writeString(host)
        parcel.writeString(hostIcon)
        parcel.writeInt(chunks)
        parcel.writeValue(crc)
        parcel.writeString(download)
        parcel.writeValue(streamable)
        parcel.writeString(generated)
        parcel.writeString(type)
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
//todo: check real return format
//todo: check if page starts from 0 or 1

interface DownloadsApi {

    /**
     * Get user downloads list
     * You can not use both offset and page at the same time, page is prioritized in case it happens.
     * @param token the authorization token, formed as "Bearer api_token"
     * @param offset Starting offset (must be within 0 and X-Total-Count HTTP header)
     * @param page Page number
     * @param limit Entries returned per page / request (must be within 0 and 100, default: 50)
     * @return a Response<List<DownloadItem>> a list of download items
     */
    @GET(" downloads")
    suspend fun getDownloads(
        @Header("Authorization") token: String,
        @Query("offset") offset: Int,
        @Query("page") page: Int,
        @Query("limit") limit: Int = 10
    ): Response<List<DownloadItem>>

    @GET(" downloads")
    suspend fun getDownloadsList(
        @Header("Authorization") token: String,
        @Query("offset") offset: Int,
        @Query("page") page: Int,
        @Query("limit") limit: Int = 10
    ): Response<DownloadList>
}