package com.github.livingwithhippos.unchained.plugins.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import com.github.livingwithhippos.unchained.data.model.cache.CachedTorrent
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
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
    var isCached: Boolean = false,
) : Parcelable