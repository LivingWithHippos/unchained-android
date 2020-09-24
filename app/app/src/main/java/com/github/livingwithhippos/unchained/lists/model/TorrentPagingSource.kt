package com.github.livingwithhippos.unchained.lists.model

import androidx.paging.PagingSource
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.TorrentsRepository
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import retrofit2.HttpException
import java.io.IOException

private const val TORRENT_STARTING_PAGE_INDEX = 1

class TorrentPagingSource(
    private val torrentsRepository: TorrentsRepository,
    private val credentialsRepository: CredentialsRepository
) : PagingSource<Int, TorrentItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TorrentItem> {
        val page = params.key ?: TORRENT_STARTING_PAGE_INDEX
        val token = credentialsRepository.getToken()
        if (token.length < 5)
            throw IllegalArgumentException("Error loading token: $token")

        return try {
            val response = torrentsRepository.getTorrentsList(token, null, page, params.loadSize)

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

}