package com.github.livingwithhippos.unchained.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.livingwithhippos.unchained.data.model.HostRegex
import com.github.livingwithhippos.unchained.data.model.KodiDevice

/**
 * Annotates class to be a Room Database with a table (entity) of the Credentials class
 */
@Database(
    entities = [HostRegex::class, KodiDevice::class],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 4, to = 5)
    ]
)
abstract class UnchaineDB : RoomDatabase() {
    abstract fun hostRegexDao(): HostRegexDao
    abstract fun kodiDeviceDao(): KodiDeviceDao
}
