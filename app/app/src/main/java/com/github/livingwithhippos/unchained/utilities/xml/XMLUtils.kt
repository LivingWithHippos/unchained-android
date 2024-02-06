package com.github.livingwithhippos.unchained.utilities.xml

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper

/*
internal val xmlMapper: ObjectMapper = XmlMapper(JacksonXmlModule().apply {
    setDefaultUseWrapper(false)
}).registerKotlinModule()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

internal val xmlMapper = XmlMapper.builder(XmlFactory(
    WstxInputFactory(),
    WstxOutputFactory()
))
    .defaultUseWrapper(false)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build()
 */

internal val xmlMapper =
    XmlMapper.builder()
        .defaultUseWrapper(false)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()
