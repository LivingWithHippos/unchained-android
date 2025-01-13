package com.github.livingwithhippos.unchained.data.repository

import android.util.Base64
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.KodiGenericResponse
import com.github.livingwithhippos.unchained.data.model.KodiItem
import com.github.livingwithhippos.unchained.data.model.KodiParams
import com.github.livingwithhippos.unchained.data.model.KodiRequest
import com.github.livingwithhippos.unchained.data.model.KodiResponse
import com.github.livingwithhippos.unchained.data.remote.KodiApi
import com.github.livingwithhippos.unchained.data.remote.KodiApiHelper
import com.github.livingwithhippos.unchained.data.remote.KodiApiHelperImpl
import com.github.livingwithhippos.unchained.utilities.addHttpScheme
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber

class KodiRepository(protoStore: ProtoStore, private val client: OkHttpClient) :
    BaseRepository(protoStore) {

    // todo: conflict with other Retrofit with qualifiers?
    private fun provideRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .client(client)
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    private fun provideApi(baseUrl: String): KodiApi {
        return provideRetrofit(baseUrl).create(KodiApi::class.java)
    }

    private fun provideApiHelper(baseUrl: String): KodiApiHelper {
        return KodiApiHelperImpl(provideApi(baseUrl))
    }

    suspend fun getVolume(
        baseUrl: String,
        port: Int,
        username: String? = null,
        password: String? = null,
    ): KodiGenericResponse? {
        try {
            val kodiApiHelper: KodiApiHelper = provideApiHelper("${addHttpScheme(baseUrl)}:$port/")
            val kodiResponse =
                safeApiCall(
                    call = {
                        kodiApiHelper.getVolume(
                            request =
                                KodiRequest(
                                    method = "Application.GetProperties",
                                    params = KodiParams(properties = listOf("volume")),
                                ),
                            auth = encodeAuthentication(username, password),
                        )
                    },
                    errorMessage = "Error getting Kodi volume",
                )

            return kodiResponse
        } catch (e: Exception) {
            Timber.e(e)
            return null
        }
    }

    suspend fun openUrl(
        baseUrl: String,
        port: Int,
        url: String,
        username: String? = null,
        password: String? = null,
    ): KodiResponse? {

        try {
            val kodiApiHelper: KodiApiHelper =
                if (baseUrl.startsWith("http", ignoreCase = true))
                    provideApiHelper("$baseUrl:$port/")
                else provideApiHelper("http://$baseUrl:$port/")

            val kodiResponse =
                safeApiCall(
                    call = {
                        kodiApiHelper.openUrl(
                            request =
                                KodiRequest(
                                    method = "Player.Open",
                                    params = KodiParams(item = KodiItem(fileUrl = url)),
                                ),
                            auth = encodeAuthentication(username, password),
                        )
                    },
                    errorMessage = "Error Sending url to Kodi",
                )

            return kodiResponse
        } catch (e: Exception) {
            Timber.e(e)
            return null
        }
    }

    private fun encodeAuthentication(username: String?, password: String?): String? {
        return if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            "Basic " +
                Base64.encodeToString("$username:$password".toByteArray(), Base64.DEFAULT).trim()
        } else null
    }
}
