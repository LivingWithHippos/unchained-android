package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.data.local.RemoteDeviceDao
import com.github.livingwithhippos.unchained.data.local.RemoteService
import javax.inject.Inject

class RemoteDeviceRepository @Inject constructor(private val remoteDeviceDao: RemoteDeviceDao) {

    suspend fun getAllDevices() = remoteDeviceDao.getAllDevices()

    suspend fun deleteAll() = remoteDeviceDao.deleteAll()

    suspend fun getDeviceServices(deviceId: Int): List<RemoteService> =
        remoteDeviceDao.getDeviceServices(deviceId)

    suspend fun getDevicesAndServices(): Map<RemoteDevice, List<RemoteService>> =
        remoteDeviceDao.getDevicesAndServices()

    suspend fun insertDevice(device: RemoteDevice): Long = remoteDeviceDao.insertDevice(device)
    suspend fun insertService(service: RemoteService): Long = remoteDeviceDao.insertService(service)
    suspend fun getDeviceIDByRow(rowId: Long): Int? = remoteDeviceDao.getDeviceIDByRow(rowId)
}