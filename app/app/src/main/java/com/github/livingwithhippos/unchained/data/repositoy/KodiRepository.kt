package com.github.livingwithhippos.unchained.data.repositoy

import com.github.livingwithhippos.unchained.data.model.KodiItem
import com.github.livingwithhippos.unchained.data.model.KodiOpenRequest
import com.github.livingwithhippos.unchained.data.model.KodiParams
import com.github.livingwithhippos.unchained.data.remote.KodiApi
import com.github.livingwithhippos.unchained.data.remote.KodiApiHelper
import com.github.livingwithhippos.unchained.data.remote.KodiApiHelperImpl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject

class KodiRepository @Inject constructor(
    private val client: OkHttpClient
) : BaseRepository() {

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

    private fun provideApiHelper(baseUrl: String): KodiApiHelper =
        KodiApiHelperImpl(provideApi(baseUrl))

    suspend fun openUrl(baseUrl: String, url: String) {

        val kodiApiHelper: KodiApiHelper = provideApiHelper(baseUrl)

        val kodiResponse = safeApiCall(
            call = {
                kodiApiHelper.openUrl(
                    KodiOpenRequest(
                        params = KodiParams(
                            item = KodiItem(
                                fileUrl = url
                            )
                        )
                    )
                )
            },
            errorMessage = "Error Sending url to Kodi"
        )

        return kodiResponse
    }
}