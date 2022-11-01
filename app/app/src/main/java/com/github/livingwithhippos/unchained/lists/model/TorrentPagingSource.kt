import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.repository.TorrentsRepository
import retrofit2.HttpException
import java.io.IOException

private const val TORRENT_STARTING_PAGE_INDEX = 1

/**
 * Paging Source Using Paging V3. See https://github.com/android/architecture-components-samples/tree/main/PagingWithNetworkSample for a sample
 */
class TorrentPagingSource(
    private val torrentsRepository: TorrentsRepository,
    private val query: String,
) : PagingSource<Int, TorrentItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TorrentItem> {
        val page = params.key ?: TORRENT_STARTING_PAGE_INDEX

        return try {
            val response =
                if (query.isBlank())
                    torrentsRepository.getTorrentsList(null, page, params.loadSize)
                else
                    torrentsRepository.getTorrentsList(null, page, params.loadSize)
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
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}
