package com.github.livingwithhippos.unchained.base.model.repositories

import android.util.Log
import com.github.livingwithhippos.unchained.base.model.network.NetworkResponse
import retrofit2.Response
import java.io.IOException

open class BaseRepository {

    suspend fun <T : Any> safeApiCall(call: suspend () -> Response<T>, errorMessage: String): T? {

        val result: NetworkResponse<T> = safeApiResult(call, errorMessage)
        var data: T? = null

        when (result) {
            is NetworkResponse.Success ->
                data = result.data
            // todo: temporary workaround. Add support for empty body success
            is NetworkResponse.SuccessEmptyBody ->
                Log.d("BaseRepository", "Successful call with empty body : ${result.code}")
            is NetworkResponse.Error -> {
                Log.d("BaseRepository", "$errorMessage - Exception : ${result.exception}")
            }
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
}