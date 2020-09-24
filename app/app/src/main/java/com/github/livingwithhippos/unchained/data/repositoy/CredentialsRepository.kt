package com.github.livingwithhippos.unchained.data.repositoy

import com.github.livingwithhippos.unchained.data.local.CredentialsDao
import com.github.livingwithhippos.unchained.data.model.Credentials
import com.github.livingwithhippos.unchained.utilities.PRIVATE_TOKEN
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

    suspend fun insertPrivateToken(privateToken: String) =
        credentialsDao.insertPrivateToken(privateToken)

    suspend fun deleteAllCredentials() = credentialsDao.deleteAll()

    suspend fun deleteAllOpenSourceCredentials() = credentialsDao.deleteAllOpenSource()

    suspend fun deleteIncompleteCredentials() = credentialsDao.deleteIncompleteCredentials()

    suspend fun getToken(): String {
        return getFirstCredentials()?.accessToken ?: ""
    }

    suspend fun getFirstCredentials(): Credentials? {
        val credentials = credentialsDao.getCompleteCredentials()
        return credentials
            // return private credentials first
            .firstOrNull { it.refreshToken == PRIVATE_TOKEN }
        // open source credentials second
            ?: credentials.firstOrNull()
    }

    suspend fun getFirstOpenCredentials(): Credentials? = credentialsDao.getCompleteCredentials()
        .firstOrNull { it.refreshToken != null && it.refreshToken != PRIVATE_TOKEN }

}