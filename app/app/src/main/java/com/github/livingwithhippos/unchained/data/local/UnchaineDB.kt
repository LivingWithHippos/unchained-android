package com.github.livingwithhippos.unchained.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.livingwithhippos.unchained.data.model.HostRegex

/**
 * Annotates class to be a Room Database with a table (entity) of the Credentials class
 */
@Database(
    entities = [HostRegex::class],
    version = 4,
    exportSchema = true
)
abstract class UnchaineDB : RoomDatabase() {
    abstract fun hostRegexDao(): HostRegexDao
}
