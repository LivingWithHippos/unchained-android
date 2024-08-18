package com.github.livingwithhippos.unchained.data.model.jackett

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.github.livingwithhippos.unchained.data.model.torznab.Capabilities

@JacksonXmlRootElement(localName = "indexers")
data class Indexers(
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "indexer")
    val indexers: List<Indexer>
)

data class Indexer(
    @JacksonXmlProperty(isAttribute = true, localName = "id") val id: String,
    @JacksonXmlProperty(isAttribute = true, localName = "configured") val configured: String,
    @JacksonXmlProperty(localName = "title") val title: String,
    @JacksonXmlProperty(localName = "description") val description: String,
    @JacksonXmlProperty(localName = "link") val link: String,
    @JacksonXmlProperty(localName = "type") val type: String,
    @JacksonXmlProperty(localName = "caps") val capabilities: Capabilities
)
