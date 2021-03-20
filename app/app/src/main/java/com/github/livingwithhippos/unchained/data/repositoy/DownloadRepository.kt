package com.github.livingwithhippos.unchained.data.repositoy

import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.github.livingwithhippos.unchained.data.local.CredentialsDao
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.remote.DownloadApiHelper
import com.github.livingwithhippos.unchained.lists.model.DownloadPagingSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DownloadRepository @Inject constructor(private val downloadApiHelper: DownloadApiHelper, private val credentialsDao: CredentialsDao) :
    BaseRepository() {
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

    suspend fun deleteDownload(token: String, id: String): Unit? {

        val response = safeApiCall(
            call = {
                downloadApiHelper.deleteDownload(
                    token = "Bearer $token",
                    id = id
                )
            },
            errorMessage = "Error deleting download"
        )

        return response
    }

    fun getDownloadsResultStream(query: String): Flow<PagingData<DownloadItem>> {
        return Pager(
            config = PagingConfig(pageSize = 50, initialLoadSize = 100),
            pagingSourceFactory = {
                DownloadPagingSource(
                    downloadApiHelper,
                    credentialsDao,
                    query
                )
            })
            .flow
    }

}