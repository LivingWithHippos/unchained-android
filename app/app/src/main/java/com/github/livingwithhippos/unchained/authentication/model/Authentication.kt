package com.github.livingwithhippos.unchained.authentication.model

import com.github.livingwithhippos.unchained.utilities.OPEN_SOURCE_CLIENT_ID
import com.github.livingwithhippos.unchained.utilities.OPEN_SOURCE_GRANT_TYPE
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.*

@JsonClass(generateAdapter = true)
data class Authentication(
    @Json(name = "device_code")
    val deviceCode: String,
    @Json(name = "user_code")
    val userCode: String,
    @Json(name = "interval")
    val interval: Int,
    @Json(name = "expires_in")
    val expiresIn: Int,
    @Json(name = "verification_url")
    val verificationUrl: String,
    @Json(name = "direct_verification_url")
    val directVerificationUrl: String
)

@JsonClass(generateAdapter = true)
data class Secrets(
    @Json(name = "client_id")
    val clientId: String,
    @Json(name = "client_secret")
    val clientSecret: String
)

@JsonClass(generateAdapter = true)
data class Token(
    @Json(name = "access_token")
    val accessToken: String,
    @Json(name = "expires_in")
    val expiresIn: String,
    @Json(name = "token_type")
    val tokenType: String,
    @Json(name = "refresh_token")
    val refreshToken: String
)

interface AuthenticationApi {

    @GET("device/code")
    suspend fun getAuthentication(
        @Query("client_id") id: String = OPEN_SOURCE_CLIENT_ID,
        @Query("new_credentials") newCredentials: String = "yes"
    ): Response<Authentication>

    @GET("device/credentials")
    suspend fun getSecrets(
        @Query("client_id") id: String = OPEN_SOURCE_CLIENT_ID,
        @Query("code") deviceCode: String
    ): Response<Secrets>


    @FormUrlEncoded
    @POST("token")
    suspend fun getToken(
        @Field("client_id") clientId: String= OPEN_SOURCE_CLIENT_ID,
        @Field("client_secret") clientSecret: String,
        @Field("code") deviceCode: String,
        @Field("grant_type") grantType: String = OPEN_SOURCE_GRANT_TYPE
    ): Response<Token>
}