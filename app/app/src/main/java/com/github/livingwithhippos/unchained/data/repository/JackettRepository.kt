package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.di.ClassicClient
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.addHttpScheme
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber

class JackettRepository @Inject constructor(@ClassicClient private val client: OkHttpClient) {

    suspend fun performSearch(
        baseUrl: String,
        port: Int = 9117,
        apiKey: String,
        query: String,
        mediaType: JackettMediaType? = null,
        categories: String? = null,
        genre: JackettGenre? = null,
        year: String? = null,
        season: String? = null,
        episodes: String? = null,
        imdb: String? = null,
        album: String? = null,
        artist: String? = null,
        publisher: String? = null,
    ): EitherResult<Exception, Boolean> =
        withContext(Dispatchers.IO) {
            val sb = StringBuilder("${addHttpScheme(baseUrl)}:$port/api?apikey=$apiKey")

            if (mediaType == null)
                sb.append("&t=search")
            else
                sb.append("&t=${mediaType.value}")

            if (categories != null)
                sb.append("&cat=$categories")

            if (year != null)
                sb.append("&year=$year")

            if (season != null)
                sb.append("&season=$season")

            if (episodes != null)
                sb.append("&ep=$episodes")

            if (genre != null)
                sb.append("&genre=${genre.value}")

            if (imdb != null)
                sb.append("&imdbid=$imdb")

            if (album != null)
                sb.append("&album=$album")

            if (artist != null)
                sb.append("&artist=$artist")

            if (publisher != null)
                sb.append("&publisher=$publisher")

            sb.append("&q=$query")

            val request = Request.Builder()
                .url(sb.toString())
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful)
                    return@withContext EitherResult.Failure(
                        IOException("Unexpected http code $response")
                    )

                Timber.d(response.body!!.string())
                return@withContext EitherResult.Success(true)
            }
        }


    suspend fun getCapabilities(
        baseUrl: String,
        port: Int = 9117,
        apiKey: String,
    ) {
        val sb = StringBuilder("${addHttpScheme(baseUrl)}:$port/api?t=caps")

    }

    private fun parseXmlToJsonObject(xml: String) : String {
        var jsonObj: JSONObject? = null
        try {
            jsonObj = XML.toJSONObject(xml)
        } catch (e: JSONException) {
            Log.e("JSON exception", e.message)
            e.printStackTrace()
        }

        return jsonObj.toString()
    }
}



enum class JackettMediaType(val value: String) {
    ALL("search"),
    MOVIE("movie"),
    TV("tvsearch"),
    MUSIC("music"),
    BOOK("book")
}

enum class JackettGenre(val value: String) {
    COMEDY("comedy"),
    HORROR("horror"),
}