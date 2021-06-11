package com.github.livingwithhippos.unchained.data.remote

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import timber.log.Timber
import javax.inject.Inject

@ExperimentalCoroutinesApi
class KodiSocket @Inject constructor(private val client: OkHttpClient) {

    fun openWebSocket(url: String): Flow<WebSocketEvents> = callbackFlow {
        val request = Request.Builder()
            .url(url)
            .build()

        val webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                Timber.d("Aperto canale ${response.body}")
                trySend(WebSocketEvents.ConnectionOpened)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                super.onMessage(webSocket, bytes)
                Timber.d("Binary messagge received ${bytes}")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                Timber.d("Messagge received ${text}")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                trySend(WebSocketEvents.ConnectionError(t.message ?: response?.message ?: "Failure using the websocket for url $url"))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                Timber.d("Connection close")
                trySend(WebSocketEvents.ConnectionClosed)
            }
        }
        )
    }
}

sealed class WebSocketEvents {
    object ConnectionOpened : WebSocketEvents()
    object ConnectionClosed : WebSocketEvents()
    data class ConnectionError(val error: String) : WebSocketEvents()
}