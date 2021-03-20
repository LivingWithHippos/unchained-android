package com.github.livingwithhippos.unchained.lists.model

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.TorrentsRepository
import retrofit2.HttpException
import java.io.IOException

private const val TORRENT_STARTING_PAGE_INDEX = 1

class TorrentPagingSource(
    private val torrentsRepository: TorrentsRepository,
    private val credentialsRepository: CredentialsRepository,
    private val query: String,
) : PagingSource<Int, TorrentItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TorrentItem> {
        val page = params.key ?: TORRENT_STARTING_PAGE_INDEX
        val token = credentialsRepository.getToken()
        if (token.length < 5)
            throw IllegalArgumentException("Error loading token: $token")

        return try {
            val response =
                if (query.isBlank())
                    torrentsRepository.getTorrentsList(token, null, page, params.loadSize)
                else
                    torrentsRepository.getTorrentsList(token, null, page, params.loadSize)
                        .filter { it.filename.contains(query, ignoreCase = true) }

            LoadResult.Page(
                data = response,
                prevKey = if (page == TORRENT_STARTING_PAGE_INDEX) null else page - 1,
                nextKey = if (response.isEmpty()) null else page + 1
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }

    override val jumpingSupported: Boolean = true

    override fun getRefreshKey(state: PagingState<Int, TorrentItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
        // This loads starting from previous page, but since PagingConfig.initialLoadSize spans
        // multiple pages, the initial load will still load items centered around
        // anchorPosition. This also prevents needing to immediately launch prepend due to
        // prefetchDistance.
        state.closestPageToPosition(anchorPosition)?.prevKey
    }
    }

}