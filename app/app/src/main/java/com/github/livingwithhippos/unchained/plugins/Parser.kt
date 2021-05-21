package com.github.livingwithhippos.unchained.plugins

import com.github.livingwithhippos.unchained.plugins.model.Plugin
import okhttp3.OkHttpClient
import okhttp3.Request

class Parser(val client: OkHttpClient) {

    // todo: implement okhttp DnsOverHttps (https://square.github.io/okhttp/4.x/okhttp-dnsoverhttps/okhttp3.dnsoverhttps/-dns-over-https/) to get the domains

    fun checkSupportedVersion(plugin: Plugin): Boolean {
        return plugin.engineVersion.toInt() == PLUGIN_ENGINE_VERSION.toInt()
    }

    fun search(plugin: Plugin, query: String, category: String? = null): ParserResult {
        if (query.isNotEmpty()) {
            if (category != null)
                return searchWithCategory(plugin, query, category)
            else
                return searchWithoutCategory(plugin, query)
        } else {
            return ParserResult.MissingQuery
        }
    }

    private fun getSource(url: String): String {
        val request: Request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            return response.body?.string() ?: ""
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

    private fun searchWithCategory(
        plugin: Plugin,
        query: String,
        category: String,
        page: Int = 1
    ): ParserResult {
        val currentCategory = getCategory(plugin, category)
        val queryUrl = plugin.search.urlCategory
        if (currentCategory == null || queryUrl == null)
            return ParserResult.MissingCategory
        else {
            return parsePage(plugin, queryUrl)
        }
    }


    private fun parseLinks(plugin: Plugin, source: String): LinkData {
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
        return LinkData(name, magnets, torrents)
    }

    private fun searchWithoutCategory(plugin: Plugin, query: String, pag: Int = 1): ParserResult {
        val url = plugin.url
        // todo: check var substitution
        return parsePage(plugin, plugin.search.urlNoCategory)
    }

    private fun parsePage(plugin: Plugin, query: String): ParserResult {
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
                    results.add(parseLinks(plugin, s))
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
    val name: String,
    val magnets: List<String>,
    val torrents: List<String>
)