package com.github.livingwithhippos.unchained.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.livingwithhippos.unchained.data.local.HostRegexDao
import com.github.livingwithhippos.unchained.data.local.KodiDeviceDao
import com.github.livingwithhippos.unchained.data.local.RemoteDeviceDao
import com.github.livingwithhippos.unchained.data.local.RepositoryDataDao
import com.github.livingwithhippos.unchained.data.local.UnchaineDB
import com.github.livingwithhippos.unchained.data.model.REGEX_TYPE_HOST
import com.github.livingwithhippos.unchained.data.repository.AuthenticationRepository
import com.github.livingwithhippos.unchained.data.repository.KodiDeviceRepository
import com.github.livingwithhippos.unchained.data.repository.RemoteDeviceRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), UnchaineDB::class.java, "unchained_db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    single<HostRegexDao> { get<UnchaineDB>().hostRegexDao() }
    single<KodiDeviceDao> { get<UnchaineDB>().kodiDeviceDao() }
    single<RemoteDeviceDao> { get<UnchaineDB>().pluginRemoteDeviceDao() }
    single<RepositoryDataDao> { get<UnchaineDB>().pluginRepositoryDao() }

    singleOf(::RemoteDeviceRepository) { bind<RemoteDeviceRepository>() }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `host_regex` (`regex` TEXT NOT NULL, PRIMARY KEY(`regex`))"
        )
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE host_regex ADD COLUMN type INTEGER NOT NULL DEFAULT $REGEX_TYPE_HOST"
        )
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE credentials")
    }
}