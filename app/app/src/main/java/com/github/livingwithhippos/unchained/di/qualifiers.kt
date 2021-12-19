package com.github.livingwithhippos.unchained.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TorrentNotification

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SummaryNotification

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadNotification

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ClassicClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DOHClient
