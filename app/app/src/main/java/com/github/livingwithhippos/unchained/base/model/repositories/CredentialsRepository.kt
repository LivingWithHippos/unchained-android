package com.github.livingwithhippos.unchained.base.model.repositories

import com.github.livingwithhippos.unchained.base.model.dao.CredentialsDao
import com.github.livingwithhippos.unchained.base.model.entities.Credentials
import javax.inject.Inject

// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class CredentialsRepository @Inject constructor(private val credentialsDao: CredentialsDao) {

    suspend fun getAllCredentials() = credentialsDao.getAllCredentials()

    suspend fun getCredentials(deviceCode: String) = credentialsDao.getCredentials(deviceCode)

    suspend fun getCompleteCredentials() = credentialsDao.getCompleteCredentials()

    suspend fun updateCredentials(credentials: Credentials) =
        credentialsDao.updateCredentials(credentials)

    suspend fun insert(credentials: Credentials) = credentialsDao.insert(credentials)
}