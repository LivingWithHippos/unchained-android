package com.github.livingwithhippos.unchained.data.model.torznab

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "rss")
data class SearchRSS(@param:JacksonXmlProperty(localName = "channel") val channel: Channel)

data class Channel(
    @param:JacksonXmlProperty(localName = "title") val title: String,
    @param:JacksonXmlProperty(localName = "description") val description: String,
    @param:JacksonXmlProperty(localName = "link") val link: String,
    @param:JacksonXmlProperty(localName = "language") val language: String,
    @param:JacksonXmlProperty(localName = "category") val category: String,
    @param:JacksonXmlProperty(localName = "item") val items: List<Item>,
)

data class Item(
    @param:JacksonXmlProperty(localName = "title") val title: String,
    @param:JacksonXmlProperty(localName = "guid") val guid: String,
    @param:JacksonXmlProperty(localName = "type") val type: String,
    @param:JacksonXmlProperty(localName = "comments") val comments: String,
    @param:JacksonXmlProperty(localName = "pubDate") val pubDate: String,
    @param:JacksonXmlProperty(localName = "size") val size: String,
    @param:JacksonXmlProperty(localName = "description") val description: String,
    @param:JacksonXmlProperty(localName = "link") val link: String,
    @param:JacksonXmlProperty(localName = "category") val categories: List<String>,
    @param:JacksonXmlProperty(localName = "enclosure") val enclosure: Enclosure,
    // todo: check what happens with empty responses, nullable? default emptyList()?
    @param:JacksonXmlElementWrapper(useWrapping = false)
    @param:JacksonXmlProperty(namespace = "torznab", localName = "attr")
    val torznabAttributes: List<TorznabAttribute>,
)

data class Enclosure(
    @param:JacksonXmlProperty(isAttribute = true, localName = "url") val url: String,
    @param:JacksonXmlProperty(isAttribute = true, localName = "length") val length: String,
    @param:JacksonXmlProperty(isAttribute = true, localName = "type") val type: String,
)

data class TorznabAttribute(
    @param:JacksonXmlProperty(isAttribute = true, localName = "name") val name: String,
    @param:JacksonXmlProperty(isAttribute = true, localName = "value") val value: String,
)
