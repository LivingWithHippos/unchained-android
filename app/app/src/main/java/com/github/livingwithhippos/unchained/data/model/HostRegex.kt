package com.github.livingwithhippos.unchained.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "host_regex")
class HostRegex(
    @PrimaryKey @ColumnInfo(name = "regex") val regex: String,
    @ColumnInfo(name = "type") val type: Int = REGEX_TYPE_HOST
)

const val REGEX_TYPE_HOST = 0
const val REGEX_TYPE_FOLDER = 1
