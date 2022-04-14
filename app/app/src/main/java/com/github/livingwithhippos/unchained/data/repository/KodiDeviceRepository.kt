package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.KodiDeviceDao
import com.github.livingwithhippos.unchained.data.model.KodiDevice
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class KodiDeviceRepository @Inject constructor(
    private val kodiDeviceDao: KodiDeviceDao
) : BaseRepository() {

    val devicesFlow: Flow<List<KodiDevice>>
        get() = kodiDeviceDao.getAllDevicesFlow()


    suspend fun getDevices(): List<KodiDevice> {
        return kodiDeviceDao.getAllDevices()
    }

    suspend fun add(device: KodiDevice): Long {
        return kodiDeviceDao.insert(device)
    }

    suspend fun remove(device: KodiDevice) {
        kodiDeviceDao.remove(device.name)
    }

    suspend fun addAll(devices: List<KodiDevice>) {
        kodiDeviceDao.insertAll(devices)
    }

    suspend fun clearAll() {
        kodiDeviceDao.deleteAll()
    }

    suspend fun getDefault(): KodiDevice? {
        return kodiDeviceDao.getDefault()
    }

    suspend fun clearDefaultsExcept(device: KodiDevice) {
        kodiDeviceDao.resetDefaultsExcept(device.name)
    }

    suspend fun setDefault(device: KodiDevice) {
        kodiDeviceDao.resetDefaultsExcept(device.name)
        kodiDeviceDao.setDefault(device.name)
    }
}