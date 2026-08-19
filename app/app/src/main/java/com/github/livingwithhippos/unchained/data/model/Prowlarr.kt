package com.github.livingwithhippos.unchained.data.model

import android.content.Context
import com.github.livingwithhippos.unchained.plugins.model.ScrapedItem
import com.github.livingwithhippos.unchained.utilities.HASH_PATTERN
import com.github.livingwithhippos.unchained.utilities.MAGNET_PATTERN
import com.github.livingwithhippos.unchained.utilities.extension.getFileSizeString
import java.util.regex.Pattern
import kotlinx.serialization.Serializable
import timber.log.Timber

private val magnetPattern = Pattern.compile(MAGNET_PATTERN)
private val hashPattern = Regex(HASH_PATTERN)

@Serializable
data class ProwlarrResponse(
    val guid: String,
    val age: Int,
    val ageHours: Double,
    val ageMinutes: Double,
    val size: Long,
    val files: Int? = null,
    val indexerId: Int,
    val indexer: String,
    val title: String,
    val sortTitle: String,
    val imdbId: Int? = null,
    val tmdbId: Int? = null,
    val tvdbId: Int? = null,
    val tvMazeId: Int? = null,
    val publishDate: String,
    val infoUrl: String,
    val indexerFlags: List<String> = emptyList(),
    val categories: List<PCategory> = emptyList(),
    val downloadUrl: String? = null,
    val magnetUrl: String? = null,
    val infoHash: String? = null,
    val seeders: Int,
    val leechers: Int,
    val protocol: String,
    val fileName: String,
)

@Serializable
data class PCategory(
    val id: Int,
    val name: String? = null,
    val subCategories: List<PCategory> = emptyList(),
)

fun prowlarrToScrapedItems(context: Context, response: List<ProwlarrResponse>): List<ScrapedItem> {
    return response.mapNotNull {
        when (it.protocol) {
            "torrent",
            "magnet" -> {
                // todo: sometimes indexers have no magnet links, and the downloadUtl
                //  need to be followed because it returns a magnet link
                //  (and maybe some of them returns a torrent file)
                //  the user can still long click on the result and manually follow the link
                //  so we still shown it

                var magnet: String? = null
                if (magnetPattern.matcher(it.guid).lookingAt()) magnet = it.guid
                else if (it.magnetUrl != null && magnetPattern.matcher(it.magnetUrl).lookingAt())
                    magnet = it.magnetUrl
                else if (it.infoHash != null && hashPattern.matches(it.infoHash))
                    magnet = "magnet:?xt=urn:btih:${it.infoHash}"
                else if (it.magnetUrl != null) magnet = it.magnetUrl

                ScrapedItem(
                    name = it.title,
                    link = it.infoUrl,
                    size = getFileSizeString(context, it.size),
                    addedDate = it.publishDate,
                    parsedSize = it.size.toDouble(),
                    seeders = it.seeders.toString(),
                    leechers = it.leechers.toString(),
                    magnets = if (magnet != null) listOf(magnet) else emptyList(),
                    torrents = if (it.downloadUrl != null) listOf(it.downloadUrl) else emptyList(),
                    hosting = emptyList(),
                )
            }
            else -> {
                Timber.e(
                    "Don't know how to handle Prowlarr response with protocol ${it.protocol}, $it"
                )
                null
            }
        }
    }
}
