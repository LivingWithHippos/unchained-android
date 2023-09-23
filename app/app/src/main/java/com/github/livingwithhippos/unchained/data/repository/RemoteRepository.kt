package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.di.ClassicClient
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.addHttpScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject


class RemoteRepository @Inject constructor(@ClassicClient private val client: OkHttpClient) {
    // todo: add https://mpv.io/manual/stable/

    suspend fun openUrl(
        baseUrl: String,
        port: Int = 9090,
        url: String,
        username: String? = null,
        password: String? = null
    ): EitherResult<Exception, Boolean> = withContext(Dispatchers.IO) {
        // https://wiki.videolan.org/Documentation:Modules/http_intf/#VLC_2.0.0_and_later
        // needs a password or it won't work:
        // vlc --http-host 0.0.0.0 --http-port 9090 --http-password pass
        val credential = okhttp3.Credentials.basic(username ?: "", password ?: "")
        val request = Request.Builder()
                .url("${addHttpScheme(baseUrl)}:$port/requests/status.xml?command=in_play&input=$url")
                .header("Authorization", credential)
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful)
                return@withContext EitherResult.Failure(IOException("Unexpected http code $response"))

            Timber.d(response.body!!.string())
            return@withContext EitherResult.Success(true)
        }
    }
}
