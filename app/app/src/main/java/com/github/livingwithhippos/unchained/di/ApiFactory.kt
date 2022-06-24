package com.github.livingwithhippos.unchained.di

import android.content.SharedPreferences
import com.github.livingwithhippos.unchained.BuildConfig
import com.github.livingwithhippos.unchained.data.model.EmptyBodyInterceptor
import com.github.livingwithhippos.unchained.data.remote.AuthApiHelper
import com.github.livingwithhippos.unchained.data.remote.AuthApiHelperImpl
import com.github.livingwithhippos.unchained.data.remote.AuthenticationApi
import com.github.livingwithhippos.unchained.data.remote.CustomDownload
import com.github.livingwithhippos.unchained.data.remote.CustomDownloadHelper
import com.github.livingwithhippos.unchained.data.remote.CustomDownloadHelperImpl
import com.github.livingwithhippos.unchained.data.remote.DownloadApi
import com.github.livingwithhippos.unchained.data.remote.DownloadApiHelper
import com.github.livingwithhippos.unchained.data.remote.DownloadApiHelperImpl
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
import com.github.livingwithhippos.unchained.data.remote.UpdateApi
import com.github.livingwithhippos.unchained.data.remote.UpdateApiHelper
import com.github.livingwithhippos.unchained.data.remote.UpdateApiHelperImpl
import com.github.livingwithhippos.unchained.data.remote.UserApi
import com.github.livingwithhippos.unchained.data.remote.UserApiHelper
import com.github.livingwithhippos.unchained.data.remote.UserApiHelperImpl
import com.github.livingwithhippos.unchained.data.remote.VariousApi
import com.github.livingwithhippos.unchained.data.remote.VariousApiHelper
import com.github.livingwithhippos.unchained.data.remote.VariousApiHelperImpl
import com.github.livingwithhippos.unchained.plugins.Parser
import com.github.livingwithhippos.unchained.utilities.BASE_AUTH_URL
import com.github.livingwithhippos.unchained.utilities.BASE_URL
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionSpec
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.InetAddress
import javax.inject.Singleton

/**
 * This object manages the Dagger-Hilt injection for the  OkHttp and Retrofit clients
 */
@InstallIn(SingletonComponent::class)
@Module
object ApiFactory {

    @Provides
    @Singleton
    @ClassicClient
    fun provideOkHttpClient(): OkHttpClient {
        if (BuildConfig.DEBUG) {
            val logInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            return OkHttpClient().newBuilder()
                // should fix the javax.net.ssl.SSLHandshakeException: Failure in SSL library
                .connectionSpecs(
                    listOf(
                        ConnectionSpec.CLEARTEXT,
                        ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                            .allEnabledTlsVersions()
                            .allEnabledCipherSuites()
                            .build()
                    )
                )
                // logs all the calls, removed in the release channel
                .addInterceptor(logInterceptor)
                // avoid issues with empty bodies on delete/put and 20x return codes
                .addInterceptor(EmptyBodyInterceptor)
                .build()
        } else return OkHttpClient()
            .newBuilder()
            .connectionSpecs(
                listOf(
                    ConnectionSpec.CLEARTEXT,
                    ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .allEnabledTlsVersions()
                        .allEnabledCipherSuites()
                        .build()
                )
            )
            // avoid issues with empty bodies on delete/put and 20x return codes
            .addInterceptor(EmptyBodyInterceptor)
            .build()
    }

    /**
     * examples: [https://github.com/square/okhttp/blob/master/okhttp-dnsoverhttps/src/test/java/okhttp3/dnsoverhttps/DohProviders.java]
     * list: [https://github.com/curl/curl/wiki/DNS-over-HTTPS]
     * @return
     */
    @Provides
    @Singleton
    @DOHClient
    fun provideDOHClient(): OkHttpClient {

        val bootstrapClient: OkHttpClient = if (BuildConfig.DEBUG) {

            val logInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            OkHttpClient().newBuilder()
                .connectionSpecs(
                    listOf(
                        ConnectionSpec.CLEARTEXT,
                        ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                            .allEnabledTlsVersions()
                            .allEnabledCipherSuites()
                            .build()
                    )
                )
                // logs all the calls, removed in the release channel
                .addInterceptor(logInterceptor)
                // avoid issues with empty bodies on delete/put and 20x return codes
                .addInterceptor(EmptyBodyInterceptor)
                .build()
        } else {
            OkHttpClient()
                .newBuilder()
                .connectionSpecs(
                    listOf(
                        ConnectionSpec.CLEARTEXT,
                        ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                            .allEnabledTlsVersions()
                            .allEnabledCipherSuites()
                            .build()
                    )
                )
                .addInterceptor(EmptyBodyInterceptor)
                .build()
        }

        val dns = DnsOverHttps.Builder().client(bootstrapClient)
            .url("https://dns.google/dns-query".toHttpUrl())
            .bootstrapDnsHosts(InetAddress.getByName("8.8.8.8"), InetAddress.getByName("8.8.4.4"))
            .build()

        return bootstrapClient.newBuilder().dns(dns).build()
    }

    @Provides
    @Singleton
    @AuthRetrofit
    fun authRetrofit(@ClassicClient okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(BASE_AUTH_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    @Provides
    @Singleton
    @ApiRetrofit
    fun apiRetrofit(@ClassicClient okHttpClient: OkHttpClient): Retrofit {
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
    fun provideDownloadsApi(@ApiRetrofit retrofit: Retrofit): DownloadApi {
        return retrofit.create(DownloadApi::class.java)
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

    // update api injection
    @Provides
    @Singleton
    fun provideUpdateApi(@ApiRetrofit retrofit: Retrofit): UpdateApi {
        return retrofit.create(UpdateApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUpdateApiHelper(apiHelper: UpdateApiHelperImpl): UpdateApiHelper =
        apiHelper

    // custom download injection
    @Provides
    @Singleton
    fun provideCustomDownload(@ApiRetrofit retrofit: Retrofit): CustomDownload {
        return retrofit.create(CustomDownload::class.java)
    }

    @Provides
    @Singleton
    fun provideCustomDownloadHelper(customHelper: CustomDownloadHelperImpl): CustomDownloadHelper =
        customHelper

    /**
     * Search Plugins stuff
     */

    @Provides
    @Singleton
    fun provideParser(
        preferences: SharedPreferences,
        @ClassicClient classicClient: OkHttpClient,
        @DOHClient dohClient: OkHttpClient
    ): Parser = Parser(preferences, classicClient, dohClient)
}
