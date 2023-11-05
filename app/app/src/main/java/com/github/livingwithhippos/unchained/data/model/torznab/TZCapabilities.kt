package com.github.livingwithhippos.unchained.data.model.torznab

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName ="caps")
data class Capabilities(
    @JacksonXmlProperty(localName = "server")
    val server: Server,
    @JacksonXmlProperty(localName = "limits")
    val limits: Limits,
    @JacksonXmlProperty(localName = "searching")
    val searching: Searching,
    @JacksonXmlProperty(localName = "categories")
    val categories: Categories
)

data class Server(
    @JacksonXmlProperty(isAttribute = true, localName = "title") val title: String
)

data class Limits(
    @JacksonXmlProperty(isAttribute = true, localName = "default") val default: Int,
    @JacksonXmlProperty(isAttribute = true, localName = "max") val max: Int
)

data class Searching(
    @JacksonXmlProperty(localName = "search") val search: CapsSearch,
    @JacksonXmlProperty(localName = "tv-search") val tvSearch: CapsSearch,
    @JacksonXmlProperty(localName = "movie-search") val movieSearch: CapsSearch,
    @JacksonXmlProperty(localName = "music-search") val musicSearch: CapsSearch,
    @JacksonXmlProperty(localName = "audio-search") val audioSearch: CapsSearch,
    @JacksonXmlProperty(localName = "book-search") val bookSearch: CapsSearch
)

data class CapsSearch(
    @JacksonXmlProperty(isAttribute = true, localName = "available") val available: String,
    @JacksonXmlProperty(isAttribute = true, localName = "supportedParams") val supportedParams: String,
    @JacksonXmlProperty(isAttribute = true, localName = "searchEngine") val searchEngine: String?
)

data class Categories(
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "category")
    val category: List<Category>
)

data class Category(
    @JacksonXmlProperty(isAttribute = true, localName = "id") val id: Int,
    @JacksonXmlProperty(isAttribute = true, localName = "name") val name: String,
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "subcat")
    val subcat: List<SubCategory>?
)

data class SubCategory(
    @JacksonXmlProperty(isAttribute = true, localName = "id") val id: Int,
    @JacksonXmlProperty(isAttribute = true, localName = "name") val name: String
)
