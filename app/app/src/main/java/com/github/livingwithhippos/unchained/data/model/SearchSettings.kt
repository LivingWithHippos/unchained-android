package com.github.livingwithhippos.unchained.data.model

enum class Category(val id: Int) {
    ALL(0),
    ART(1),
    ANIME(2),
    DOUJINSHI(3),
    MANGA(4),
    SOFTWARE(5),
    GAMES(6),
    MOVIES(7),
    PICTURES(8),
    VIDEOS(9),
    MUSIC(10),
    TV(11),
    BOOKS(12),
}

enum class Sorting(val id: Int) {
    DEFAULT(0),
    SEEDERS(1),
    SIZE_ASC(2),
    SIZE_DESC(3),
    AZ(4),
    ZA(5),
    DATE(6),
}
