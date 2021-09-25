package com.github.livingwithhippos.unchained.lists.model

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.repository.DownloadRepository
import retrofit2.HttpException
import java.io.IOException

private const val DOWNLOAD_STARTING_PAGE_INDEX = 1

class DownloadPagingSource(
    private val downloadRepository: DownloadRepository,
    private val protoStore: ProtoStore,
    private val query: String,
) : PagingSource<Int, DownloadItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DownloadItem> {
        val page = params.key ?: DOWNLOAD_STARTING_PAGE_INDEX
        val token = protoStore.getCredentials().accessToken
        if (token.length < 5)
            throw IllegalArgumentException("Error loading token: $token")

        return try {
            val response =
                if (query.isBlank())
                    downloadRepository.getDownloads(token, null, page, params.loadSize)
                else
                    downloadRepository.getDownloads(token, null, page, params.loadSize)
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
        return state.anchorPosition?.let { anchorPosition ->
            // This loads starting from previous page, but since PagingConfig.initialLoadSize spans
            // multiple pages, the initial load will still load items centered around
            // anchorPosition. This also prevents needing to immediately launch prepend due to
            // prefetchDistance.
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}
