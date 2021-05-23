package com.github.livingwithhippos.unchained.plugins

import android.os.Parcel
import android.os.Parcelable
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import okhttp3.dnsoverhttps.DnsOverHttps

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
                        oldUrl = plugin.search.urlNoCategory,
                        url = plugin.url,
                        query = currentQuery,
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

        // todo: check if this works and add a custom agent
        // todo: return the complete Response to let the caller check the return code
        dohClient.client.newCall(request).execute().use { response: Response ->
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
        val magnets = mutableSetOf<String>()
        val torrents = mutableSetOf<String>()
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
        return LinkData(link, name, magnets.toList(), torrents.toList())
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
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString()?: "",
        parcel.createStringArrayList() ?: emptyList<String>(),
        parcel.createStringArrayList() ?: emptyList<String>()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(link)
        parcel.writeString(name)
        parcel.writeStringList(magnets)
        parcel.writeStringList(torrents)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LinkData> {
        override fun createFromParcel(parcel: Parcel): LinkData {
            return LinkData(parcel)
        }

        override fun newArray(size: Int): Array<LinkData?> {
            return arrayOfNulls(size)
        }
    }
}