package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.CompleteRemoteService
import com.github.livingwithhippos.unchained.data.local.CompleteRemoteServiceDao
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ServiceRepository @Inject constructor(private val serviceDao: CompleteRemoteServiceDao) {

    suspend fun insertService(service: CompleteRemoteService): Long =
        serviceDao.insertService(service)

    suspend fun upsertService(service: CompleteRemoteService): Long =
        serviceDao.upsertService(service)

    suspend fun deleteService(service: CompleteRemoteService) = serviceDao.deleteService(service)

    suspend fun deleteService(serviceID: Int) = serviceDao.deleteService(serviceID)

    suspend fun insertAllServices(list: List<CompleteRemoteService>): List<Long> =
        serviceDao.insertAllServices(list)

    suspend fun getServices(): List<CompleteRemoteService> = serviceDao.getServices()

    suspend fun getServicesTypes(types: List<Int>): List<CompleteRemoteService> =
        serviceDao.getServicesTypes(types)

    fun getServicesTypesFlow(types: List<Int>): Flow<List<CompleteRemoteService>> =
        serviceDao.getServicesTypesFlow(types)

    suspend fun getServiceIDByRow(rowId: Long): Int? = serviceDao.getServiceIDByRow(rowId)

    suspend fun getService(serviceID: Int): CompleteRemoteService? =
        serviceDao.getService(serviceID)

    suspend fun deleteAll() = serviceDao.deleteAll()

    suspend fun removeService(id: Int) = serviceDao.removeService(id)

    suspend fun getDefaultService(): RemoteDevice? = serviceDao.getDefaultService()

    suspend fun setDefaultService(id: Int) = serviceDao.setDefaultService(id)

    suspend fun setDefault(name: String) = serviceDao.setDefault(name)

    suspend fun enableService(id: Int, enabled: Boolean) = serviceDao.enableService(id, enabled)

    suspend fun getEnabledServicesTypes(types: List<Int>): List<CompleteRemoteService> =
        withContext(Dispatchers.IO) {
            return@withContext serviceDao.getEnabledServicesTypes(types)
        }
}
