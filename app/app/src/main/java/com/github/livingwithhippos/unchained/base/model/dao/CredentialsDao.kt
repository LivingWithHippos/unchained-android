package com.github.livingwithhippos.unchained.base.model.dao

import androidx.room.*
import com.github.livingwithhippos.unchained.base.model.entities.Credentials

@Dao
interface CredentialsDao {

    @Query("SELECT * FROM credentials WHERE device_code = :deviceCode")
    suspend fun getCredentials(deviceCode: String): Credentials?

    // this is supposing only correct values get saved
    // fixme: this does not return lines even if we have filled rows. Query works on external editor with downloaded database. Use getCredentials and .filter{ fields != null }
    @Query("SELECT * FROM credentials WHERE client_id IS NOT NULL AND client_secret IS NOT NULL AND access_token IS NOT NULL AND refresh_token IS NOT NULL")
    suspend fun getCompleteCredentials(): List<Credentials>

    @Query("SELECT * from credentials")
    suspend fun getAllCredentials(): List<Credentials>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(credentials: Credentials)

    // bug: not working, use updateCredentials
    @Query("UPDATE credentials SET client_id = :clientId AND client_secret = :clientSecret WHERE device_code = :deviceCode")
    suspend fun updateSecrets(deviceCode: String, clientId: String, clientSecret: String)

    // bug: not working, use updateCredentials
    @Query("UPDATE credentials SET access_token = :accessToken AND refresh_token = :refreshToken WHERE device_code = :deviceCode")
    suspend fun updateToken(deviceCode: String, accessToken: String, refreshToken: String)

    @Update
    suspend fun updateCredentials(credentials: Credentials)

    @Query("DELETE FROM credentials")
    suspend fun deleteAll()

    @Query("DELETE FROM credentials WHERE device_code = :deviceCode")
    suspend fun delete(deviceCode: String)

    @Query("DELETE FROM credentials WHERE access_token IS NULL OR access_token LIKE ''")
    suspend fun deleteIncompleteCredentials()

    @Query("DELETE FROM credentials WHERE access_token != :privateTokenIndicator")
    suspend fun deleteAllOpenSource(privateTokenIndicator: String = PRIVATE_TOKEN)
}