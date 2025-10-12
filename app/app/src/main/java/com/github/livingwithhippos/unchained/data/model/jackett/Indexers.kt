package com.github.livingwithhippos.unchained.data.model.jackett

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.github.livingwithhippos.unchained.data.model.torznab.Capabilities

@JacksonXmlRootElement(localName = "indexers")
data class Indexers(
    @param:JacksonXmlElementWrapper(useWrapping = false)
    @param:JacksonXmlProperty(localName = "indexer")
    val indexers: List<Indexer>
)

data class Indexer(
    @param:JacksonXmlProperty(isAttribute = true, localName = "id") val id: String,
    @param:JacksonXmlProperty(isAttribute = true, localName = "configured") val configured: String,
    @param:JacksonXmlProperty(localName = "title") val title: String,
    @param:JacksonXmlProperty(localName = "description") val description: String,
    @param:JacksonXmlProperty(localName = "link") val link: String,
    @param:JacksonXmlProperty(localName = "type") val type: String,
    @param:JacksonXmlProperty(localName = "caps") val capabilities: Capabilities,
)
