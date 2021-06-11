package com.github.livingwithhippos.unchained.data.repositoy

import com.github.livingwithhippos.unchained.data.model.KodiItem
import com.github.livingwithhippos.unchained.data.model.KodiOpenRequest
import com.github.livingwithhippos.unchained.data.model.KodiParams
import com.github.livingwithhippos.unchained.data.model.KodiResponse
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

    private fun provideApiHelper(baseUrl: String): KodiApiHelper {
        val apiHelper = KodiApiHelperImpl(provideApi(baseUrl))
        return apiHelper
    }


    suspend fun openUrl(baseUrl: String, port: Int, url: String): KodiResponse? {

        val kodiApiHelper: KodiApiHelper = provideApiHelper("http://$baseUrl:$port/")

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