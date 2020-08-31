package com.github.livingwithhippos.unchained.base.model.repositories

import androidx.lifecycle.LiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.liveData
import com.github.livingwithhippos.unchained.downloadlists.model.DownloadApiHelper
import com.github.livingwithhippos.unchained.downloadlists.model.DownloadItem
import com.github.livingwithhippos.unchained.downloadlists.model.DownloadList
import com.github.livingwithhippos.unchained.downloadlists.model.DownloadPagingSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DownloadRepository @Inject constructor(private val downloadApiHelper: DownloadApiHelper) :
    BaseRepository() {
    suspend fun getDownloads(
        token: String,
        offset: Int = 0,
        page: Int = 1,
        limit: Int = 5
    ): List<DownloadItem> {

        val downloadResponse = safeApiCall(
            call = { downloadApiHelper.getDownloads("Bearer $token", offset, page, limit) },
            errorMessage = "Error Fetching User Info"
        )

        return downloadResponse ?: emptyList<DownloadItem>()

    }

    fun getDownloadListLiveData(query: String): LiveData<PagingData<DownloadList>> {
        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { DownloadPagingSource(downloadApiHelper, query) }
        ).liveData
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 50
    }

}