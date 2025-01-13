package com.github.livingwithhippos.unchained.di

import android.content.SharedPreferences
import com.github.livingwithhippos.unchained.BuildConfig
import com.github.livingwithhippos.unchained.authentication.viewmodel.AuthenticationViewModel
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
import com.github.livingwithhippos.unchained.data.repository.AuthenticationRepository
import com.github.livingwithhippos.unchained.data.repository.CustomDownloadRepository
import com.github.livingwithhippos.unchained.data.repository.DatabasePluginRepository
import com.github.livingwithhippos.unchained.data.repository.DownloadRepository
import com.github.livingwithhippos.unchained.data.repository.HostsRepository
import com.github.livingwithhippos.unchained.data.repository.KodiDeviceRepository
import com.github.livingwithhippos.unchained.data.repository.KodiRepository
import com.github.livingwithhippos.unchained.data.repository.PluginRepository
import com.github.livingwithhippos.unchained.data.repository.RemoteRepository
import com.github.livingwithhippos.unchained.data.repository.StreamingRepository
import com.github.livingwithhippos.unchained.data.repository.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repository.UnrestrictRepository
import com.github.livingwithhippos.unchained.data.repository.UpdateRepository
import com.github.livingwithhippos.unchained.data.repository.UserRepository
import com.github.livingwithhippos.unchained.data.repository.VariousApiRepository
import com.github.livingwithhippos.unchained.downloaddetails.viewmodel.DownloadDetailsViewModel
import com.github.livingwithhippos.unchained.folderlist.viewmodel.FolderListViewModel
import com.github.livingwithhippos.unchained.lists.viewmodel.DownloadDialogViewModel
import com.github.livingwithhippos.unchained.lists.viewmodel.ListTabsViewModel
import com.github.livingwithhippos.unchained.newdownload.viewmodel.NewDownloadViewModel
import com.github.livingwithhippos.unchained.plugins.Parser
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.remotedevice.viewmodel.DeviceViewModel
import com.github.livingwithhippos.unchained.repository.viewmodel.RepositoryViewModel
import com.github.livingwithhippos.unchained.search.viewmodel.SearchViewModel
import com.github.livingwithhippos.unchained.settings.viewmodel.HtmlDialogViewModel
import com.github.livingwithhippos.unchained.settings.viewmodel.SettingsViewModel
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import com.github.livingwithhippos.unchained.torrentdetails.viewmodel.TorrentDetailsViewModel
import com.github.livingwithhippos.unchained.torrentfilepicker.viewmodel.TorrentProcessingViewModel
import com.github.livingwithhippos.unchained.utilities.BASE_AUTH_URL
import com.github.livingwithhippos.unchained.utilities.BASE_URL
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import okhttp3.ConnectionSpec
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.net.InetAddress

val appModule = module {
    single(named("ClassicClient")) { provideOkHttpClient() }
    single(named("DOHClient")) { provideDOHClient() }
    single(named("AuthRetrofit")) { authRetrofit(get(named("ClassicClient"))) }
    single(named("ApiRetrofit")) { apiRetrofit(get(named("ClassicClient"))) }

    single<KodiDeviceRepository> { KodiDeviceRepository(get(named("ClassicClient"))) }
    single<KodiRepository> { KodiRepository(get(), get(named("ClassicClient"))) }

    single<RemoteRepository> { RemoteRepository( get(named("ClassicClient"))) }
    singleOf(::UpdateRepository) { bind<UpdateRepository>() }

    single<AuthenticationApi> { provideAuthenticationApi(get(named("AuthRetrofit"))) }
    single<AuthApiHelper> { AuthApiHelperImpl(get()) }
    singleOf(::AuthenticationRepository) { bind<AuthenticationRepository>() }

    single<UserApi> { provideUserApi(get(named("ApiRetrofit"))) }
    single<UserApiHelper> { UserApiHelperImpl(get()) }
    singleOf(::UserRepository) { bind<UserRepository>() }

    single<UnrestrictApi> { provideUnrestrictApi(get(named("ApiRetrofit"))) }
    single<UnrestrictApiHelper> { UnrestrictApiHelperImpl(get()) }
    singleOf(::UnrestrictRepository) { bind<UnrestrictRepository>() }

    single<StreamingApi> { provideStreamingApi(get(named("ApiRetrofit"))) }
    single<StreamingApiHelper> { StreamingApiHelperImpl(get()) }
    singleOf(::StreamingRepository) { bind<StreamingRepository>() }

    single<TorrentsApi> { provideTorrentsApi(get(named("ApiRetrofit"))) }
    single<TorrentApiHelper> { TorrentApiHelperImpl(get()) }
    singleOf(::TorrentsRepository) { bind<TorrentsRepository>() }

    single<DownloadApi> { provideDownloadsApi(get(named("ApiRetrofit"))) }
    single<DownloadApiHelper> { DownloadApiHelperImpl(get()) }
    singleOf(::DownloadRepository) { bind<DownloadRepository>() }

    single < HostsApi>{ provideHostsApi(get(named("ApiRetrofit"))) }
    single<HostsApiHelper> { HostsApiHelperImpl(get()) }
    singleOf(::HostsRepository) { bind<HostsRepository>() }

    single < VariousApi>{ provideVariousApi(get(named("ApiRetrofit"))) }
    single<VariousApiHelper> { VariousApiHelperImpl(get()) }
    singleOf(::VariousApiRepository) { bind<VariousApiRepository>() }

    single < UpdateApi> { provideUpdateApi(get(named("ApiRetrofit"))) }
    single<UpdateApiHelper> { UpdateApiHelperImpl(get()) }

    single < CustomDownload> { provideCustomDownload(get(named("ApiRetrofit"))) }
    single<CustomDownloadHelper> { CustomDownloadHelperImpl(get()) }
    singleOf(::CustomDownloadRepository) { bind<CustomDownloadRepository>() }

    singleOf(::DatabasePluginRepository) { bind<DatabasePluginRepository>() }
    single<PluginRepository> { PluginRepository(providePluginAdapter()) }

    single<Parser> { provideParser(get(), get(named("ClassicClient")), get(named("DOHClient"))) }

    /***************
     * ViewModels *
     ***************/

    viewModelOf(::MainActivityViewModel)
    viewModelOf(::AuthenticationViewModel)
    viewModelOf(::DownloadDetailsViewModel)
    viewModelOf(::FolderListViewModel)
    viewModelOf(::ListTabsViewModel)
    viewModelOf(::DownloadDialogViewModel)
    viewModelOf(::DeviceViewModel)
    viewModelOf(::TorrentDetailsViewModel)
    viewModelOf(::HtmlDialogViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::RepositoryViewModel)
    viewModelOf(::NewDownloadViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::TorrentProcessingViewModel)

    /*************
     * VARIOUS
     */

}


fun provideOkHttpClient(): OkHttpClient {
    if (BuildConfig.DEBUG) {
        val logInterceptor: HttpLoggingInterceptor =
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        return OkHttpClient().newBuilder()
            // should fix the javax.net.ssl.SSLHandshakeException: Failure in SSL library
            .connectionSpecs(
                listOf(
                    ConnectionSpec.CLEARTEXT,
                    ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).allEnabledTlsVersions()
                        .allEnabledCipherSuites().build(),
                )
            )
            // logs all the calls, removed in the release channel
            .addInterceptor(logInterceptor)
            // avoid issues with empty bodies on delete/put and 20x return codes
            .addInterceptor(EmptyBodyInterceptor).build()
    } else return OkHttpClient().newBuilder().connectionSpecs(
            listOf(
                ConnectionSpec.CLEARTEXT,
                ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).allEnabledTlsVersions()
                    .allEnabledCipherSuites().build(),
            )
        )
        // avoid issues with empty bodies on delete/put and 20x return codes
        .addInterceptor(EmptyBodyInterceptor).build()
}

fun provideDOHClient(): OkHttpClient {

    val bootstrapClient: OkHttpClient = if (BuildConfig.DEBUG) {

        val logInterceptor: HttpLoggingInterceptor =
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        OkHttpClient().newBuilder().connectionSpecs(
                listOf(
                    ConnectionSpec.CLEARTEXT,
                    ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).allEnabledTlsVersions()
                        .allEnabledCipherSuites().build(),
                )
            )
            // logs all the calls, removed in the release channel
            .addInterceptor(logInterceptor)
            // avoid issues with empty bodies on delete/put and 20x return codes
            .addInterceptor(EmptyBodyInterceptor).build()
    } else {
        OkHttpClient().newBuilder().connectionSpecs(
                listOf(
                    ConnectionSpec.CLEARTEXT,
                    ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).allEnabledTlsVersions()
                        .allEnabledCipherSuites().build(),
                )
            ).addInterceptor(EmptyBodyInterceptor).build()
    }

    val dns = DnsOverHttps.Builder().client(bootstrapClient)
        .url("https://dns.google/dns-query".toHttpUrl()).bootstrapDnsHosts(
            InetAddress.getByName("8.8.8.8"),
            InetAddress.getByName("8.8.4.4"),
        ).build()

    return bootstrapClient.newBuilder().dns(dns).build()
}

fun authRetrofit(okHttpClient: OkHttpClient): Retrofit {
    return Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(BASE_AUTH_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
}

fun apiRetrofit(okHttpClient: OkHttpClient): Retrofit {
    return Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(BASE_URL)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
}

fun provideAuthenticationApi(retrofit: Retrofit): AuthenticationApi {
    return retrofit.create(AuthenticationApi::class.java)
}

fun provideUserApi(retrofit: Retrofit): UserApi {
    return retrofit.create(UserApi::class.java)
}

fun provideUnrestrictApi(retrofit: Retrofit): UnrestrictApi {
    return retrofit.create(UnrestrictApi::class.java)
}

fun provideStreamingApi(retrofit: Retrofit): StreamingApi {
    return retrofit.create(StreamingApi::class.java)
}

fun provideTorrentsApi(retrofit: Retrofit): TorrentsApi {
    return retrofit.create(TorrentsApi::class.java)
}

fun provideDownloadsApi(retrofit: Retrofit): DownloadApi {
    return retrofit.create(DownloadApi::class.java)
}

fun provideHostsApi(retrofit: Retrofit): HostsApi {
    return retrofit.create(HostsApi::class.java)
}

fun provideVariousApi(retrofit: Retrofit): VariousApi {
    return retrofit.create(VariousApi::class.java)
}

fun provideUpdateApi(retrofit: Retrofit): UpdateApi {
    return retrofit.create(UpdateApi::class.java)
}

fun provideCustomDownload(retrofit: Retrofit): CustomDownload {
    return retrofit.create(CustomDownload::class.java)
}

fun provideParser(
    preferences: SharedPreferences,
    classicClient: OkHttpClient,
    dohClient: OkHttpClient
): Parser {
    return Parser(preferences, classicClient, dohClient)
}

fun providePluginAdapter(): JsonAdapter<Plugin> {
    return Moshi.Builder().build().adapter(Plugin::class.java)
}