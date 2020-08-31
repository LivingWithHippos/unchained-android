package com.github.livingwithhippos.unchained.downloadlists.model

import androidx.paging.PagingSource
import retrofit2.HttpException
import java.io.IOException

private const val DOWNLOAD_STARTING_PAGE_INDEX = 1
private const val DOWNLOAD_OFFSET= 0

class DownloadPagingSource(
    private val downloadApiHelper: DownloadApiHelper,
    private val token: String
) : PagingSource<Int, DownloadItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DownloadItem> {
        val position = params.key ?: DOWNLOAD_STARTING_PAGE_INDEX

        return try {
            val response = downloadApiHelper.getDownloadsDirectly(token, DOWNLOAD_OFFSET, position, params.loadSize)
            val downloads = response.downloads
            LoadResult.Page(
                data = downloads,
                prevKey = if (position == DOWNLOAD_STARTING_PAGE_INDEX) null else position - 1,
                nextKey = if (downloads.isEmpty()) null else position + 1
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }
}