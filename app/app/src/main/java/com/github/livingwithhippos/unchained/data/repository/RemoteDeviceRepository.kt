package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.data.local.RemoteDeviceDao
import javax.inject.Inject

class RemoteDeviceRepository @Inject constructor(private val remoteDeviceDao: RemoteDeviceDao) {

    suspend fun getAllDevices() = remoteDeviceDao.getAllDevices()

    suspend fun deleteAll() = remoteDeviceDao.deleteAll()
}