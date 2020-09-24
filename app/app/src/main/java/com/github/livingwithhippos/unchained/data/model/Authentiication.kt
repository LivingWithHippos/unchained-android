package com.github.livingwithhippos.unchained.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This class correspond to the JSON response from the authentication endpoint that starts the authentication process.
 */
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

/**
 * This class correspond to the JSON response from the secret endpoint, the second step in the authentication process.
 */
@JsonClass(generateAdapter = true)
data class Secrets(
    @Json(name = "client_id")
    val clientId: String,
    @Json(name = "client_secret")
    val clientSecret: String
)

/**
 * This class correspond to the JSON response from the token endpoint, the third and last step in the authentication process.
 * It can also be used to refresh an expired token
 */
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