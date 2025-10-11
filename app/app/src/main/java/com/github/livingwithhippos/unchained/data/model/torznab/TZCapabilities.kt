package com.github.livingwithhippos.unchained.data.model.torznab

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "caps")
data class Capabilities(
    @param:JacksonXmlProperty(localName = "server") val server: Server,
    @param:JacksonXmlProperty(localName = "limits") val limits: Limits,
    @param:JacksonXmlProperty(localName = "searching") val searching: Searching,
    @param:JacksonXmlProperty(localName = "categories") val categories: Categories,
)

data class Server(
    @param:JacksonXmlProperty(isAttribute = true, localName = "title") val title: String
)

data class Limits(
    @param:JacksonXmlProperty(isAttribute = true, localName = "default") val default: Int,
    @param:JacksonXmlProperty(isAttribute = true, localName = "max") val max: Int,
)

data class Searching(
    @param:JacksonXmlProperty(localName = "search") val search: CapsSearch,
    @param:JacksonXmlProperty(localName = "tv-search") val tvSearch: CapsSearch,
    @param:JacksonXmlProperty(localName = "movie-search") val movieSearch: CapsSearch,
    @param:JacksonXmlProperty(localName = "music-search") val musicSearch: CapsSearch,
    @param:JacksonXmlProperty(localName = "audio-search") val audioSearch: CapsSearch,
    @param:JacksonXmlProperty(localName = "book-search") val bookSearch: CapsSearch,
)

data class CapsSearch(
    @param:JacksonXmlProperty(isAttribute = true, localName = "available") val available: String,
    @param:JacksonXmlProperty(isAttribute = true, localName = "supportedParams")
    val supportedParams: String,
    @param:JacksonXmlProperty(isAttribute = true, localName = "searchEngine")
    val searchEngine: String?,
)

data class Categories(
    @param:JacksonXmlElementWrapper(useWrapping = false)
    @param:JacksonXmlProperty(localName = "category")
    val category: List<Category>
)

data class Category(
    @param:JacksonXmlProperty(isAttribute = true, localName = "id") val id: Int,
    @param:JacksonXmlProperty(isAttribute = true, localName = "name") val name: String,
    @param:JacksonXmlElementWrapper(useWrapping = false)
    @param:JacksonXmlProperty(localName = "subcat")
    val subcat: List<SubCategory>?,
)

data class SubCategory(
    @param:JacksonXmlProperty(isAttribute = true, localName = "id") val id: Int,
    @param:JacksonXmlProperty(isAttribute = true, localName = "name") val name: String,
)
