package com.github.livingwithhippos.unchained.plugins.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep

@Keep
data class ScrapedItem(
    val name: String,
    val link: String?,
    val seeders: String? = null,
    val leechers: String? = null,
    val size: String? = null,
    val parsedSize: Double? = null,
    val magnets: List<String>,
    val torrents: List<String>,
    val hosting: List<String>,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readDouble(),
        parcel.createStringArrayList() ?: emptyList(),
        parcel.createStringArrayList() ?: emptyList(),
        parcel.createStringArrayList() ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(link)
        parcel.writeString(seeders)
        parcel.writeString(leechers)
        parcel.writeString(size)
        parcel.writeDouble(parsedSize ?: 0.0)
        parcel.writeStringList(magnets)
        parcel.writeStringList(torrents)
        parcel.writeStringList(hosting)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ScrapedItem> {
        override fun createFromParcel(parcel: Parcel): ScrapedItem {
            return ScrapedItem(parcel)
        }

        override fun newArray(size: Int): Array<ScrapedItem?> {
            return arrayOfNulls(size)
        }
    }
}
