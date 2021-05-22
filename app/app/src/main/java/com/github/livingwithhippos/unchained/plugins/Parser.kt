package com.github.livingwithhippos.unchained.plugins

import com.github.livingwithhippos.unchained.plugins.model.Plugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class Parser(val client: OkHttpClient) {

    // todo: implement okhttp DnsOverHttps (https://square.github.io/okhttp/4.x/okhttp-dnsoverhttps/okhttp3.dnsoverhttps/-dns-over-https/) to get the domains

    fun checkSupportedVersion(plugin: Plugin): Boolean {
        return plugin.engineVersion.toInt() == PLUGIN_ENGINE_VERSION.toInt()
    }

    suspend fun search(plugin: Plugin, query: String, category: String? = null): ParserResult {
        if (query.isNotEmpty()) {
            if (category != null)
                return searchWithCategory(plugin, query, category)
            else
                return searchWithoutCategory(plugin, query)
        } else {
            return ParserResult.MissingQuery
        }
    }

    private suspend fun getSource(url: String): String =  withContext(Dispatchers.IO) {
        val request: Request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            response.body?.string() ?: ""
        }
    }

    private fun parseInnerLinks(plugin: Plugin, source: String): List<String> {
        val regex: Regex = plugin.download.internalLink!!.link.toRegex()
        val matches: Sequence<MatchResult> = regex.findAll(source)

        return matches.map {
            when (plugin.download.internalLink.slugType) {
                "append_url" -> plugin.url + it.groupValues[1]
                "append_other" -> plugin.download.internalLink.other!! + it.groupValues[1]
                "completed" -> it.groupValues[1]
                else -> it.groupValues[1]
            }
        }.toList()
    }

    private suspend fun searchWithCategory(
        plugin: Plugin,
        query: String,
        category: String,
        page: Int = 1
    ): ParserResult {
        val currentCategory = getCategory(plugin, category)
        val queryUrl = replaceData(
            oldUrl = plugin.search.urlCategory!!,
            url = plugin.url,
            query = query,
            category = currentCategory,
            page = page
        )
        if (currentCategory == null)
            return ParserResult.MissingCategory
        else {
            return parsePage(plugin, queryUrl)
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


    private fun parseLinks(plugin: Plugin, source: String, link: String): LinkData {
        val magnets = mutableListOf<String>()
        val torrents = mutableListOf<String>()
        // get magnets
        val magnetRegex: Regex = plugin.download.magnet.toRegex()
        val matches: Sequence<MatchResult> = magnetRegex.findAll(source)
        magnets.addAll(
            matches.map { it.groupValues[1] }
        )
        // get torrents
        val torrentRegexes: List<Regex> = plugin.download.torrent.map {
            it.toRegex()
        }
        torrentRegexes.forEach {
            val m = it.findAll(source)
            torrents.addAll(
                m.map { torrent -> torrent.groupValues[1] }
            )
        }
        val nameRegex: Regex = plugin.download.name.toRegex()
        val name: String = nameRegex.find(source)?.groupValues?.get(1) ?: ""
        return LinkData(link, name, magnets, torrents)
    }

    private suspend fun searchWithoutCategory(plugin: Plugin, query: String, page: Int = 1): ParserResult {

        val queryUrl = replaceData(
            oldUrl = plugin.search.urlNoCategory,
            url = plugin.url,
            query = query,
            category = null,
            page = page
        )

        return parsePage(plugin, queryUrl)
    }

    private suspend fun parsePage(plugin: Plugin, query: String): ParserResult {
        val source = getSource(query)
        if (source.length < 10)
            return ParserResult.NetworkBodyError
        // todo: add other ways to parse links
        if (plugin.download.internalLink != null) {
            // check if all the options are acceptable before calling parseInnerLinks
            if (plugin.download.internalLink.slugType == "append_other"
                && plugin.download.internalLink.other == null
            )
                return ParserResult.PluginParsingError

            val innerSource: List<String> = parseInnerLinks(plugin, source)
            if (innerSource.isNotEmpty()) {
                val results = mutableListOf<LinkData>()
                for (link in innerSource) {
                    val s = getSource(link)
                    results.add(parseLinks(plugin, s, link))
                }
                return ParserResult.Result(results)
            } else {
                return ParserResult.EmptyInnerLinksError
            }
        } else {
            return ParserResult.MissingImplementationError
        }
    }

    private fun getCategory(plugin: Plugin, category: String): String? {
        return when (category) {
            "all" -> plugin.supportedCategories?.all
            "anime" -> plugin.supportedCategories?.anime
            "software" -> plugin.supportedCategories?.software
            "games" -> plugin.supportedCategories?.games
            "movies" -> plugin.supportedCategories?.movies
            "music" -> plugin.supportedCategories?.music
            "tv" -> plugin.supportedCategories?.tv
            else -> null
        }
    }

    companion object {
        const val PLUGIN_ENGINE_VERSION: Double = 1.0
    }
}

sealed class ParserResult {
    object MissingQuery : ParserResult()
    object MissingCategory : ParserResult()
    object NetworkBodyError : ParserResult()
    object EmptyInnerLinksError : ParserResult()
    object PluginParsingError : ParserResult()
    object MissingImplementationError : ParserResult()
    data class Result(val values: List<LinkData>) : ParserResult()
}

data class LinkData(
    val link: String,
    val name: String,
    val magnets: List<String>,
    val torrents: List<String>
)