package com.github.livingwithhippos.unchained.data.model.jackett

import com.github.livingwithhippos.unchained.data.model.torznab.Capabilities
import com.github.livingwithhippos.unchained.data.model.torznab.parseCapabilities
import com.github.livingwithhippos.unchained.utilities.directChild
import com.github.livingwithhippos.unchained.utilities.directChildren
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

@Serializable data class Indexers(@SerialName("indexer") val indexers: List<Indexer>)

@Serializable
data class Indexer(
    val id: String,
    val configured: String,
    val title: String,
    val description: String,
    val link: String,
    val type: String,
    @SerialName("caps") val capabilities: Capabilities,
)

fun parseIndexers(body: String): Indexers? {

    val document = Jsoup.parse(body, "", Parser.xmlParser())
    val indexers =
        document.directChild("indexers")
            ?: throw IllegalArgumentException("Missing indexers element")
    val indexerList =
        indexers.directChildren("indexer").map { indexerElement ->
            val id = indexerElement.attr("id")
            val configured = indexerElement.attr("configured")
            val title = indexerElement.directChild("title")
            val description = indexerElement.directChild("description")
            val link = indexerElement.directChild("link")
            val type = indexerElement.directChild("type")

            val capabilities = parseCapabilities(indexerElement.directChild("caps")) ?: return null
            Indexer(
                id = id,
                configured = configured,
                title = title?.text() ?: "",
                description = description?.text() ?: "",
                link = link?.text() ?: "",
                type = type?.text() ?: "",
                capabilities = capabilities,
            )
        }
    return Indexers(indexerList)
}
