package com.github.livingwithhippos.unchained.data.repository

import android.net.Uri
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.livingwithhippos.unchained.data.model.jackett.Indexers
import com.github.livingwithhippos.unchained.data.model.torznab.Capabilities
import com.github.livingwithhippos.unchained.data.model.torznab.SearchRSS
import com.github.livingwithhippos.unchained.di.ClassicClient
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.xml.xmlMapper
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.net.URISyntaxException

class JackettRepository @Inject constructor(@ClassicClient private val client: OkHttpClient) {

    private fun getBasicBuilder(
        baseUrl: String,
        port: Int = 9117,
        apiKey: String,
        indexersFilter: String = "all",
        useSecureHttp: Boolean = false
    ): Uri.Builder? {
        var existingUri: Uri = try {
            Uri.parse("$baseUrl:$port")
        } catch (ex: Exception) {
            Timber.e(ex, "Error parsing url: $baseUrl:$port")
            return null
        }

        if (!(existingUri.scheme.equals(
                "http",
                ignoreCase = true
            ) || existingUri.scheme.equals("https", ignoreCase = true))
        ) {
            existingUri = Uri.parse("${if (useSecureHttp) "https" else "http"}://$baseUrl:$port")
        }
        val baseBuilder: Uri.Builder = try {
            val builder = Uri.Builder()
                .scheme(existingUri.scheme)
                .encodedAuthority(existingUri.encodedAuthority)

            builder
        } catch (ex: URISyntaxException) {
            Timber.e(ex, "Error parsing url: $baseUrl:$port")
            null
        } ?: return null
        return baseBuilder
            .appendPath("api")
            .appendPath("v2.0")
            .appendPath("indexers")
            .appendPath(indexersFilter)
            .appendPath("results")
            .appendPath("torznab")
            .appendPath("api")
            .appendQueryParameter("apikey", apiKey)
    }

    suspend fun performSearch(
        baseUrl: String,
        port: Int = 9117,
        indexer: String = "all",
        apiKey: String,
        query: String,
        categories: String = "",
        attributes: String? = null,
        extended: Boolean? = null,
        offset: Int? = null,
        limit: Int? = null,
        mediaType: JackettMediaType? = null,
        genre: JackettGenre? = null,
        year: String? = null,
        season: String? = null,
        episodes: String? = null,
        imdb: String? = null,
        album: String? = null,
        artist: String? = null,
        publisher: String? = null,
    ): EitherResult<Exception, SearchRSS> =
        withContext(Dispatchers.IO) {
            val builder = getBasicBuilder(baseUrl, port, apiKey, indexer)
                ?: return@withContext EitherResult.Failure(
                    IllegalArgumentException("Impossible to parse url")
                )

            if (mediaType == null)
                builder.appendQueryParameter("t", "search")
            else
                builder.appendQueryParameter("t", mediaType.value)

            // the search fails with no "cat" element, even if empty
            builder.appendQueryParameter("cat", categories)

            if (attributes != null)
                builder.appendQueryParameter("attrs", attributes)

            if (extended != null)
                builder.appendQueryParameter("extended", if (extended) "1" else "0")

            if (offset != null)
                builder.appendQueryParameter("offset", offset.toString())

            if (limit != null)
                builder.appendQueryParameter("limit", limit.toString())

            if (limit != null)
                builder.appendQueryParameter("year", year)

            if (season != null)
                builder.appendQueryParameter("season", season)

            if (episodes != null)
                builder.appendQueryParameter("ep", episodes)

            if (genre != null)
                builder.appendQueryParameter("genre", genre.value)

            if (imdb != null)
                builder.appendQueryParameter("imdbid", imdb)

            if (album != null)
                builder.appendQueryParameter("album", album)

            if (artist != null)
                builder.appendQueryParameter("artist", artist)

            if (publisher != null)
                builder.appendQueryParameter("publisher", publisher)

            builder.appendQueryParameter("q", query)

            val request = Request.Builder()
                .url(builder.build().toString())
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful)
                    return@withContext EitherResult.Failure(
                        IOException("Unexpected http code $response")
                    )
                val body: String = response.body?.string()
                    ?: return@withContext EitherResult.Failure(
                        IOException("Unexpected empty body")
                    )
                try {
                    val search = xmlMapper.readValue<SearchRSS>(body)
                    return@withContext EitherResult.Success(search)
                } catch (ex: Exception) {
                    Timber.e(ex, "Error parsing Search response")
                }

                return@withContext EitherResult.Failure(
                    IOException("Unexpected search failure")
                )
            }
        }


    suspend fun getCapabilities(
        baseUrl: String,
        port: Int = 9117,
        apiKey: String,
        indexer: String = "all"
    ): EitherResult<Exception, Capabilities> = withContext(Dispatchers.IO) {
        val builder = getBasicBuilder(baseUrl, port, apiKey, indexer)
            ?: return@withContext EitherResult.Failure(
                IllegalArgumentException("Impossible to parse url")
            )
        builder.appendQueryParameter("t", "caps")

        val request = Request.Builder()
            .url(builder.build().toString())
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful)
                return@withContext EitherResult.Failure(
                    IOException("Unexpected http code $response")
                )
            val body: String = response.body?.string()
                ?: return@withContext EitherResult.Failure(
                    IOException("Unexpected empty body")
                )
            try {
                val capabilities = xmlMapper.readValue<Capabilities>(body)
                return@withContext EitherResult.Success(capabilities)
            } catch (ex: Exception) {
                Timber.e(ex, "Error parsing Capabilities response")
            }

            return@withContext EitherResult.Failure(
                IOException("Unexpected capabilities failure")
            )
        }
    }

    suspend fun getValidIndexers(
        baseUrl: String,
        port: Int = 9117,
        apiKey: String,
        configured: Boolean = true,
    ): EitherResult<Exception, Indexers> = withContext(Dispatchers.IO) {
        try {
            val builder = getBasicBuilder(
                baseUrl,
                port,
                apiKey,
                indexersFilter = "!status:failing,test:passed"
            ) ?: return@withContext EitherResult.Failure(
                IllegalArgumentException("Impossible to parse url")
            )
            builder.appendQueryParameter("t", "indexers")

            if (configured)
                builder.appendQueryParameter("configured", "true")
            else
                builder.appendQueryParameter("configured", "false")

            val request = Request.Builder()
                .url(builder.build().toString())
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful)
                    return@withContext EitherResult.Failure(
                        IOException("Unexpected http code $response")
                    )
                val body: String = response.body?.string()
                    ?: return@withContext EitherResult.Failure(
                        IOException("Unexpected empty body")
                    )
                try {
                    val indexers = xmlMapper.readValue<Indexers>(body)
                    return@withContext EitherResult.Success(indexers)
                } catch (ex: Exception) {
                    Timber.e(ex, "Error parsing indexers response")
                }

                return@withContext EitherResult.Failure(
                    IOException("Unexpected indexers failure")
                )
            }

        } catch (ex: Exception) {
            Timber.e(ex, "Error getting indexers")
            return@withContext EitherResult.Failure(
                ex
            )
        }
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