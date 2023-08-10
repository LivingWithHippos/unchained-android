package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.data.local.RemoteDeviceDao
import javax.inject.Inject

class RemoteDeviceRepository @Inject constructor(private val remoteDeviceDao: RemoteDeviceDao) {

    suspend fun insert(device: RemoteDevice) = remoteDeviceDao.insert(device)

    suspend fun insertAll(list: List<RemoteDevice>) = remoteDeviceDao.insertAll(list)

    suspend fun getAllDevices() = remoteDeviceDao.getAllDevices()

    suspend fun deleteAll() = remoteDeviceDao.deleteAll()

    suspend fun remove(id: Int) = remoteDeviceDao.remove(id)

    suspend fun getDefault() = remoteDeviceDao.getDefault()

    suspend fun setDefault(id: Int) = remoteDeviceDao.setDefault(id)

    suspend fun resetDefaults() = remoteDeviceDao.resetDefaults()
}