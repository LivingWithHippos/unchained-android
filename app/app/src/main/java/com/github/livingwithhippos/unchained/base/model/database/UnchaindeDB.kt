package com.github.livingwithhippos.unchained.base.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.livingwithhippos.unchained.base.model.dao.CredentialsDao
import com.github.livingwithhippos.unchained.base.model.entities.Credentials

// Annotates class to be a Room Database with a table (entity) of the Word class
@Database(entities = [Credentials::class],
    version = 1,
    exportSchema = false
)
public abstract class UnchaindeDB : RoomDatabase() {

    abstract fun credentialsDao(): CredentialsDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: UnchaindeDB? = null

        fun getDatabase(context: Context): UnchaindeDB {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UnchaindeDB::class.java,
                    "unachained_db"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}