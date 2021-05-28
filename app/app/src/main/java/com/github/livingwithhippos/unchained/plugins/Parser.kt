package com.github.livingwithhippos.unchained.plugins

import android.os.Parcel
import android.os.Parcelable
import android.text.Spanned
import androidx.core.text.HtmlCompat
import com.github.livingwithhippos.unchained.plugins.model.CustomRegex
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.plugins.model.PluginRegexes
import com.github.livingwithhippos.unchained.plugins.model.TableDirect
import com.github.livingwithhippos.unchained.utilities.extension.removeWebFormatting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import okhttp3.dnsoverhttps.DnsOverHttps
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import timber.log.Timber
import java.net.SocketTimeoutException

class Parser(
    val dohClient: DnsOverHttps
) {

    private fun isPluginSupported(plugin: Plugin): Boolean {
        return plugin.engineVersion.toInt() == PLUGIN_ENGINE_VERSION.toInt()
    }

    fun completeSearch(plugin: Plugin, query: String, category: String? = null, page: Int = 1) =
        flow {
            if (query.isBlank())
                emit(ParserResult.MissingQuery)
            else {
                // todo: format queries with other unsupported web characters
                // todo: check if this works with other plugins, otherwise add it as a json parameter. Possible alternative: %20
                val currentQuery = query.trim().replace("\\s+".toRegex(), "+")
                if (!isPluginSupported(plugin)) {
                    emit(ParserResult.PluginVersionUnsupported)
                } else {
                    val currentCategory =
                        if (category.isNullOrBlank()) null else getCategory(plugin, category)

                    val queryUrl = replaceData(
                        oldUrl = if (currentCategory == null) plugin.search.urlNoCategory else plugin.search.urlCategory!!,
                        url = plugin.url,
                        query = currentQuery,
                        category = currentCategory,
                        page = page
                    )

                    emit(ParserResult.SearchStarted(-1))
                    val source = getSource(queryUrl)
                    if (source.length < 10)
                        emit(ParserResult.NetworkBodyError)
                    else {
                        /**
                         * Parsing data with the internal link mechanism
                         */
                        when {
                            plugin.download.internalLink != null -> {
                                emit(ParserResult.SearchStarted(-1))
                                val innerSource: List<String> =
                                    parseList(plugin.download.internalLink.link, source, plugin.url)
                                emit(ParserResult.SearchStarted(innerSource.size))
                                if (innerSource.isNotEmpty()) {
                                    for (link in innerSource) {
                                        // parse every page linked to the results
                                        val s = getSource(link)
                                        val name = cleanName(
                                            parseSingle(
                                                plugin.download.regexes.nameRegex,
                                                s,
                                                plugin.url
                                            ) ?: ""
                                        )
                                        // parse magnets
                                        val magnets =
                                            if (plugin.download.regexes.magnetRegex != null)
                                                parseList(
                                                    plugin.download.regexes.magnetRegex,
                                                    s,
                                                    plugin.url
                                                ).map { it.removeWebFormatting() }
                                            else
                                                emptyList()
                                        // parse torrents
                                        val torrents = mutableListOf<String>()
                                        if (plugin.download.regexes.torrentRegexes != null) {
                                            torrents.addAll(
                                                parseList(
                                                    plugin.download.regexes.torrentRegexes,
                                                    s,
                                                    plugin.url
                                                )
                                            )
                                        }

                                        // emit results once at time to avoid updating the list all at once
                                        emit(
                                            ParserResult.SingleResult(
                                                ScrapedItem(
                                                    name = name,
                                                    link = link,
                                                    magnets = magnets,
                                                    torrents = torrents
                                                )
                                            )
                                        )
                                    }
                                    emit(ParserResult.SearchFinished)
                                } else {
                                    emit(ParserResult.EmptyInnerLinksError)
                                }
                            }
                            plugin.download.tableLink != null -> {
                                emit(
                                    ParserResult.Results(
                                        parseTable(
                                            plugin.download.tableLink,
                                            plugin.download.regexes,
                                            source,
                                            plugin.url
                                        )
                                    )
                                )
                                emit(ParserResult.SearchFinished)
                            }
                            else -> emit(ParserResult.MissingImplementationError)
                        }
                    }
                }
            }
        }

    private fun parseTable(
        tableLink: TableDirect,
        regexes: PluginRegexes,
        source: String,
        baseUrl: String,
    ): List<ScrapedItem> {
        val tableItems = mutableListOf<ScrapedItem>()
        val doc: Document = Jsoup.parse(source)
        try {
            // restrict the document to a certain table
            val table: Element = when {
                tableLink.idName != null -> doc.getElementById(tableLink.idName)
                tableLink.className != null -> doc.getElementsByClass(tableLink.className).first()
                else -> doc.getElementsByTag("table").first()
            }

            // parse all the rows
            val rows = table.select("tr")
            val skipHead = table.select("thead").size > 0


            for (index in 0 until rows.size) {
                if (skipHead && index == 0)
                    continue

                // parse the cells according to the selected plugin
                val columns = rows[index].select("td")
                val name =
                    cleanName(
                        parseSingle(
                            regexes.nameRegex,
                            columns[tableLink.columns.nameColumn].html(),
                            baseUrl
                        ) ?: ""
                    )

                var details: String? = null
                if (tableLink.columns.detailsColumn != null)
                    details = parseSingle(
                        regexes.detailsRegex,
                        columns[tableLink.columns.detailsColumn].html(),
                        baseUrl
                    )
                var seeders: String? = null
                if (tableLink.columns.seedersColumn != null)
                    seeders = parseSingle(
                        regexes.seedersRegex,
                        columns[tableLink.columns.seedersColumn].html(),
                        baseUrl
                    )
                var leechers: String? = null
                if (tableLink.columns.leechersColumn != null)
                    leechers = parseSingle(
                        regexes.leechersRegex,
                        columns[tableLink.columns.leechersColumn].html(),
                        baseUrl
                    )
                var size: String? = null
                if (tableLink.columns.sizeColumn != null)
                    size = parseSingle(
                        regexes.sizeRegex,
                        columns[tableLink.columns.sizeColumn].html(),
                        baseUrl
                    )
                var magnets: List<String> = emptyList()
                if (tableLink.columns.magnetColumn != null)
                    magnets = parseList(
                        regexes.magnetRegex,
                        columns[tableLink.columns.magnetColumn].html(),
                        baseUrl
                    ).map {
                        // this function cleans links from html codes such as %3A, %3F etc.
                        it.removeWebFormatting()
                    }
                var torrents: List<String> = emptyList()
                if (tableLink.columns.torrentColumn != null)
                    torrents = parseList(
                        regexes.torrentRegexes,
                        columns[tableLink.columns.torrentColumn].html(),
                        baseUrl
                    )

                tableItems.add(
                    ScrapedItem(
                        name = name,
                        link = details,
                        seeders = seeders,
                        leechers = leechers,
                        size = size,
                        magnets = magnets,
                        torrents = torrents
                    )
                )
            }

        } catch (exception: NullPointerException) {
            Timber.d("Some not nullable values were null: ${exception.message}")
        }
        return tableItems
    }

    private suspend fun getSource(url: String): String = withContext(Dispatchers.IO) {
        val request: Request = Request.Builder()
            .url(url)
            .build()

        // todo: check if this works and add a custom agent
        // todo: return the complete Response to let the caller check the return code
        try {
            dohClient.client.newCall(request).execute().use { response: Response ->
                response.body?.string() ?: ""
            }
        } catch (e: SocketTimeoutException) {
            Timber.e("Error getting source while parsing link: ${e.message} ")
            ""
        }
    }

    private fun replaceData(
        oldUrl: String,
        url: String,
        query: String,
        category: String?,
        page: Int?,
    ): String {
        var newUrl = oldUrl.replace("\${url}", url)
            .replace("\${query}", query)
        if (category != null)
            newUrl = newUrl.replace("\${category}", category)
        if (page != null)
            newUrl = newUrl.replace("\${page}", page.toString())

        return newUrl
    }

    /**
     * Parse a single result from a source with a [CustomRegex]
     *
     * @param customRegex
     * @param source
     * @param url
     * @return
     */
    private fun parseSingle(customRegex: CustomRegex?, source: String, url: String): String? {
        if (customRegex == null)
            return null
        val regex: Regex = Regex(customRegex.regex, RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(source)?.groupValues?.get(customRegex.group) ?: return null
        return when (customRegex.slugType) {
            "append_url" -> {
                if (url.endsWith("/") && match.startsWith("/"))
                    url.removeSuffix("/") + match
                else
                    url + match
            }
            "append_other" -> {
                if (customRegex.other!!.endsWith("/") && match.startsWith("/"))
                    customRegex.other.removeSuffix("/") + match
                else
                    customRegex.other + match
            }
            "complete" -> match
            else -> match
        }
    }


    /**
     * Parse a list of results from a source with a [CustomRegex]
     *
     * @param customRegex
     * @param source
     * @param url
     * @return
     */
    private fun parseList(
        customRegexes: List<CustomRegex>?,
        source: String,
        url: String
    ): List<String> {
        if (customRegexes.isNullOrEmpty())
            return emptyList()
        val results = mutableSetOf<String>()
        for (customRegex in customRegexes) {
            val regex: Regex = customRegex.regex.toRegex()
            val matches = regex.findAll(source)
            for (match in matches) {
                val result: String = match.groupValues[customRegex.group]
                if (result.isNotBlank())
                    results.add(
                        when (customRegex.slugType) {
                            "append_url" -> {
                                if (url.endsWith("/") && result.startsWith("/"))
                                    url.removeSuffix("/") + result
                                else
                                    url + result
                            }
                            "append_other" -> {
                                if (customRegex.other!!.endsWith("/") && result.startsWith("/"))
                                    customRegex.other.removeSuffix("/") + result
                                else
                                    customRegex.other + result
                            }
                            "complete" -> result
                            else -> result
                        }
                    )
            }

        }

        return results.toList()
    }

    private fun parseList(
        customRegex: CustomRegex?,
        source: String,
        url: String
    ): List<String> {
        return if (customRegex != null)
            parseList(listOf(customRegex), source, url)
        else emptyList()
    }

    private fun getCategory(plugin: Plugin, category: String): String? {
        return when (category) {
            "all" -> plugin.supportedCategories.all
            "anime" -> plugin.supportedCategories.anime
            "software" -> plugin.supportedCategories.software
            "games" -> plugin.supportedCategories.games
            "movies" -> plugin.supportedCategories.movies
            "music" -> plugin.supportedCategories.music
            "tv" -> plugin.supportedCategories.tv
            "books" -> plugin.supportedCategories.books
            else -> null
        }
    }

    private fun cleanName(name: String): String {
        val textFromHtml: Spanned = HtmlCompat.fromHtml(name, HtmlCompat.FROM_HTML_MODE_COMPACT)
        return textFromHtml.trim()
            // replace newlines with spaces
            .replace("\\R+".toRegex(), " ")
            // remove all html tags from the name. Will replace anything like <*> if it's in the name
            .replace("<[^>]+>".toRegex(), "")
            // replace multiple spaces with single space
            .replace("\\s{2,}".toRegex(), " ")
    }

    companion object {
        const val PLUGIN_ENGINE_VERSION: Double = 1.0
    }
}

sealed class ParserResult {
    // errors
    object MissingPlugin : ParserResult()
    object PluginVersionUnsupported : ParserResult()
    object MissingQuery : ParserResult()
    object MissingCategory : ParserResult()
    object NetworkBodyError : ParserResult()
    object EmptyInnerLinksError : ParserResult()
    object PluginBuildError : ParserResult()
    object MissingImplementationError : ParserResult()

    // search flow
    data class SearchStarted(val size: Int) : ParserResult()
    object SearchFinished : ParserResult()

    // results
    data class Results(val values: List<ScrapedItem>) : ParserResult()
    data class SingleResult(val value: ScrapedItem) : ParserResult()
}

data class ScrapedItem(
    val name: String,
    val link: String?,
    val seeders: String? = null,
    val leechers: String? = null,
    val size: String? = null,
    val magnets: List<String>,
    val torrents: List<String>
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.createStringArrayList() ?: emptyList(),
        parcel.createStringArrayList() ?: emptyList()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(link)
        parcel.writeString(seeders)
        parcel.writeString(leechers)
        parcel.writeString(size)
        parcel.writeStringList(magnets)
        parcel.writeStringList(torrents)
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