package com.github.livingwithhippos.unchained.base.network

import com.github.livingwithhippos.unchained.BuildConfig
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

//todo: restrict scope or remove d.i. from here since the one most used after the login will be
// the other one (ApiFactory)
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
    fun provideOkHttpClient(): OkHttpClient {
        //note: alternatively use a different build flavor
        // https://proandroiddev.com/think-before-using-buildconfig-debug-f2e279da7bad
        if (BuildConfig.DEBUG) {
            val logInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            return OkHttpClient().newBuilder()
                .addInterceptor(logInterceptor)
                .build()

        } else {

            return OkHttpClient()
                .newBuilder()
                .build()
        }

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