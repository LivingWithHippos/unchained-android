package com.github.livingwithhippos.unchained.base.model.repositories

import com.github.livingwithhippos.unchained.base.model.dao.CredentialsDao
import com.github.livingwithhippos.unchained.base.model.entities.Credentials

// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class CredentialsRepository(private val credentialsDao: CredentialsDao) {

    suspend fun getAllCredentials() = credentialsDao.getAllCredentials()

    suspend fun getCredentials(username: String) = credentialsDao.getCredentials(username)

    suspend fun updateSecrets(username: String, clientId: String, clientSecret: String) =
        credentialsDao.updateSecrets(username, clientId, clientSecret)

    suspend fun updateToken(deviceCode: String, accessToken: String, refreshToken: String) =
        credentialsDao.updateToken(deviceCode, accessToken, refreshToken)

    suspend fun insert(credentials: Credentials) = credentialsDao.insert(credentials)
}