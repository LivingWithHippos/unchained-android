package com.github.livingwithhippos.unchained.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This class correspond to the JSON response from the authentication endpoint that starts the
 * authentication process.
 */
@JsonClass(generateAdapter = true)
data class Authentication(
    @param:Json(name = "device_code") val deviceCode: String,
    @param:Json(name = "user_code") val userCode: String,
    @param:Json(name = "interval") val interval: Int,
    @param:Json(name = "expires_in") val expiresIn: Int,
    @param:Json(name = "verification_url") val verificationUrl: String,
    @param:Json(name = "direct_verification_url") val directVerificationUrl: String,
)

/**
 * This class correspond to the JSON response from the secret endpoint, the second step in the
 * authentication process.
 */
@JsonClass(generateAdapter = true)
data class Secrets(
    @param:Json(name = "client_id") val clientId: String,
    @param:Json(name = "client_secret") val clientSecret: String,
)

/**
 * This class correspond to the JSON response from the token endpoint, the third and last step in
 * the authentication process. It can also be used to refresh an expired token
 */
@JsonClass(generateAdapter = true)
data class Token(
    @param:Json(name = "access_token") val accessToken: String,
    @param:Json(name = "expires_in") val expiresIn: Int,
    @param:Json(name = "token_type") val tokenType: String,
    @param:Json(name = "refresh_token") val refreshToken: String,
)

enum class UserAction {
    PERMISSION_DENIED,
    TFA_NEEDED,
    TFA_PENDING,
    IP_NOT_ALLOWED,
    UNKNOWN,
    NETWORK_ERROR,
    RETRY_LATER,
}
