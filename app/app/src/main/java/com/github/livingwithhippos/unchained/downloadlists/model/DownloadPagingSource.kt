package com.github.livingwithhippos.unchained.downloadlists.model

import androidx.paging.PagingSource
import com.github.livingwithhippos.unchained.base.model.repositories.CredentialsRepository
import com.github.livingwithhippos.unchained.base.model.repositories.DownloadRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

private const val DOWNLOAD_STARTING_PAGE_INDEX = 1


class DownloadPagingSource (
    private val downloadRepository: DownloadRepository,
    private val credentialsRepository: CredentialsRepository
) : PagingSource<Int, DownloadItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DownloadItem> {
        val page = params.key ?: DOWNLOAD_STARTING_PAGE_INDEX
        val token = credentialsRepository.getToken()
        if (token.length<5)
            throw IllegalArgumentException("Error loading token: $token")

        return try {
            val response = downloadRepository.getDownloads(token, null, page, params.loadSize)

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


}