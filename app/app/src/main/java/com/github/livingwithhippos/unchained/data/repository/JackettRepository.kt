package com.github.livingwithhippos.unchained.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.github.livingwithhippos.unchained.data.local.CompleteRemoteService
import com.github.livingwithhippos.unchained.data.model.jackett.Indexers
import com.github.livingwithhippos.unchained.data.model.jackett.parseIndexers
import com.github.livingwithhippos.unchained.data.model.torznab.Capabilities
import com.github.livingwithhippos.unchained.data.model.torznab.SearchRSS
import com.github.livingwithhippos.unchained.data.model.torznab.parseCapabilities
import com.github.livingwithhippos.unchained.data.model.torznab.parseSearchRss
import com.github.livingwithhippos.unchained.data.model.torznab.rssToScrapedItems
import com.github.livingwithhippos.unchained.di.ClassicClient
import com.github.livingwithhippos.unchained.plugins.ParserResult
import com.github.livingwithhippos.unchained.utilities.EitherResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

class JackettRepository @Inject constructor(
    @param:ClassicClient private val client: OkHttpClient,
    @ApplicationContext private val applicationContext: Context,
) {

    private fun getBasicApi(
        service: CompleteRemoteService,
        indexersFilter: String = "all",
    ): Uri.Builder? {
        Timber.d(service.address)
        return try {
            val baseUri = service.address.toUri()
            if (baseUri.scheme.isNullOrBlank() || baseUri.encodedAuthority.isNullOrBlank()) {
                return null
            }

            baseUri
                .buildUpon()
                .encodedQuery(null)
                .fragment(null)
                .appendPath("api")
                .appendPath("v2.0")
                .appendPath("indexers")
                .appendPath(indexersFilter)
                .appendPath("results")
                .appendPath("torznab")
                .appendPath("api")
                .appendQueryParameter("apikey", service.apiToken)
        } catch (ex: Exception) {
            Timber.e(ex, "Error parsing url from $service")
            null
        }
    }

    fun performSearch(
        service: CompleteRemoteService,
        indexer: String = "all",
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
    ) =
        flow {
                val builder = getBasicApi(service, indexer)
                if (builder == null) {
                    emit(ParserResult.SourceError)
                    return@flow
                }
                if (mediaType == null) builder.appendQueryParameter("t", "search")
                else builder.appendQueryParameter("t", mediaType.value)

                // the search fails with no "cat" element, even if empty
                builder.appendQueryParameter("cat", categories)

                if (attributes != null) builder.appendQueryParameter("attrs", attributes)

                if (extended != null)
                    builder.appendQueryParameter("extended", if (extended) "1" else "0")

                if (offset != null) builder.appendQueryParameter("offset", offset.toString())
                if (limit != null) builder.appendQueryParameter("limit", limit.toString())
                if (year != null) builder.appendQueryParameter("year", year)
                if (season != null) builder.appendQueryParameter("season", season)
                if (episodes != null) builder.appendQueryParameter("ep", episodes)
                if (genre != null) builder.appendQueryParameter("genre", genre.value)
                if (imdb != null) builder.appendQueryParameter("imdbid", imdb)
                if (album != null) builder.appendQueryParameter("album", album)
                if (artist != null) builder.appendQueryParameter("artist", artist)
                if (publisher != null) builder.appendQueryParameter("publisher", publisher)

                builder.appendQueryParameter("q", query)

                val request = Request.Builder().url(builder.build().toString()).build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        emit(ParserResult.SourceError)
                        return@flow
                    }
                    if (response.body == null) {
                        emit(ParserResult.NetworkBodyError)
                        return@flow
                    }
                    val body: String = response.body.string()
                    try {
                        val search: SearchRSS = parseSearchRss(body)
                        val items = rssToScrapedItems(applicationContext,search)
                        emit(ParserResult.Results(items.first))
                        return@flow
                    } catch (ex: Exception) {
                        Timber.e(ex, "Error parsing Search response")
                    }

                    emit(ParserResult.SourceError)
                }
            }
            .flowOn(Dispatchers.IO)

    suspend fun getCapabilities(
        service: CompleteRemoteService,
        indexer: String = "all",
    ): EitherResult<Exception, Capabilities> =
        withContext(Dispatchers.IO) {
            val builder =
                getBasicApi(service, indexer)
                    ?: return@withContext EitherResult.Failure(
                        IllegalArgumentException("Impossible to parse url")
                    )
            builder.appendQueryParameter("t", "caps")

            val request = Request.Builder().url(builder.build().toString()).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful)
                    return@withContext EitherResult.Failure(
                        IOException("Unexpected http code $response")
                    )
                val body: String =
                    response.body?.string()
                        ?: return@withContext EitherResult.Failure(
                            IOException("Unexpected empty body")
                        )
                try {
                    val capabilities: Capabilities =
                        parseCapabilities(body)
                            ?: return@withContext EitherResult.Failure(
                                IOException("Unexpected empty capabilities")
                            )
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
        service: CompleteRemoteService,
        configured: Boolean = true,
    ): EitherResult<Exception, Indexers> =
        withContext(Dispatchers.IO) {
            try {
                val builder =
                    getBasicApi(service, indexersFilter = "!status:failing,test:passed")
                        ?: return@withContext EitherResult.Failure(
                            IllegalArgumentException("Impossible to parse url")
                        )
                builder.appendQueryParameter("t", "indexers")

                if (configured) builder.appendQueryParameter("configured", "true")
                else builder.appendQueryParameter("configured", "false")

                val request = Request.Builder().url(builder.build().toString()).build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful)
                        return@withContext EitherResult.Failure(
                            IOException("Unexpected http code $response")
                        )
                    val body: String =
                        response.body?.string()
                            ?: return@withContext EitherResult.Failure(
                                IOException("Unexpected empty body")
                            )
                    try {
                        val indexers: Indexers =
                            parseIndexers(body)
                                ?: return@withContext EitherResult.Failure(
                                    IOException("Unexpected empty indexers")
                                )
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
                return@withContext EitherResult.Failure(ex)
            }
        }
}

enum class JackettMediaType(val value: String) {
    ALL("search"),
    MOVIE("movie"),
    TV("tvsearch"),
    MUSIC("music"),
    BOOK("book"),
}

enum class JackettGenre(val value: String) {
    COMEDY("comedy"),
    HORROR("horror"),
}
