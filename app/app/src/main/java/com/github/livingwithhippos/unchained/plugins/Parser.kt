package com.github.livingwithhippos.unchained.plugins

import com.github.livingwithhippos.unchained.plugins.model.Plugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class Parser(val client: OkHttpClient) {

    // todo: implement okhttp DnsOverHttps (https://square.github.io/okhttp/4.x/okhttp-dnsoverhttps/okhttp3.dnsoverhttps/-dns-over-https/) to get the domains

    private fun isPluginSupported(plugin: Plugin): Boolean {
        return plugin.engineVersion.toInt() == PLUGIN_ENGINE_VERSION.toInt()
    }

    fun completeSearch(plugin: Plugin, query: String, category: String? = null, page: Int = 1) =
        flow {
            // todo: format queries with unsupported web characters
            if (query.isBlank())
                emit(ParserResult.MissingQuery)
            else {
                if (!isPluginSupported(plugin)) {
                    emit(ParserResult.PluginVersionUnsupported)
                } else {
                    val currentCategory =
                        if (category.isNullOrBlank()) null else getCategory(plugin, category)

                    val queryUrl = replaceData(
                        oldUrl = plugin.search.urlNoCategory,
                        url = plugin.url,
                        query = query,
                        category = currentCategory,
                        page = page
                    )

                    val source = getSource(queryUrl)

                    if (source.length < 10)
                        emit(ParserResult.NetworkBodyError)
                    else {
                        if (plugin.download.internalLink != null) {
                            // check if all the options are acceptable before calling parseInnerLinks
                            if (plugin.download.internalLink.slugType == "append_other"
                                && plugin.download.internalLink.other.isNullOrBlank()
                            )
                                emit(ParserResult.PluginBuildError)
                            else {
                                val innerSource: List<String> = parseInnerLinks(plugin, source)
                                if (innerSource.isNotEmpty()) {
                                    emit(ParserResult.SearchStarted(innerSource.size))
                                    for (link in innerSource) {
                                        val s = getSource(link)
                                        emit(ParserResult.SingleResult(parseLinks(plugin, s, link)))
                                    }
                                    emit(ParserResult.SearchFinished)
                                } else {
                                    emit(ParserResult.EmptyInnerLinksError)
                                }
                            }
                        } else {
                            // todo: add other ways to parse links
                            emit(ParserResult.MissingImplementationError)
                        }
                    }
                }
            }
        }


    private suspend fun getSource(url: String): String = withContext(Dispatchers.IO) {
        val request: Request = Request.Builder()
            .url(url)
            .build()

        // todo: return the complete Response to let the caller check the return code
        client.newCall(request).execute().use { response: Response ->
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
    data class Result(val values: List<LinkData>) : ParserResult()
    data class SingleResult(val value: LinkData) : ParserResult()
}

data class LinkData(
    val link: String,
    val name: String,
    val magnets: List<String>,
    val torrents: List<String>
)