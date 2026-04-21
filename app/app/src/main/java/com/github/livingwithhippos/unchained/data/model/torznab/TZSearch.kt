package com.github.livingwithhippos.unchained.data.model.torznab

import android.content.Context
import com.github.livingwithhippos.unchained.plugins.model.ScrapedItem
import com.github.livingwithhippos.unchained.utilities.directChild
import com.github.livingwithhippos.unchained.utilities.directChildText
import com.github.livingwithhippos.unchained.utilities.directChildren
import com.github.livingwithhippos.unchained.utilities.extension.getFileSizeString
import com.github.livingwithhippos.unchained.utilities.extension.isMagnet
import com.github.livingwithhippos.unchained.utilities.extension.isTorrent
import com.github.livingwithhippos.unchained.utilities.parseCommonSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import timber.log.Timber

@Serializable data class SearchRSS(val version: String, val channel: Channel)

@Serializable
data class Channel(
    val title: String,
    val description: String,
    val link: String,
    val language: String,
    val category: String,
    @SerialName("item") val items: List<Item>,
)

@Serializable
data class Item(
    val title: String,
    val guid: String,
    val type: String,
    val comments: String,
    val pubDate: String,
    val size: String,
    val description: String,
    val link: String,
    @SerialName("category") val categories: List<String>,
    val enclosure: Enclosure,
    // todo: check what happens with empty responses, nullable? default emptyList()?
    val torznabAttributes: List<TorznabAttribute> = emptyList(),
)

@Serializable data class Enclosure(val url: String, val length: String, val type: String)

@Serializable data class TorznabAttribute(val name: String, val value: String)

fun parseSearchRss(body: String): SearchRSS {
    val document = Jsoup.parse(body, "", Parser.xmlParser())
    val rss =
        document.getElementsByTag("rss").firstOrNull()
            ?: throw IllegalArgumentException("Missing rss element")
    val channel = rss.directChild("channel") ?: throw IllegalArgumentException("Missing channel")

    return SearchRSS(
        version = rss.attr("version"),
        channel =
            Channel(
                title = channel.directChildText("title"),
                description = channel.directChildText("description"),
                link = channel.directChildText("link"),
                language = channel.directChildText("language"),
                category = channel.directChildText("category"),
                items = channel.directChildren("item").map(::parseItem),
            ),
    )
}

private fun parseItem(item: Element): Item {
    val enclosure = item.directChild("enclosure")

    return Item(
        title = item.directChildText("title"),
        guid = item.directChildText("guid"),
        type = item.directChildText("type"),
        comments = item.directChildText("comments"),
        pubDate = item.directChildText("pubDate"),
        size = item.directChildText("size"),
        description = item.directChildText("description"),
        link = item.directChildText("link"),
        categories = item.directChildren("category").map(Element::text),
        enclosure =
            Enclosure(
                url = enclosure?.attr("url").orEmpty(),
                length = enclosure?.attr("length").orEmpty(),
                type = enclosure?.attr("type").orEmpty(),
            ),
        torznabAttributes =
            item.directChildren("torznab:attr").map {
                TorznabAttribute(name = it.attr("name"), value = it.attr("value"))
            },
    )
}

fun rssToScrapedItems(context: Context, rss: SearchRSS): Pair<List<ScrapedItem>, Int> {
    var errors = 0
    val scraped = mutableListOf<ScrapedItem>()
    rss.channel.items.forEach { item ->
        try {
            val sizeLong = item.size.toLongOrNull()

            var magnet: String?
            var torrent: String? = null

            magnet =
                item.torznabAttributes
                    .firstOrNull { it.name.equals("magneturl", ignoreCase = true) }
                    ?.value
            if (magnet == null) {
                if (item.link.isMagnet()) magnet = item.link
                else if (item.guid.isMagnet()) magnet = item.guid
                else if (item.enclosure.url.isMagnet()) magnet = item.enclosure.url
            }

            if (item.link.isTorrent()) torrent = item.link
            else if (item.guid.isTorrent()) torrent = item.guid
            else if (item.enclosure.url.isTorrent()) torrent = item.enclosure.url

            if (magnet != null || torrent != null) {
                scraped.add(
                    ScrapedItem(
                        name = item.title,
                        link = item.comments,
                        seeders =
                            item.torznabAttributes
                                .firstOrNull { it.name.equals("seeders", ignoreCase = true) }
                                ?.value,
                        leechers =
                            item.torznabAttributes
                                .firstOrNull { it.name.equals("leechers", ignoreCase = true) }
                                ?.value,
                        size =
                            if (sizeLong !== null) getFileSizeString(context, sizeLong)
                            else item.size,
                        addedDate = item.pubDate,
                        parsedSize = parseCommonSize(item.size),
                        // todo: add better recognition of links
                        magnets = if (magnet != null) listOf(magnet) else emptyList(),
                        torrents = if (torrent != null) listOf(torrent) else emptyList(),
                        hosting = emptyList(),
                    )
                )
            }
        } catch (ex: Exception) {
            Timber.e(ex)
            errors++
        }
    }
    return Pair(scraped, errors)
}
