package com.github.livingwithhippos.unchained.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CompleteRemoteServiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: CompleteRemoteService): Long

    @Upsert suspend fun upsertService(service: CompleteRemoteService): Long

    @Delete suspend fun deleteService(service: CompleteRemoteService)

    @Query("DELETE FROM complete_remote_service WHERE id = :serviceID") suspend fun deleteService(serviceID: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllServices(list: List<CompleteRemoteService>): List<Long>

    @Query(
        "SELECT * FROM complete_remote_service"
    )
    suspend fun getServices(): List<CompleteRemoteService>

    @Query(
        "SELECT * FROM complete_remote_service WHERE complete_remote_service.type IN (:types)"
    )
    suspend fun getMediaPlayerServices(
        types: List<Int>
    ): List<CompleteRemoteService>

    @Query(
        "SELECT * FROM complete_remote_service WHERE complete_remote_service.type IN (:types)"
    )
    fun getMediaPlayerServicesFlow(
        types: List<Int>
    ): Flow<List<CompleteRemoteService>>

    @Query("SELECT id FROM complete_remote_service WHERE rowid = :rowId")
    suspend fun getServiceIDByRow(rowId: Long): Int?

    @Query("SELECT * FROM complete_remote_service WHERE id = :serviceID")
    suspend fun getService(serviceID: Int): CompleteRemoteService?

    @Query("DELETE FROM complete_remote_service") suspend fun deleteAll()

    @Query("DELETE FROM complete_remote_service WHERE id = :id") suspend fun removeService(id: Int)

    @Query("SELECT * from complete_remote_service WHERE complete_remote_service.is_default = 1 LIMIT 1")
    suspend fun getDefaultService(): RemoteDevice?

    @Query("UPDATE complete_remote_service SET is_default = CASE WHEN id = :id THEN 1 ELSE 0 END;")
    suspend fun setDefaultService(id: Int)
}
