package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.ApiConversionError
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.data.model.NetworkResponse
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber
import java.io.IOException

/**
 * Base repository class to be extended by other repositories.
 * Manages the calls between retrofit and the actual repositories.
 */
open class BaseRepository(private val protoStore: ProtoStore) {

    // todo: inject this
    private val jsonAdapter: JsonAdapter<APIError> = Moshi.Builder()
        .build()
        .adapter(APIError::class.java)

    suspend fun <T : Any> safeApiCall(call: suspend () -> Response<T>, errorMessage: String): T? {
        val result: NetworkResponse<T> = try {
            safeApiResult(call, errorMessage)
        } catch (e: Exception) {
            NetworkResponse.Error(e)
        }

        var data: T? = null

        when (result) {
            is NetworkResponse.Success ->
                data = result.data
            is NetworkResponse.SuccessEmptyBody ->
                Timber.d("Successful call with empty body : ${result.code}")
            is NetworkResponse.Error ->
                Timber.d(errorMessage)
        }

        return data
    }

    private suspend fun <T : Any> safeApiResult(
        call: suspend () -> Response<T>,
        errorMessage: String
    ): NetworkResponse<T> {
        try {
            val response: Response<T> = call.invoke()
            if (response.isSuccessful) {
                val body = response.body()
                return if (body != null)
                    NetworkResponse.Success(body)
                else
                    NetworkResponse.SuccessEmptyBody(response.code())
            }
        } catch (e: Exception) {
            NetworkResponse.Error(e)
        }

        return NetworkResponse.Error(IOException("Error Occurred while getting api result, error : $errorMessage"))
    }

    suspend fun <T : Any> eitherApiResult(
        call: suspend () -> Response<T>,
        errorMessage: String
    ): EitherResult<UnchainedNetworkException, T> = withContext(Dispatchers.IO) {
        val response: Response<T> = try {
            call.invoke()
        } catch (e: Exception) {
            return@withContext EitherResult.Failure(NetworkError(-1, errorMessage))
        }
        val code = response.code()
        if (response.isSuccessful) {
            val body: T? = response.body()
            return@withContext if (body != null)
                EitherResult.Success(body)
            else
                // todo: some calls return nothing and this is actually a Success, manage them
                /**
                 * /disable_access_token
                 * /downloads/delete/{id}
                 * /torrents/selectFiles/{id}
                 * /torrents/delete/{id}
                 * /settings/update
                 * /settings/convertPoints
                 * /settings/changePassword
                 * /settings/avatarFile
                 * /settings/avatarDelete
                 */
                EitherResult.Failure(EmptyBodyError(code))
        } else {
            try {
                val error: APIError? = jsonAdapter.fromJson(response.errorBody()!!.string())
                return@withContext if (error != null)
                    EitherResult.Failure(error)
                else
                    EitherResult.Failure(ApiConversionError(-1))
            } catch (e: IOException) {
                // todo: analyze error to return code
                return@withContext EitherResult.Failure(NetworkError(-1, "$errorMessage, http code $code"))
            }
        }
    }

    /**
     * Get the access token saved in the db. Used by most calls to RD APIs
     * Throws an exception if token is missing or malformed
     * @return the token string
     */
    suspend fun getToken(): String {
        val token = protoStore.getCredentials().accessToken
        if (token.isBlank() || token.length < 5)
            throw IllegalArgumentException("Loaded token was empty or wrong: $token")

        return token
    }
}
