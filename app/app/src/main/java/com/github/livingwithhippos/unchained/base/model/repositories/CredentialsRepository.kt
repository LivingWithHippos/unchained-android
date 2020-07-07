package com.github.livingwithhippos.unchained.base.model.repositories

import com.github.livingwithhippos.unchained.base.model.dao.CredentialsDao
import com.github.livingwithhippos.unchained.base.model.entities.Credentials
import javax.inject.Inject

// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class CredentialsRepository @Inject constructor(private val credentialsDao: CredentialsDao) {

    suspend fun getAllCredentials() = credentialsDao.getAllCredentials()

    suspend fun getCredentials(deviceCode: String) = credentialsDao.getCredentials(deviceCode)

    suspend fun updateSecrets(deviceCode: String, clientId: String, clientSecret: String) =
        credentialsDao.updateSecrets(deviceCode, clientId, clientSecret)

    suspend fun updateToken(deviceCode: String, accessToken: String, refreshToken: String) =
        credentialsDao.updateToken(deviceCode, accessToken, refreshToken)

    suspend fun updateCredentials(credentials: Credentials) =
        credentialsDao.updateCredentials(credentials)

    suspend fun insert(credentials: Credentials) = credentialsDao.insert(credentials)
}