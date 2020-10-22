package com.github.livingwithhippos.unchained.di

import com.github.livingwithhippos.unchained.data.model.EmptyBodyInterceptor
import com.github.livingwithhippos.unchained.data.remote.AuthApiHelper
import com.github.livingwithhippos.unchained.data.remote.AuthApiHelperImpl
import com.github.livingwithhippos.unchained.data.remote.AuthenticationApi
import com.github.livingwithhippos.unchained.data.remote.DownloadApiHelper
import com.github.livingwithhippos.unchained.data.remote.DownloadApiHelperImpl
import com.github.livingwithhippos.unchained.data.remote.DownloadsApi
import com.github.livingwithhippos.unchained.data.remote.HostsApi
import com.github.livingwithhippos.unchained.data.remote.HostsApiHelper
import com.github.livingwithhippos.unchained.data.remote.HostsApiHelperImpl
import com.github.livingwithhippos.unchained.data.remote.StreamingApi
import com.github.livingwithhippos.unchained.data.remote.StreamingApiHelper
import com.github.livingwithhippos.unchained.data.remote.StreamingApiHelperImpl
import com.github.livingwithhippos.unchained.data.remote.TorrentApiHelper
import com.github.livingwithhippos.unchained.data.remote.TorrentApiHelperImpl
import com.github.livingwithhippos.unchained.data.remote.TorrentsApi
import com.github.livingwithhippos.unchained.data.remote.UnrestrictApi
import com.github.livingwithhippos.unchained.data.remote.UnrestrictApiHelper
import com.github.livingwithhippos.unchained.data.remote.UnrestrictApiHelperImpl
import com.github.livingwithhippos.unchained.data.remote.UserApi
import com.github.livingwithhippos.unchained.data.remote.UserApiHelper
import com.github.livingwithhippos.unchained.data.remote.UserApiHelperImpl
import com.github.livingwithhippos.unchained.data.remote.VariousApi
import com.github.livingwithhippos.unchained.data.remote.VariousApiHelper
import com.github.livingwithhippos.unchained.data.remote.VariousApiHelperImpl
import com.github.livingwithhippos.unchained.utilities.BASE_AUTH_URL
import com.github.livingwithhippos.unchained.utilities.BASE_URL
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

/**
 * This object manages the Dagger-Hilt injection for the  OkHttp and Retrofit clients
 */
@InstallIn(ApplicationComponent::class)
@Module
object ApiFactory {

    /*********************************/
    // N.B. all updates to this code //
    // also need to be ported to the //
    // debug build flavor version.   //
    /*********************************/

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()
        .newBuilder()
        // avoid issues with empty bodies on delete/put and 20x return codes
        .addInterceptor(EmptyBodyInterceptor)
        .build()

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
    fun apiRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

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

    // download api injection
    @Provides
    @Singleton
    fun provideDownloadsApi(@ApiRetrofit retrofit: Retrofit): DownloadsApi {
        return retrofit.create(DownloadsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDownloadApiHelper(apiHelper: DownloadApiHelperImpl): DownloadApiHelper =
        apiHelper

    // hosts api injection
    @Provides
    @Singleton
    fun provideHostsApi(@ApiRetrofit retrofit: Retrofit): HostsApi {
        return retrofit.create(HostsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideHostsApiHelper(apiHelper: HostsApiHelperImpl): HostsApiHelper =
        apiHelper
    
    // various api injection
    @Provides
    @Singleton
    fun provideVariousApi(@ApiRetrofit retrofit: Retrofit): VariousApi {
        return retrofit.create(VariousApi::class.java)
    }

    @Provides
    @Singleton
    fun provideVariousApiHelper(apiHelper: VariousApiHelperImpl): VariousApiHelper =
        apiHelper
}