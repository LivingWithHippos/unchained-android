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
) : Parcelable {
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
    )

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
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

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
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString()!!,
        parcel.readValue(Int::class.java.classLoader) as? Int
    )

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
    @Json(name = "internal")
    val internalParser: InternalParser?,
    @Json(name = "table_direct")
    val tableLink: TableParser?,
    @Json(name = "table_indirect")
    val indirectTableLink: TableParser?,
    @Json(name = "regexes")
    val regexes: PluginRegexes
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(InternalParser::class.java.classLoader),
        parcel.readParcelable(TableParser::class.java.classLoader),
        parcel.readParcelable(TableParser::class.java.classLoader),
        parcel.readParcelable(PluginRegexes::class.java.classLoader)!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(internalParser, flags)
        parcel.writeParcelable(tableLink, flags)
        parcel.writeParcelable(indirectTableLink, flags)
        parcel.writeParcelable(regexes, flags)
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
data class RegexpsGroup(
    @Json(name = "regex_use")
    val regexUse: String = "first",
    @Json(name = "regexps")
    val regexps: List<CustomRegex>
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.createTypedArrayList(CustomRegex)!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(regexUse)
        parcel.writeTypedList(regexps)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RegexpsGroup> {
        override fun createFromParcel(parcel: Parcel): RegexpsGroup {
            return RegexpsGroup(parcel)
        }

        override fun newArray(size: Int): Array<RegexpsGroup?> {
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
    val slugType: String = "complete",
    @Json(name = "other")
    val other: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString()
    )

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
data class InternalParser(
    @Json(name = "link")
    val link: RegexpsGroup
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(RegexpsGroup::class.java.classLoader)!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(link, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<InternalParser> {
        override fun createFromParcel(parcel: Parcel): InternalParser {
            return InternalParser(parcel)
        }

        override fun newArray(size: Int): Array<InternalParser?> {
            return arrayOfNulls(size)
        }
    }
}

@JsonClass(generateAdapter = true)
data class Internal(
    @Json(name = "link")
    val link: RegexpsGroup
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(RegexpsGroup::class.java.classLoader)!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(link, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Internal> {
        override fun createFromParcel(parcel: Parcel): Internal {
            return Internal(parcel)
        }

        override fun newArray(size: Int): Array<Internal?> {
            return arrayOfNulls(size)
        }
    }
}

@JsonClass(generateAdapter = true)
data class TableParser(
    @Json(name = "class")
    val className: String?,
    @Json(name = "id")
    val idName: String?,
    @Json(name = "columns")
    val columns: Columns
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readParcelable(Columns::class.java.classLoader)!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(className)
        parcel.writeString(idName)
        parcel.writeParcelable(columns, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TableParser> {
        override fun createFromParcel(parcel: Parcel): TableParser {
            return TableParser(parcel)
        }

        override fun newArray(size: Int): Array<TableParser?> {
            return arrayOfNulls(size)
        }
    }
}

@JsonClass(generateAdapter = true)
data class PluginRegexes(
    @Json(name = "name")
    val nameRegex: RegexpsGroup,
    @Json(name = "seeders")
    val seedersRegex: RegexpsGroup?,
    @Json(name = "leechers")
    val leechersRegex: RegexpsGroup?,
    @Json(name = "size")
    val sizeRegex: RegexpsGroup?,
    @Json(name = "magnet")
    val magnetRegex: RegexpsGroup?,
    @Json(name = "torrents")
    val torrentRegexes: RegexpsGroup?,
    @Json(name = "hosting")
    val hostingRegexes: RegexpsGroup?,
    @Json(name = "details")
    val detailsRegex: RegexpsGroup?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(RegexpsGroup::class.java.classLoader)!!,
        parcel.readParcelable(RegexpsGroup::class.java.classLoader),
        parcel.readParcelable(RegexpsGroup::class.java.classLoader),
        parcel.readParcelable(RegexpsGroup::class.java.classLoader),
        parcel.readParcelable(RegexpsGroup::class.java.classLoader),
        parcel.readParcelable(RegexpsGroup::class.java.classLoader),
        parcel.readParcelable(RegexpsGroup::class.java.classLoader),
        parcel.readParcelable(RegexpsGroup::class.java.classLoader)
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(nameRegex, flags)
        parcel.writeParcelable(seedersRegex, flags)
        parcel.writeParcelable(leechersRegex, flags)
        parcel.writeParcelable(sizeRegex, flags)
        parcel.writeParcelable(magnetRegex, flags)
        parcel.writeParcelable(torrentRegexes, flags)
        parcel.writeParcelable(hostingRegexes, flags)
        parcel.writeParcelable(detailsRegex, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PluginRegexes> {
        override fun createFromParcel(parcel: Parcel): PluginRegexes {
            return PluginRegexes(parcel)
        }

        override fun newArray(size: Int): Array<PluginRegexes?> {
            return arrayOfNulls(size)
        }
    }
}

@JsonClass(generateAdapter = true)
data class Columns(
    @Json(name = "name_column")
    val nameColumn: Int?,
    @Json(name = "seeders_column")
    val seedersColumn: Int?,
    @Json(name = "leechers_column")
    val leechersColumn: Int?,
    @Json(name = "size_column")
    val sizeColumn: Int?,
    @Json(name = "magnet_column")
    val magnetColumn: Int?,
    @Json(name = "torrent_column")
    val torrentColumn: Int?,
    @Json(name = "details_column")
    val detailsColumn: Int?,
    @Json(name = "hosting_column")
    val hostingColumn: Int?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(nameColumn)
        parcel.writeValue(seedersColumn)
        parcel.writeValue(leechersColumn)
        parcel.writeValue(sizeColumn)
        parcel.writeValue(magnetColumn)
        parcel.writeValue(torrentColumn)
        parcel.writeValue(detailsColumn)
        parcel.writeValue(hostingColumn)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Columns> {
        override fun createFromParcel(parcel: Parcel): Columns {
            return Columns(parcel)
        }

        override fun newArray(size: Int): Array<Columns?> {
            return arrayOfNulls(size)
        }
    }
}
