package com.github.livingwithhippos.unchained.remotedevice.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.data.model.User
import com.github.livingwithhippos.unchained.data.repository.RemoteDeviceRepository
import com.github.livingwithhippos.unchained.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel
@Inject
constructor(private val deviceRepository: RemoteDeviceRepository) :
    ViewModel() {

    val deviceLiveData = MutableLiveData<List<RemoteDevice>>()

    fun fetchRemoteDevices() {

        viewModelScope.launch {
            deviceLiveData.postValue(
                deviceRepository.getAllDevices()
            )
        }
    }
}
