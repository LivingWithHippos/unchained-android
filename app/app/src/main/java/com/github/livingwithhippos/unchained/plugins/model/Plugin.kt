package com.github.livingwithhippos.unchained.plugins.model

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class Plugin(
    @Json(name = "engine_version")
    val engineVersion: Double,
    @Json(name = "version")
    val version: Double,
    @Json(name = "url")
    val url: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "description")
    val description: String?,
    @Json(name = "author")
    val author: String?,
    @Json(name = "supported_categories")
    val supportedCategories: SupportedCategories,
    @Json(name = "search")
    val search: PluginSearch,
    @Json(name = "download")
    val download: PluginDownload
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString(),
        parcel.readString(),
        parcel.readParcelable(SupportedCategories::class.java.classLoader)!!,
        parcel.readParcelable(PluginSearch::class.java.classLoader)!!,
        parcel.readParcelable(PluginDownload::class.java.classLoader)!!
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(engineVersion)
        parcel.writeDouble(version)
        parcel.writeString(url)
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeString(author)
        parcel.writeParcelable(supportedCategories, flags)
        parcel.writeParcelable(search, flags)
        parcel.writeParcelable(download, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Plugin> {
        override fun createFromParcel(parcel: Parcel): Plugin {
            return Plugin(parcel)
        }

        override fun newArray(size: Int): Array<Plugin?> {
            return arrayOfNulls(size)
        }
    }
}

@JsonClass(generateAdapter = true)
data class SupportedCategories(
    @Json(name = "all")
    val all: String,
    @Json(name = "anime")
    val anime: String?,
    @Json(name = "software")
    val software: String?,
    @Json(name = "games")
    val games: String?,
    @Json(name = "movies")
    val movies: String?,
    @Json(name = "music")
    val music: String?,
    @Json(name = "tv")
    val tv: String?,
    @Json(name = "books")
    val books: String?
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(all)
        parcel.writeString(anime)
        parcel.writeString(software)
        parcel.writeString(games)
        parcel.writeString(movies)
        parcel.writeString(music)
        parcel.writeString(tv)
        parcel.writeString(books)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SupportedCategories> {
        override fun createFromParcel(parcel: Parcel): SupportedCategories {
            return SupportedCategories(parcel)
        }

        override fun newArray(size: Int): Array<SupportedCategories?> {
            return arrayOfNulls(size)
        }
    }
}

@JsonClass(generateAdapter = true)
data class PluginSearch(
    @Json(name = "category")
    val urlCategory: String?,
    @Json(name = "no_category")
    val urlNoCategory: String,
    @Json(name = "page_start")
    val pageStart: Int? = 1
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readValue(Int::class.java.classLoader) as? Int
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(urlCategory)
        parcel.writeString(urlNoCategory)
        parcel.writeValue(pageStart)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PluginSearch> {
        override fun createFromParcel(parcel: Parcel): PluginSearch {
            return PluginSearch(parcel)
        }

        override fun newArray(size: Int): Array<PluginSearch?> {
            return arrayOfNulls(size)
        }
    }
}

@JsonClass(generateAdapter = true)
data class PluginDownload(
    @Json(name = "magnet")
    val magnet: CustomRegex?,
    @Json(name = "torrent")
    val torrent: List<CustomRegex>?,
    @Json(name = "internal")
    val internalLink: InternalLink?,
    @Json(name = "table_direct")
    val tableLink: TableLink?
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(CustomRegex::class.java.classLoader),
        parcel.createTypedArrayList(CustomRegex),
        parcel.readParcelable(InternalLink::class.java.classLoader),
        parcel.readParcelable(TableLink::class.java.classLoader)
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(magnet, flags)
        parcel.writeTypedList(torrent)
        parcel.writeParcelable(internalLink, flags)
        parcel.writeParcelable(tableLink, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PluginDownload> {
        override fun createFromParcel(parcel: Parcel): PluginDownload {
            return PluginDownload(parcel)
        }

        override fun newArray(size: Int): Array<PluginDownload?> {
            return arrayOfNulls(size)
        }
    }
}

@JsonClass(generateAdapter = true)
data class CustomRegex(
    @Json(name = "regex")
    val regex: String,
    @Json(name = "group")
    val group: Int = 1,
    @Json(name = "slug_type")
    val slugType: String,
    @Json(name = "other")
    val other: String?
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(regex)
        parcel.writeInt(group)
        parcel.writeString(slugType)
        parcel.writeString(other)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CustomRegex> {
        override fun createFromParcel(parcel: Parcel): CustomRegex {
            return CustomRegex(parcel)
        }

        override fun newArray(size: Int): Array<CustomRegex?> {
            return arrayOfNulls(size)
        }
    }
}

@JsonClass(generateAdapter = true)
data class InternalLink(
    @Json(name = "name")
    val name: CustomRegex,
    @Json(name = "link")
    val link: CustomRegex
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(CustomRegex::class.java.classLoader)!!,
        parcel.readParcelable(CustomRegex::class.java.classLoader)!!
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(name, flags)
        parcel.writeParcelable(link, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<InternalLink> {
        override fun createFromParcel(parcel: Parcel): InternalLink {
            return InternalLink(parcel)
        }

        override fun newArray(size: Int): Array<InternalLink?> {
            return arrayOfNulls(size)
        }
    }
}


@JsonClass(generateAdapter = true)
data class TableLink(
    @Json(name = "class")
    val className: String?,
    @Json(name = "id")
    val idName: String?,
    @Json(name = "name_column")
    val nameColumn: Int,
    @Json(name = "name_regex")
    val nameRegex: CustomRegex,
    @Json(name = "seeders_column")
    val seedersColumn: Int?,
    @Json(name = "seeders_regex")
    val seedersRegex: CustomRegex?,
    @Json(name = "leechers_column")
    val leechersColumn: Int?,
    @Json(name = "leechers_regex")
    val leechersRegex: CustomRegex?,
    @Json(name = "size_column")
    val sizeColumn: Int?,
    @Json(name = "size_regex")
    val sizeRegex: CustomRegex?,
    @Json(name = "magnet_column")
    val magnetColumn: Int?,
    @Json(name = "magnet_regex")
    val magnetRegex: CustomRegex?,
    @Json(name = "torrent_column")
    val torrentColumn: Int?,
    @Json(name = "torrent_regex")
    val torrentRegex: CustomRegex?,
    @Json(name = "details_column")
    val detailsColumn: Int?,
    @Json(name = "details_regex")
    val detailsRegex: CustomRegex?
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readParcelable(CustomRegex::class.java.classLoader)!!,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readParcelable(CustomRegex::class.java.classLoader),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readParcelable(CustomRegex::class.java.classLoader),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readParcelable(CustomRegex::class.java.classLoader),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readParcelable(CustomRegex::class.java.classLoader),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readParcelable(CustomRegex::class.java.classLoader),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readParcelable(CustomRegex::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(className)
        parcel.writeString(idName)
        parcel.writeInt(nameColumn)
        parcel.writeParcelable(nameRegex, flags)
        parcel.writeValue(seedersColumn)
        parcel.writeParcelable(seedersRegex, flags)
        parcel.writeValue(leechersColumn)
        parcel.writeParcelable(leechersRegex, flags)
        parcel.writeValue(sizeColumn)
        parcel.writeParcelable(sizeRegex, flags)
        parcel.writeValue(magnetColumn)
        parcel.writeParcelable(magnetRegex, flags)
        parcel.writeValue(torrentColumn)
        parcel.writeParcelable(torrentRegex, flags)
        parcel.writeValue(detailsColumn)
        parcel.writeParcelable(detailsRegex, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TableLink> {
        override fun createFromParcel(parcel: Parcel): TableLink {
            return TableLink(parcel)
        }

        override fun newArray(size: Int): Array<TableLink?> {
            return arrayOfNulls(size)
        }
    }
}
