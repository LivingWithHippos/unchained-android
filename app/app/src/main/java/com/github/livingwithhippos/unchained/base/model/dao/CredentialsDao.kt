package com.github.livingwithhippos.unchained.base.model.dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.livingwithhippos.unchained.base.model.entities.Credentials

interface CredentialsDao {

    @Query("SELECT * FROM credentials WHERE username = :username")
    suspend fun getCredentials(username: String): Credentials?

    @Query("SELECT * from credentials")
    suspend fun getAllCredentials(): List<Credentials>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(credentials: Credentials)

    @Query("UPDATE credentials SET client_id = :clientId AND client_secret = :clientSecret WHERE username = :username")
    suspend fun updateSecrets(username: String, clientId: String, clientSecret: String)

    @Query("DELETE FROM credentials")
    suspend fun deleteAll()

    @Query("DELETE FROM credentials WHERE username = :username")
    suspend fun delete(username: String)
}