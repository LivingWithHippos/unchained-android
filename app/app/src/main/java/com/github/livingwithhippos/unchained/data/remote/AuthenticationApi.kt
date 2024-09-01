package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.Authentication
import com.github.livingwithhippos.unchained.data.model.Secrets
import com.github.livingwithhippos.unchained.data.model.Token
import com.github.livingwithhippos.unchained.utilities.OPEN_SOURCE_CLIENT_ID
import com.github.livingwithhippos.unchained.utilities.OPEN_SOURCE_GRANT_TYPE
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * This interface is used by Retrofit to manage all the REST calls to the endpoints needed to
 * authenticate the user
 */
interface AuthenticationApi {

    @GET("device/code")
    suspend fun getAuthentication(
        @Query("client_id") id: String = OPEN_SOURCE_CLIENT_ID,
        @Query("new_credentials") newCredentials: String = "yes",
    ): Response<Authentication>

    @GET("device/credentials")
    suspend fun getSecrets(
        @Query("client_id") id: String = OPEN_SOURCE_CLIENT_ID,
        @Query("code") deviceCode: String,
    ): Response<Secrets>

    /** This is also used to refresh the token. */
    @FormUrlEncoded
    @POST("token")
    suspend fun getToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String = OPEN_SOURCE_GRANT_TYPE,
    ): Response<Token>
}
