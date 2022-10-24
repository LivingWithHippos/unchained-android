package com.github.livingwithhippos.unchained.data.model.cache

import android.os.Parcelable
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import kotlinx.parcelize.Parcelize
import timber.log.Timber

@Parcelize
data class InstantAvailability(
    val cachedTorrents: List<CachedTorrent>
) : Parcelable

@Parcelize
data class CachedTorrent(
    val btih: String,
    // i may have a list of providers, for now I always see only "rd"
    val cachedAlternatives: List<CachedAlternative>
) : Parcelable

@Parcelize
data class CachedAlternative(
    val cachedFiles: List<CachedFile>
) : Parcelable

@Parcelize
data class CachedFile(
    val id: Long,
    val fileName: String,
    val fileSize: Long
) : Parcelable

class CachedRequestAdapter : JsonAdapter<InstantAvailability>() {

    @FromJson
    override fun fromJson(reader: JsonReader): InstantAvailability {
        val cachedTorrents = mutableListOf<CachedTorrent>()
        reader.beginObject()
        // always available for cached and missing
        var key: String?
        while (reader.hasNext()) {
            // Start CachedTorrent
            key = reader.nextName()

            when (reader.peek()) {
                JsonReader.Token.BEGIN_OBJECT -> {
                    // cached content
                    reader.beginObject()
                    val cachedAlternatives = mutableListOf<CachedAlternative>()
                    // cache providers, still has to see something else beside "rd"
                    val provider = reader.nextName()
                    if (provider == "rd") {
                        // start list of CachedAlternative
                        reader.beginArray()
                        while (reader.hasNext()) {
                            reader.beginObject()
                            // start CachedFile list
                            val cachedFiles = mutableListOf<CachedFile>()
                            while (reader.hasNext()) {
                                val currId = reader.nextName().toLong()
                                reader.beginObject()
                                var fileName = ""
                                var fileSize = 0L
                                while (reader.hasNext()) {
                                    when (val token = reader.nextName()) {
                                        "filename" -> fileName = reader.nextString()
                                        "filesize" -> fileSize = reader.nextLong()
                                        else -> {
                                            Timber.d("skip inner token $token")
                                            reader.skipValue()
                                        }
                                    }
                                }
                                reader.endObject()
                                cachedFiles.add(CachedFile(currId, fileName, fileSize))
                            }
                            reader.endObject()
                            cachedAlternatives.add(
                                CachedAlternative(cachedFiles)
                            )
                        }
                        // finished list of CachedAlternatives
                        reader.endArray()
                    } else {
                        // check ??
                        Timber.d("Provider: $provider")
                    }
                    // finished CachedTorrent
                    reader.endObject()
                    cachedTorrents.add(CachedTorrent(key, cachedAlternatives))
                }
                JsonReader.Token.BEGIN_ARRAY -> {
                    // cache-less content
                    // evaluate if torrent without cache could be added to the output with an empty map
                    Timber.d("Skipping empty torrent: $key")
                    reader.skipValue()
                }
                else -> {
                    // ??
                    Timber.d("Skipped cachedTorrent")
                    reader.skipValue()
                }
            }
        }

        reader.endObject()
        return InstantAvailability(cachedTorrents)
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: InstantAvailability?) {
        TODO("Not yet implemented")
    }
}