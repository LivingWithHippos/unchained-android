package com.github.livingwithhippos.unchained.base.network

import com.github.livingwithhippos.unchained.authentication.model.AuthenticationApi
import com.github.livingwithhippos.unchained.utilities.BASE_AUTH_URL
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
object ApiAuthFactory {


    //OkhttpClient for building http request url
    private val debridClient: OkHttpClient = OkHttpClient().newBuilder()
        .build()

    @Provides
    fun provideBaseUrl() = BASE_AUTH_URL

    @Provides
    @Singleton
    fun provideLoggerClient(): OkHttpClient {
        //todo: remove logger when finished
        val logInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient().newBuilder()
            .addInterceptor(logInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun retrofit(okHttpClient: OkHttpClient, BASE_URL: String): Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideAuthenticationApi(retrofit: Retrofit): AuthenticationApi {
        return retrofit.create(AuthenticationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthenticationApiHelper(apiHelper: AuthApiHelperImpl): AuthApiHelper = apiHelper
}