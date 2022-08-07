package com.github.livingwithhippos.unchained.data.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.internal.Util

class InstantAvailabilityAdapter(
    moshi: Moshi
) : JsonAdapter<RdCache>() {
    private val options: JsonReader.Options = JsonReader.Options.of("rd")

    private val listOfMapOfStringTorrentFileAdapter: JsonAdapter<List<Map<String, TorrentFile>>> =
        moshi.adapter(
            Types.newParameterizedType(
                List::class.java,
                Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    TorrentFile::class.java
                )
            ),
            emptySet(), "rd"
        )

    override fun toString(): String = buildString(29) {
        append("GeneratedJsonAdapter(").append("RdCache").append(')')
    }

    override fun fromJson(reader: JsonReader): RdCache {
        var rd: List<Map<String, TorrentFile>>? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0 -> rd = listOfMapOfStringTorrentFileAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("rd", "rd", reader)
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        return RdCache(
            rd = rd ?: throw Util.missingProperty("rd", "rd", reader)
        )
    }

    override fun toJson(writer: JsonWriter, value_: RdCache?) {
        if (value_ == null) {
            throw NullPointerException("value_ was null! Wrap in .nullSafe() to write nullable values.")
        }
        writer.beginObject()
        writer.name("rd")
        listOfMapOfStringTorrentFileAdapter.toJson(writer, value_.rd)
        writer.endObject()
    }
}
