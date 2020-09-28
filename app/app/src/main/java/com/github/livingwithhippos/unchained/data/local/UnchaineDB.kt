package com.github.livingwithhippos.unchained.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.livingwithhippos.unchained.data.model.Credentials

/**
 * Annotates class to be a Room Database with a table (entity) of the Credentials class
 */
@Database(
    entities = [Credentials::class],
    version = 1,
    exportSchema = false
)
abstract class UnchaineDB : RoomDatabase() {

    abstract fun credentialsDao(): CredentialsDao

}