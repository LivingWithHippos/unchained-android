package com.github.livingwithhippos.unchained.data.model.torznab

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "rss")
data class SearchRSS(
    @JacksonXmlProperty(localName = "channel") val channel: Channel,
)

data class Channel(
    @JacksonXmlProperty(localName = "title") val title: String,
    @JacksonXmlProperty(localName = "description") val description: String,
    @JacksonXmlProperty(localName = "link") val link: String,
    @JacksonXmlProperty(localName = "language") val language: String,
    @JacksonXmlProperty(localName = "category") val category: String,
    @JacksonXmlProperty(localName = "item") val items: List<Item>
)

data class Item(
    @JacksonXmlProperty(localName = "title") val title: String,
    @JacksonXmlProperty(localName = "guid") val guid: String,
    @JacksonXmlProperty(localName = "type") val type: String,
    @JacksonXmlProperty(localName = "comments") val comments: String,
    @JacksonXmlProperty(localName = "pubDate") val pubDate: String,
    @JacksonXmlProperty(localName = "size") val size: String,
    @JacksonXmlProperty(localName = "description") val description: String,
    @JacksonXmlProperty(localName = "link") val link: String,
    @JacksonXmlProperty(localName = "category") val categories: List<String>,
    @JacksonXmlProperty(localName = "enclosure") val enclosure: Enclosure,
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(namespace = "torznab", localName = "attr") val torznabAttributes: List<TorznabAttribute>
)

data class Enclosure(
    @JacksonXmlProperty(isAttribute = true, localName = "url") val url: String,
    @JacksonXmlProperty(isAttribute = true, localName = "length") val length: String,
    @JacksonXmlProperty(isAttribute = true, localName = "type") val type: String
)

data class TorznabAttribute(
    @JacksonXmlProperty(isAttribute = true, localName = "name") val name: String,
    @JacksonXmlProperty(isAttribute = true, localName = "value") val value: String
)
