package com.github.livingwithhippos.unchained.data.repositoy

import android.util.Log
import com.github.livingwithhippos.unchained.BuildConfig
import com.github.livingwithhippos.unchained.data.model.NetworkResponse
import retrofit2.Response
import java.io.IOException

/**
 * Base repository class to be extended by other repositories.
 * Manages the calls between retrofit and the actual repositories.
 */
open class BaseRepository {

    suspend fun <T : Any> unsafeApiCall(call: suspend () -> Response<T>, errorMessage: String): T? {

        val result: NetworkResponse<T> = unsafeApiResult(call, errorMessage)
        var data: T? = null

        when (result) {
            is NetworkResponse.Success ->
                data = result.data
            is NetworkResponse.SuccessEmptyBody ->
                if (BuildConfig.DEBUG)
                    Log.d("BaseRepository", "Successful call with empty body : ${result.code}")
            is NetworkResponse.Error ->
                if (BuildConfig.DEBUG)
                    Log.d("BaseRepository", errorMessage)

        }

        return data
    }

    suspend fun <T : Any> safeApiCall(call: suspend () -> Response<T>, errorMessage: String): T? {

        val result: NetworkResponse<T> = safeApiResult(call, errorMessage)
        var data: T? = null

        when (result) {
            is NetworkResponse.Success ->
                data = result.data
            // todo: temporary workaround. Add support for empty body success
            is NetworkResponse.SuccessEmptyBody ->
                if (BuildConfig.DEBUG)
                    Log.d("BaseRepository", "Successful call with empty body : ${result.code}")
            is NetworkResponse.Error ->
                if (BuildConfig.DEBUG)
                    Log.d("BaseRepository", errorMessage)
        }

        return data
    }

    private suspend fun <T : Any> safeApiResult(
        call: suspend () -> Response<T>,
        errorMessage: String
    ): NetworkResponse<T> {
        val response = call.invoke()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null)
                return NetworkResponse.Success(body)
            else
            // todo: temporary workaround. Add support for empty body success
                return NetworkResponse.SuccessEmptyBody(response.code())
        }

        return NetworkResponse.Error(IOException("Error Occurred while getting api result, error : $errorMessage"))
    }

    private suspend fun <T : Any> unsafeApiResult(
        call: suspend () -> Response<T>,
        errorMessage: String
    ): NetworkResponse<T> {
        val response = call.invoke()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null)
                return NetworkResponse.Success(body)
            else
            // todo: temporary workaround. Add support for empty body success
                return NetworkResponse.SuccessEmptyBody(response.code())
        } else {
            //todo: implement error handling as JSON APIError class
            /*
            if (response.code() in 400..599) {
                val moshi = Moshi.Builder().build()
                val adapter: JsonAdapter<APIError> =
                    moshi.adapter(APIError::class.java)
                // todo: wrap with withContext(Dispatchers.IO)?
                val apiError = adapter.fromJson(response.errorBody().toString())
                if (apiError!=null)
                    throw APIException(apiError)
            }*/

        }

        return NetworkResponse.Error(IOException("Error Occurred while getting api result, error : $errorMessage"))
    }
}