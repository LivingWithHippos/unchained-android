package com.github.livingwithhippos.unchained.lists.model

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.livingwithhippos.unchained.data.local.CredentialsDao
import com.github.livingwithhippos.unchained.data.model.Credentials
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.NetworkResponse
import com.github.livingwithhippos.unchained.data.remote.DownloadApiHelper
import com.github.livingwithhippos.unchained.utilities.PRIVATE_TOKEN
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.io.IOException

private const val DOWNLOAD_STARTING_PAGE_INDEX = 1


class DownloadPagingSource(
    private val downloadApiHelper: DownloadApiHelper,
    private val credentialsDao: CredentialsDao,
    private val query: String,
) : PagingSource<Int, DownloadItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DownloadItem> {
        val page = params.key ?: DOWNLOAD_STARTING_PAGE_INDEX
        val token = getFirstCredentials()?.accessToken ?: ""
        if (token.length < 5)
            throw IllegalArgumentException("Error loading token: $token")

        return try {
            val response =
                if (query.isBlank())
                    getDownloads(token, null, page, params.loadSize)
                else
                    getDownloads(token, null, page, params.loadSize)
                        .filter { it.filename.contains(query, ignoreCase = true) }

            LoadResult.Page(
                data = response,
                prevKey = if (page == DOWNLOAD_STARTING_PAGE_INDEX) null else page - 1,
                nextKey = if (response.isEmpty()) null else page + 1
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, DownloadItem>): Int? {
        return state.anchorPosition
    }

    suspend fun getFirstCredentials(): Credentials? {
        val credentials = credentialsDao.getCompleteCredentials()
        return credentials
            // return private credentials first
            .firstOrNull { it.refreshToken == PRIVATE_TOKEN }
        // open source credentials second
            ?: credentials.firstOrNull()
    }

    suspend fun getDownloads(
        token: String,
        offset: Int?,
        page: Int = 1,
        limit: Int = 50
    ): List<DownloadItem> {

        val downloadResponse = safeApiCall(
            call = { downloadApiHelper.getDownloads("Bearer $token", offset, page, limit) },
            errorMessage = "Error Fetching User Info"
        )

        return downloadResponse ?: emptyList()

    }

    suspend fun <T : Any> safeApiCall(call: suspend () -> Response<T>, errorMessage: String): T? {

        val result: NetworkResponse<T> = safeApiResult(call, errorMessage)
        var data: T? = null

        when (result) {
            is NetworkResponse.Success ->
                data = result.data
            is NetworkResponse.SuccessEmptyBody ->
                Timber.d("Successful call with empty body : ${result.code}")
            is NetworkResponse.Error ->
                Timber.d(errorMessage)
        }

        return data
    }

    private suspend fun <T : Any> safeApiResult(
        call: suspend () -> Response<T>,
        errorMessage: String
    ): NetworkResponse<T> {
        val response = call.invoke()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null)
                return NetworkResponse.Success(body)
            else
                return NetworkResponse.SuccessEmptyBody(response.code())
        }

        return NetworkResponse.Error(IOException("Error Occurred while getting api result, error : $errorMessage"))
    }

}