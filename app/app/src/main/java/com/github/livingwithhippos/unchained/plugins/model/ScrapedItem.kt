package com.github.livingwithhippos.unchained.plugins.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class ScrapedItem(
    val name: String,
    val link: String?,
    val seeders: String? = null,
    val leechers: String? = null,
    val size: String? = null,
    val addedDate: String? = null,
    val parsedSize: Double? = null,
    val magnets: List<String>,
    val torrents: List<String>,
    val hosting: List<String>,
) : Parcelable
