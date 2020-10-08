package com.github.livingwithhippos.unchained.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "host_regex")
class HostRegex(
    @PrimaryKey
    @ColumnInfo(name = "regex")
    val regex: String
)