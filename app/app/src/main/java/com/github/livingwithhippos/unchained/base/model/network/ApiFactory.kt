package com.github.livingwithhippos.unchained.base.model.network

import com.github.livingwithhippos.unchained.BuildConfig
import com.github.livingwithhippos.unchained.authentication.model.AuthenticationApi
import com.github.livingwithhippos.unchained.base.di.ApiRetrofit
import com.github.livingwithhippos.unchained.base.di.AuthRetrofit
import com.github.livingwithhippos.unchained.downloaddetails.model.StreamingApi
import com.github.livingwithhippos.unchained.downloaddetails.model.StreamingApiHelper
import com.github.livingwithhippos.unchained.downloaddetails.model.StreamingApiHelperImpl
import com.github.livingwithhippos.unchained.newdownload.model.TorrentApiHelper
import com.github.livingwithhippos.unchained.newdownload.model.TorrentApiHelperImpl
import com.github.livingwithhippos.unchained.newdownload.model.TorrentsApi
import com.github.livingwithhippos.unchained.newdownload.model.UnrestrictApi
import com.github.livingwithhippos.unchained.newdownload.model.UnrestrictApiHelper
import com.github.livingwithhippos.unchained.newdownload.model.UnrestrictApiHelperImpl
import com.github.livingwithhippos.unchained.user.model.UserApi
import com.github.livingwithhippos.unchained.user.model.UserApiHelper
import com.github.livingwithhippos.unchained.user.model.UserApiHelperImpl
import com.github.livingwithhippos.unchained.utilities.BASE_AUTH_URL
import com.github.livingwithhippos.unchained.utilities.BASE_URL
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
object ApiFactory {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // note: alternatively use a different build flavor
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
    @AuthRetrofit
    fun authRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(BASE_AUTH_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    @Provides
    @Singleton
    @ApiRetrofit
    fun apiRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    // authentication api injection
    @Provides
    @Singleton
    fun provideAuthenticationApi(@AuthRetrofit retrofit: Retrofit): AuthenticationApi {
        return retrofit.create(AuthenticationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthenticationApiHelper(apiHelper: AuthApiHelperImpl): AuthApiHelper = apiHelper

    // user api injection
    @Provides
    @Singleton
    fun provideUserApi(@ApiRetrofit retrofit: Retrofit): UserApi {
        return retrofit.create(UserApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApiHelper(apiHelper: UserApiHelperImpl): UserApiHelper = apiHelper

    // unrestrict api injection
    @Provides
    @Singleton
    fun provideUnrestrictApi(@ApiRetrofit retrofit: Retrofit): UnrestrictApi {
        return retrofit.create(UnrestrictApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUnrestrictApiHelper(apiHelper: UnrestrictApiHelperImpl): UnrestrictApiHelper =
        apiHelper

    // streaming api injection
    @Provides
    @Singleton
    fun provideStreamingApi(@ApiRetrofit retrofit: Retrofit): StreamingApi {
        return retrofit.create(StreamingApi::class.java)
    }

    @Provides
    @Singleton
    fun provideStreamingApiHelper(apiHelper: StreamingApiHelperImpl): StreamingApiHelper =
        apiHelper

    // torrent api injection
    @Provides
    @Singleton
    fun provideTorrentsApi(@ApiRetrofit retrofit: Retrofit): TorrentsApi {
        return retrofit.create(TorrentsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTorrentsApiApiHelper(apiHelper: TorrentApiHelperImpl): TorrentApiHelper =
        apiHelper
}