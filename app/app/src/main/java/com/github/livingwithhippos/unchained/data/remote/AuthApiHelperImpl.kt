package com.github.livingwithhippos.unchained.data.remote

import android.content.SharedPreferences
import com.github.livingwithhippos.unchained.data.model.Authentication
import com.github.livingwithhippos.unchained.data.model.Secrets
import com.github.livingwithhippos.unchained.data.model.Token
import com.github.livingwithhippos.unchained.utilities.DebridProvider
import com.github.livingwithhippos.unchained.utilities.getDebridProvider
import javax.inject.Inject
import retrofit2.Response

class AuthApiHelperImpl
@Inject
constructor(
    private val preferences: SharedPreferences,
    private val authenticationApi: AuthenticationApi,
    private val allDebridAuthenticationApi: AllDebridAuthenticationApi,
) : AuthApiHelper {

    override suspend fun getAuthentication(): Response<Authentication> =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> authenticationApi.getAuthentication()
            DebridProvider.AllDebrid -> {
                val response = allDebridAuthenticationApi.getAuthentication()
                if (!response.isSuccessful) {
                    allDebridErrorResponse(code = response.code(), error = response.body()?.error)
                } else {
                    val body = response.body()
                    val data = body?.data
                    if (body?.status == "success" && data != null) Response.success(data.toAuthentication())
                    else allDebridErrorResponse(body?.error)
                }
            }
        }

    override suspend fun getSecrets(deviceCode: String): Response<Secrets> =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> authenticationApi.getSecrets(deviceCode = deviceCode)
            DebridProvider.AllDebrid -> Response.success(allDebridSecretsPlaceholder())
        }

    override suspend fun getToken(
        clientId: String,
        clientSecret: String,
        code: String,
    ): Response<Token> =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> authenticationApi.getToken(clientId, clientSecret, code)
            DebridProvider.AllDebrid -> Response.success(allDebridTokenPlaceholder(code))
        }

    override suspend fun checkPin(
        deviceCode: String,
        userCode: String,
    ): Response<AllDebridPinCheckResult> {
        val response = allDebridAuthenticationApi.checkPin(check = deviceCode, pin = userCode)
        if (!response.isSuccessful) {
            return allDebridErrorResponse(code = response.code(), error = response.body()?.error)
        }
        val body = response.body()
        val data = body?.data
        return if (body?.status == "success" && data != null) Response.success(data.toResult())
        else allDebridErrorResponse(body?.error)
    }
}
