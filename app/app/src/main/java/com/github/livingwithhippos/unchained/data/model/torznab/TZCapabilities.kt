package com.github.livingwithhippos.unchained.data.model.torznab

import com.github.livingwithhippos.unchained.utilities.directChild
import com.github.livingwithhippos.unchained.utilities.directChildren
import kotlin.collections.ifEmpty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

@Serializable
data class Capabilities(
    val server: Server,
    val limits: Limits,
    val searching: Searching,
    val categories: Categories,
)

@Serializable data class Server(val title: String)

@Serializable data class Limits(@SerialName("default") val default: Int, val max: Int)

@Serializable
data class Searching(
    val search: CapsSearch,
    @SerialName("tv-search") val tvSearch: CapsSearch,
    @SerialName("movie-search") val movieSearch: CapsSearch,
    @SerialName("music-search") val musicSearch: CapsSearch,
    @SerialName("audio-search") val audioSearch: CapsSearch,
    @SerialName("book-search") val bookSearch: CapsSearch,
)

@Serializable
data class CapsSearch(val available: String, val supportedParams: String, val searchEngine: String?)

@Serializable data class Categories(@SerialName("category") val category: List<Category>)

@Serializable
data class Category(
    val id: Int,
    val name: String,
    @SerialName("subcat") val subcat: List<SubCategory>? = null,
)

@Serializable data class SubCategory(val id: Int, val name: String)

fun parseCapabilities(element: Element?): Capabilities? {
    if (element == null) return null

    val capabilities: Capabilities = element.let {
        val serverTitle = it.directChild("server")?.attr("title") ?: ""
        val limitsDefault = it.directChild("limits")?.attr("default") ?: "0"
        val limitsMax = it.directChild("limits")?.attr("max") ?: "0"

        val searchingElement =
            it.directChild("searching")
                ?: throw IllegalArgumentException("Missing searching element")

        val search = searchingElement.directChild("search")
        val tvSearch = searchingElement.directChild("tv-search")
        val movieSearch = searchingElement.directChild("movie-search")
        val musicSearch = searchingElement.directChild("music-search")
        val audioSearch = searchingElement.directChild("audio-search")
        val bookSearch = searchingElement.directChild("book-search")

        Capabilities(
            server = Server(title = serverTitle),
            limits =
                Limits(
                    default = limitsDefault.toIntOrNull() ?: 0,
                    max = limitsMax.toIntOrNull() ?: 0,
                ),
            searching =
                Searching(
                    search =
                        CapsSearch(
                            available = search?.attr("available") ?: "no",
                            supportedParams = search?.attr("supportedParams") ?: "",
                            searchEngine = search?.attr("searchEngine"),
                        ),
                    tvSearch =
                        CapsSearch(
                            available = tvSearch?.attr("available") ?: "no",
                            supportedParams = tvSearch?.attr("supportedParams") ?: "",
                            searchEngine = tvSearch?.attr("searchEngine"),
                        ),
                    movieSearch =
                        CapsSearch(
                            available = movieSearch?.attr("available") ?: "no",
                            supportedParams = movieSearch?.attr("supportedParams") ?: "",
                            searchEngine = movieSearch?.attr("searchEngine"),
                        ),
                    musicSearch =
                        CapsSearch(
                            available = musicSearch?.attr("available") ?: "no",
                            supportedParams = musicSearch?.attr("supportedParams") ?: "",
                            searchEngine = musicSearch?.attr("searchEngine"),
                        ),
                    audioSearch =
                        CapsSearch(
                            available = audioSearch?.attr("available") ?: "no",
                            supportedParams = audioSearch?.attr("supportedParams") ?: "",
                            searchEngine = audioSearch?.attr("searchEngine"),
                        ),
                    bookSearch =
                        CapsSearch(
                            available = bookSearch?.attr("available") ?: "no",
                            supportedParams = bookSearch?.attr("supportedParams") ?: "",
                            searchEngine = bookSearch?.attr("searchEngine"),
                        ),
                ),
            categories =
                Categories(
                    category =
                        element.directChild("categories")?.directChildren("category")?.mapNotNull {
                            categoryElement ->
                            val categoryId =
                                categoryElement.attr("id").toIntOrNull() ?: return@mapNotNull null
                            val categoryName = categoryElement.attr("name")
                            val subcategories =
                                categoryElement.directChildren("subcat").map { subcatElement ->
                                    SubCategory(
                                        id = subcatElement.attr("id").toIntOrNull() ?: -1,
                                        name = subcatElement.attr("name"),
                                    )
                                }
                            Category(
                                id = categoryId,
                                name = categoryName,
                                subcat = subcategories.ifEmpty { null },
                            )
                        } ?: emptyList()
                ),
        )
    }

    return capabilities
}

fun parseCapabilities(body: String): Capabilities? {
    val document = Jsoup.parse(body, "", Parser.xmlParser())
    val capsElement = document.directChild("caps") ?: return null

    return parseCapabilities(capsElement)
}
