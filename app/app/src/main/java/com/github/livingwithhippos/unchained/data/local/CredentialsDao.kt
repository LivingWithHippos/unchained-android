package com.github.livingwithhippos.unchained.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.github.livingwithhippos.unchained.data.model.Credentials
import com.github.livingwithhippos.unchained.utilities.PRIVATE_TOKEN

/**
 * This Dao manage the queries for the credentials table in the database.
 */
@Dao
interface CredentialsDao {

    @Query("SELECT * FROM credentials WHERE device_code = :deviceCode")
    suspend fun getCredentials(deviceCode: String): Credentials?

    @Query("SELECT credentials.access_token FROM credentials WHERE device_code = :deviceCode")
    suspend fun getPrivateToken(deviceCode: String = PRIVATE_TOKEN): String?

    // this is supposing only correct values get saved
    // fixme: this does not return lines even if we have filled rows. Query works on external editor with downloaded database. Use getCredentials and .filter{ fields != null }
    @Query("SELECT * FROM credentials WHERE client_id IS NOT NULL AND client_secret IS NOT NULL AND access_token IS NOT NULL AND refresh_token IS NOT NULL")
    suspend fun getCompleteCredentials(): List<Credentials>

    @Query("SELECT * from credentials")
    suspend fun getAllCredentials(): List<Credentials>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(credentials: Credentials)

    /**
     * Insert a private api token in the credentials table.
     * this way we'll have a single private token since the primary key, device_code, is always $PRIVATE_TOKEN
     */
    @Query("INSERT OR REPLACE INTO credentials (access_token, client_id, client_secret, device_code, refresh_token) VALUES (:privateToken,:privateTokenIndicator,:privateTokenIndicator,:privateTokenIndicator,:privateTokenIndicator)")
    suspend fun insertPrivateToken(
        privateToken: String,
        privateTokenIndicator: String = PRIVATE_TOKEN
    )

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

    @Query("DELETE FROM credentials WHERE access_token IS NULL OR access_token = ''")
    suspend fun deleteIncompleteCredentials()

    @Query("DELETE FROM credentials WHERE access_token != :privateTokenIndicator")
    suspend fun deleteAllOpenSource(privateTokenIndicator: String = PRIVATE_TOKEN)
}