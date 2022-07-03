package com.github.livingwithhippos.unchained.data.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.squareup.moshi.internal.Util
import java.lang.reflect.Type


class CachedTorrentAdapter(
    private val rdCacheAdapter: JsonAdapter<RdCache>
) : JsonAdapter<Map<String, RdCache>>() {

    @FromJson
    override fun fromJson(reader: JsonReader): Map<String, RdCache> {
        val cache = mutableMapOf<String, RdCache>()
        var rd: RdCache?
        var key: String?

        reader.beginObject()
        while (reader.hasNext()) {
            key = reader.nextName()
            // todo: these file trees are nested, add support for them
            when (reader.peek()) {
                JsonReader.Token.BEGIN_OBJECT -> {
                    rd = rdCacheAdapter.fromJson(reader)
                        ?: throw Util.unexpectedNull("rd", "rd", reader)
                    cache[key] = rd
                }
                JsonReader.Token.BEGIN_ARRAY -> {
                    // evaluate if torrent without cache could be added to the output with an empty map
                    reader.skipValue()
                }
                else -> {
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        return cache
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value_: Map<String, RdCache>?): Unit {
        if (value_ == null) {
            throw NullPointerException("value_ was null! Wrap in .nullSafe() to write nullable values.")
        }
        writer.beginObject()
        value_.forEach { (key, cache) ->
            writer.name(key)
            rdCacheAdapter.toJson(writer, cache)
        }
        writer.endObject()
    }

    companion object {

        var type: Type = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            RdCache::class.java,
        )

        fun <T> factory(): Factory {
            return object : Factory {
                override fun create(
                    requestedType: Type,
                    annotations: Set<Annotation?>,
                    moshi: Moshi
                ): JsonAdapter<Map<String, RdCache>>? {
                    if (type != requestedType) return null
                    val rdCacheAdapter: JsonAdapter<RdCache> = InstantAvailabilityAdapter(moshi)
                    return CachedTorrentAdapter(rdCacheAdapter)
                }
            }
        }
    }
}