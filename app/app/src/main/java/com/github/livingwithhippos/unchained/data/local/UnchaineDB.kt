package com.github.livingwithhippos.unchained.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.livingwithhippos.unchained.data.model.Credentials
import com.github.livingwithhippos.unchained.data.model.HostRegex

/**
 * Annotates class to be a Room Database with a table (entity) of the Credentials class
 */
@Database(
    entities = [Credentials::class, HostRegex::class],
    version = 3,
    exportSchema = true
)
abstract class UnchaineDB : RoomDatabase() {

    abstract fun credentialsDao(): CredentialsDao
    abstract fun hostRegexDao(): HostRegexDao

}