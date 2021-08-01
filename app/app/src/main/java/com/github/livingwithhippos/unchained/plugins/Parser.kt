package com.github.livingwithhippos.unchained.plugins

import android.text.Spanned
import androidx.core.text.HtmlCompat
import com.github.livingwithhippos.unchained.plugins.model.CustomRegex
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.plugins.model.PluginRegexes
import com.github.livingwithhippos.unchained.plugins.model.RegexpsGroup
import com.github.livingwithhippos.unchained.plugins.model.ScrapedItem
import com.github.livingwithhippos.unchained.plugins.model.TableParser
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
    private val dohClient: DnsOverHttps
) {

    private fun isPluginSupported(plugin: Plugin): Boolean {
        return plugin.engineVersion.toInt() == PLUGIN_ENGINE_VERSION.toInt() && PLUGIN_ENGINE_VERSION >= plugin.engineVersion
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
                            plugin.download.internalParser != null -> {
                                emit(ParserResult.SearchStarted(-1))
                                val innerSource = mutableListOf<String>()

                                plugin.download.internalParser.link.regexps.forEach {
                                    val linksFound = parseList(
                                        it,
                                        source,
                                        plugin.url
                                    )
                                    innerSource.addAll(linksFound)
                                    if (plugin.download.internalParser.link.regexUse == "first") {
                                        // if I wanted to get only the first matches I can exit the loop if I have results
                                        if (linksFound.isNotEmpty())
                                            return@forEach
                                    }
                                }

                                emit(ParserResult.SearchStarted(innerSource.size))
                                if (innerSource.isNotEmpty()) {
                                    for (link in innerSource) {
                                        // parse every page linked to the results
                                        val s = getSource(link)
                                        val scrapedItem = parseInnerLink(
                                            plugin.download.regexes,
                                            s,
                                            link,
                                            plugin.url
                                        )

                                        emit(ParserResult.SingleResult(scrapedItem))
                                    }
                                    emit(ParserResult.SearchFinished)
                                } else {
                                    emit(ParserResult.EmptyInnerLinks)
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
                            plugin.download.indirectTableLink != null -> {
                                emit(ParserResult.SearchStarted(-1))
                                val links = parseIndirectTable(
                                    plugin.download.indirectTableLink,
                                    plugin.download.regexes,
                                    source,
                                    plugin.url
                                )
                                emit(ParserResult.SearchStarted(links.size))
                                links.forEach {

                                    val itemSource = getSource(it)
                                    val scrapedItem = parseInnerLink(
                                        plugin.download.regexes,
                                        itemSource,
                                        it,
                                        plugin.url
                                    )
                                    emit(ParserResult.SingleResult(scrapedItem))
                                }
                                emit(ParserResult.SearchFinished)
                            }
                            else -> emit(ParserResult.MissingImplementationError)
                        }
                    }
                }
            }
        }

    private fun parseIndirectTable(
        tableParser: TableParser,
        regexes: PluginRegexes,
        source: String,
        baseUrl: String
    ): List<String> {
        val tableLinks = mutableListOf<String>()
        val doc: Document = Jsoup.parse(source)
        try {
            // restrict the document to a certain table
            val table: Element = when {
                tableParser.idName != null -> doc.getElementById(tableParser.idName)
                tableParser.className != null -> doc.getElementsByClass(tableParser.className)
                    .firstOrNull()
                else -> doc.getElementsByTag("table").first()
            } ?: return emptyList()

            // parse all the rows
            val rows = table.select("tr")
            val head = if (table.select("thead").size > 0) 1 else 0

            if (tableParser.columns.detailsColumn != null) {

                for (index in head until rows.size) {
                    // parse the cells according to the selected plugin
                    val columns = rows[index].select("td")
                    regexes.detailsRegex?.regexps?.forEach {
                        try {
                            val details = parseSingle(
                                it,
                                columns[tableParser.columns.detailsColumn].html(),
                                baseUrl
                            )
                            if (details != null)
                                tableLinks.add(details)

                            if (regexes.detailsRegex.regexUse == "first") {
                                if (!details.isNullOrEmpty())
                                    return@forEach
                            }

                        } catch (e: IndexOutOfBoundsException) {
                            Timber.d("skipping row")
                        }
                    }
                }
            }
        } catch (exception: NullPointerException) {
            Timber.d("Some not nullable values were null: ${exception.message}")
        }

        return tableLinks
    }

    private fun parseInnerLink(
        regexes: PluginRegexes,
        source: String,
        link: String,
        baseUrl: String
    ): ScrapedItem {

        var name = ""
        regexes.nameRegex.regexps.forEach {

            val parsedName = cleanName(
                parseSingle(
                    it,
                    source,
                    baseUrl
                ) ?: ""
            )
            // this is a single string, no need to check for regexUse
            if (!parsedName.isNullOrBlank()) {
                name = parsedName
                return@forEach
            }
        }

        // parse magnets
        val magnets = mutableListOf<String>()
        regexes.magnetRegex?.regexps?.forEach { regex ->

            val parsedMagnets = parseList(
                regex,
                source,
                baseUrl
            ).map { it.removeWebFormatting() }

            magnets.addAll(parsedMagnets)

            if (regexes.magnetRegex.regexUse == "first") {
                if (magnets.isNotEmpty())
                    return@forEach
            }
        }
        // parse torrents
        val torrents = mutableListOf<String>()
        regexes.torrentRegexes?.regexps?.forEach { regex ->
            torrents.addAll(
                parseList(
                    regex,
                    source,
                    baseUrl
                )
            )

            if (regexes.torrentRegexes.regexUse == "first") {
                if (torrents.isNotEmpty())
                    return@forEach
            }
        }

        var seeders: String? = null
        // todo: move to function
        regexes.seedersRegex?.regexps?.forEach { regex ->
            val parsedSeeders = parseSingle(
                regex,
                source,
                baseUrl
            )

            if (!parsedSeeders.isNullOrBlank()) {
                seeders = parsedSeeders
                return@forEach
            }
        }

        var leechers: String? = null
        regexes.leechersRegex?.regexps?.forEach { regex ->
            val leechersSeeders = parseSingle(
                regex,
                source,
                baseUrl
            )

            if (!leechersSeeders.isNullOrBlank()) {
                leechers = leechersSeeders
                return@forEach
            }
        }

        var size: String? = null
        regexes.sizeRegex?.regexps?.forEach { regex ->
            val sizeSeeders = parseSingle(
                regex,
                source,
                baseUrl
            )

            if (!sizeSeeders.isNullOrBlank()) {
                size = sizeSeeders
                return@forEach
            }
        }

        val parsedSize: Double? = parseCommonSize(size)

        return ScrapedItem(
            name = name,
            link = link,
            seeders = seeders,
            leechers = leechers,
            size = size,
            parsedSize = parsedSize,
            magnets = magnets,
            torrents = torrents
        )
    }

    private fun parseCommonSize(size: String?): Double? {
        try {

            size ?: return null

            val numbers = "[\\d.]+".toRegex().find(size)?.value ?: return null

            val baseSize = numbers.toDouble()
            if (size.contains("gb", ignoreCase = true)) {
                return baseSize * 1024 * 1024
            }
            if (size.contains("mb", ignoreCase = true)) {
                return baseSize * 1024
            }
            // KiloBytes are already at the size I need
            return baseSize
        } catch (e: NumberFormatException) {
            return null
        }
    }

    private fun parseTable(
        tableLink: TableParser,
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
                tableLink.className != null -> doc.getElementsByClass(tableLink.className)
                    .firstOrNull()
                else -> doc.getElementsByTag("table").first()
            } ?: return emptyList()

            // parse all the rows
            val rows = table.select("tr")
            val head = if (table.select("thead").size > 0) 1 else 0

            for (index in head until rows.size) {
                // parse the cells according to the selected plugin
                val columns = rows[index].select("td")
                var name: String? = null
                var details: String? = null
                var seeders: String? = null
                var leechers: String? = null
                var size: String? = null
                var magnets: List<String> = emptyList()
                var torrents: List<String> = emptyList()
                try {

                    if (tableLink.columns.nameColumn != null)
                        name =
                            cleanName(
                                parseSingle(
                                    regexes.nameRegex,
                                    columns[tableLink.columns.nameColumn].html(),
                                    baseUrl
                                ) ?: ""
                            )

                    if (tableLink.columns.detailsColumn != null)
                        details = parseSingle(
                            regexes.detailsRegex,
                            columns[tableLink.columns.detailsColumn].html(),
                            baseUrl
                        )
                    if (tableLink.columns.seedersColumn != null)
                        seeders = parseSingle(
                            regexes.seedersRegex,
                            columns[tableLink.columns.seedersColumn].html(),
                            baseUrl
                        )
                    if (tableLink.columns.leechersColumn != null)
                        leechers = parseSingle(
                            regexes.leechersRegex,
                            columns[tableLink.columns.leechersColumn].html(),
                            baseUrl
                        )
                    if (tableLink.columns.sizeColumn != null)
                        size = parseSingle(
                            regexes.sizeRegex,
                            columns[tableLink.columns.sizeColumn].html(),
                            baseUrl
                        )
                    if (tableLink.columns.magnetColumn != null)
                        magnets = parseList(
                            regexes.magnetRegex,
                            columns[tableLink.columns.magnetColumn].html(),
                            baseUrl
                        ).map {
                            // this function cleans links from html codes such as %3A, %3F etc.
                            it.removeWebFormatting()
                        }
                    if (tableLink.columns.torrentColumn != null)
                        torrents = parseList(
                            regexes.torrentRegexes,
                            columns[tableLink.columns.torrentColumn].html(),
                            baseUrl
                        )
                } catch (e: IndexOutOfBoundsException) {
                    Timber.d("skipping row")
                }

                val parsedSize: Double? = parseCommonSize(size)

                if (name != null && (magnets.isNotEmpty() || torrents.isNotEmpty()))
                    tableItems.add(
                        ScrapedItem(
                            name = name,
                            link = details,
                            seeders = seeders,
                            leechers = leechers,
                            size = size,
                            parsedSize = parsedSize,
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
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36"
            )
            .build()

        // todo: check if this works
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
        val regex = Regex(customRegex.regex, RegexOption.DOT_MATCHES_ALL)
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

    private fun parseSingle(regexpsGroup: RegexpsGroup?, source: String, url: String): String? {
        if (regexpsGroup == null)
            return null
        var parsedResult: String? = null
        regexpsGroup.regexps.forEach { regex ->
            val currentRegex = Regex(regex.regex, RegexOption.DOT_MATCHES_ALL)

            val match = currentRegex.find(source)?.groupValues?.get(regex.group)
            if (match != null) {
                parsedResult = when (regex.slugType) {
                    "append_url" -> {
                        if (url.endsWith("/") && match.startsWith("/"))
                            url.removeSuffix("/") + match
                        else
                            url + match
                    }
                    "append_other" -> {
                        if (regex.other!!.endsWith("/") && match.startsWith("/"))
                            regex.other.removeSuffix("/") + match
                        else
                            regex.other + match
                    }
                    "complete" -> match
                    else -> match
                }
                if (!parsedResult.isNullOrBlank())
                    return parsedResult
            }
        }

        return parsedResult
    }

    /**
     * Parse a list of results from a source with a [CustomRegex]
     *
     * @param customRegexes
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
        regexpsGroup: RegexpsGroup?,
        source: String,
        url: String
    ): List<String> {
        if (regexpsGroup == null || regexpsGroup.regexps.isEmpty())
            return emptyList()

        val results = mutableSetOf<String>()
        regexLoop@ for (customRegex in regexpsGroup.regexps) {
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

            if (regexpsGroup.regexUse == "single") {
                if (results.isNotEmpty()) {
                    return results.toList()
                }
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
        /**
         * CHANGELOG:
         * 1.0: first version
         * 1.1: added skipping of empty rows in tables
         * 1.2: added table_indirect
         * 2.0: use array for regexes
         */
        const val PLUGIN_ENGINE_VERSION: Float = 1.2f
    }
}

sealed class ParserResult {
    // errors
    object MissingPlugin : ParserResult()
    object PluginVersionUnsupported : ParserResult()
    object MissingQuery : ParserResult()
    object MissingCategory : ParserResult()
    object NetworkBodyError : ParserResult()
    object EmptyInnerLinks : ParserResult()
    object PluginBuildError : ParserResult()
    object MissingImplementationError : ParserResult()

    // search flow
    data class SearchStarted(val size: Int) : ParserResult()
    object SearchFinished : ParserResult()

    // results
    data class Results(val values: List<ScrapedItem>) : ParserResult()
    data class SingleResult(val value: ScrapedItem) : ParserResult()
}
