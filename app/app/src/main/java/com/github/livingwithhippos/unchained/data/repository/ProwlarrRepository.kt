package com.github.livingwithhippos.unchained.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.github.livingwithhippos.unchained.data.local.CompleteRemoteService
import com.github.livingwithhippos.unchained.data.model.ProwlarrResponse
import com.github.livingwithhippos.unchained.data.model.prowlarrToScrapedItems
import com.github.livingwithhippos.unchained.di.ClassicClient
import com.github.livingwithhippos.unchained.plugins.ParserResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

class ProwlarrRepository
@Inject
constructor(
    @param:ClassicClient private val client: OkHttpClient,
    @ApplicationContext private val applicationContext: Context,
) {
    // todo: implement POST search with json body, as it is more flexible and allows to bypass url
    // length limits
    //  https://prowlarr.com/docs/api/#/Search/post_api_v1_search

    private val json = Json { ignoreUnknownKeys = true }

    private fun getBasicApi(service: CompleteRemoteService): Uri.Builder? {
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
                .appendPath("v1")
                .appendPath("search")
                .appendQueryParameter("apikey", service.apiToken)
        } catch (ex: Exception) {
            Timber.e(ex, "Error parsing url from $service")
            null
        }
    }

    fun performSearch(
        service: CompleteRemoteService,
        query: String,
        indexers: List<Int> = emptyList(),
        categories: List<Int> = emptyList(),
        offset: Int? = null,
        limit: Int? = null,
    ) =
        flow {
                val builder = getBasicApi(service)
                if (builder == null) {
                    emit(ParserResult.SourceError)
                    return@flow
                }

                builder.appendQueryParameter("query", query)
                if (indexers.isNotEmpty()) {
                    indexers.forEach { builder.appendQueryParameter("indexerIds", it.toString()) }
                }
                if (categories.isNotEmpty()) {
                    categories.forEach { builder.appendQueryParameter("categories", it.toString()) }
                }

                if (offset != null) builder.appendQueryParameter("offset", offset.toString())
                if (limit != null) builder.appendQueryParameter("limit", limit.toString())

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
                        val results: List<ProwlarrResponse> = json.decodeFromString(body)
                        val items = prowlarrToScrapedItems(applicationContext, results)
                        emit(ParserResult.Results(items))
                        return@flow
                    } catch (ex: Exception) {
                        Timber.e(ex, "Error parsing Prowlarr search response")
                    }

                    emit(ParserResult.SourceError)
                }
            }
            .flowOn(Dispatchers.IO)
}
