package com.github.livingwithhippos.unchained.data.repositoy

import android.util.Log
import com.github.livingwithhippos.unchained.BuildConfig
import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.CompleteNetworkResponse
import com.github.livingwithhippos.unchained.data.model.NetworkResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

/**
 * Base repository class to be extended by other repositories.
 * Manages the calls between retrofit and the actual repositories.
 */
open class BaseRepository {

    //todo: inject this
    private val jsonAdapter: JsonAdapter<APIError> = Moshi.Builder()
        .build()
        .adapter(APIError::class.java)

    suspend fun <T : Any> safeApiCall(call: suspend () -> Response<T>, errorMessage: String): T? {

        val result: NetworkResponse<T> = safeApiResult(call, errorMessage)
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
                return NetworkResponse.SuccessEmptyBody(response.code())
        }

        return NetworkResponse.Error(IOException("Error Occurred while getting api result, error : $errorMessage"))
    }

    suspend fun safeEmptyApiCall(call: suspend () -> Call<ResponseBody>, errorMessage: String): Int? {

        //fixme: this fun returns always -1
        var responseCode = -1

        val callback = object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                responseCode = response.code()
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                if (BuildConfig.DEBUG)
                    Log.d("BaseRepository", "Error during safeEmptyApiCall: ${t.message}")
            }
        }

        call.invoke().enqueue(callback)

        return responseCode
    }

    public suspend fun <T : Any> errorApiResult(
        call: suspend () -> Response<T>,
        errorMessage: String
    ): CompleteNetworkResponse<T?, APIError?> {
        val response = call.invoke()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null)
                return CompleteNetworkResponse.Success(body)
            else
                return CompleteNetworkResponse.SuccessEmptyBody(response.code())
        } else {
            try {
                val error: APIError? = jsonAdapter.fromJson(response.errorBody()!!.string())
                return CompleteNetworkResponse.RDError(error)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return CompleteNetworkResponse.Error(errorMessage)
    }
}