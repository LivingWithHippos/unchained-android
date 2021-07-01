package com.github.livingwithhippos.unchained.lists.model

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.DownloadRepository
import com.github.livingwithhippos.unchained.folderlist.view.FolderListFragment
import retrofit2.HttpException
import java.io.IOException

private const val DOWNLOAD_STARTING_PAGE_INDEX = 1

class DownloadPagingSource(
    private val downloadRepository: DownloadRepository,
    private val credentialsRepository: CredentialsRepository,
    private val query: String,
    private val sort: String?,
) : PagingSource<Int, DownloadItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DownloadItem> {
        val page = params.key ?: DOWNLOAD_STARTING_PAGE_INDEX
        val token = credentialsRepository.getToken()
        if (token.length < 5)
            throw IllegalArgumentException("Error loading token: $token")

        return try {
            var response = downloadRepository.getDownloads(token, null, page, params.loadSize)
            if (query.isNotBlank())
                response = response.filter { it.filename.contains(query, ignoreCase = true) }
            when (sort) {
                FolderListFragment.TAG_DEFAULT_SORT -> {}
                FolderListFragment.TAG_SORT_AZ -> {
                    response = response.sortedBy { item ->
                        item.filename
                    }
                }
                FolderListFragment.TAG_SORT_ZA -> {
                    response = response.sortedByDescending { item ->
                        item.filename
                    }
                }
                FolderListFragment.TAG_SORT_SIZE_DESC -> {
                    response = response.sortedByDescending { item ->
                        item.fileSize
                    }
                }
                FolderListFragment.TAG_SORT_SIZE_ASC -> {
                    response = response.sortedBy { item ->
                        item.fileSize
                    }
                }
                else -> {}
            }


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
