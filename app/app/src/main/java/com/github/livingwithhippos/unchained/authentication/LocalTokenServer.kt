package com.github.livingwithhippos.unchained.authentication

import java.io.BufferedReader
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.concurrent.thread
import timber.log.Timber

/**
 * Minimal, temporary HTTP server used on Android TV to receive a single text value from another
 * device on the same local network (e.g. the user's phone), since typing with a remote is painful.
 * The authentication screen uses it for the private Real-Debrid token and the new download screen
 * for a link or magnet. It serves a single form page and accepts one valid submission, after which
 * it stops itself. It must also be stopped when the screen that started it is left.
 *
 * Security model, following what LocalSend does for its browser flows and what went wrong with
 * always-on unauthenticated servers like the ES File Explorer one (CVE-2019-6447):
 * - it only runs while its screen is visible, and stops itself after [SERVER_LIFETIME_MS] anyway
 * - it binds only to the local network interface, never to all the interfaces
 * - submissions must include the random [pin] displayed on the TV, so neither another host on the
 *   network nor a malicious web page loaded on one (CSRF/DNS rebinding) can plant its own value
 * - it stops after [MAX_PIN_FAILURES] wrong PINs or after the first valid submission
 * - it never sends any data out except the static form page
 *
 * The value travels in plain http on the local network, which is acceptable for a short lived,
 * PIN protected server: TLS would require a self signed certificate that phone browsers refuse.
 *
 * @param pages localized texts used to build the served web pages
 * @param isValueValid decides whether a submitted value is acceptable; rejected values get the
 *   error page and the server keeps waiting
 * @param onValueReceived called with the submitted value, from a background thread
 * @param onStopped called when the server stops itself (timeout, too many wrong PINs or value
 *   received), from a background thread. Not called by [stop].
 */
class LocalTokenServer(
    private val pages: Pages,
    private val isValueValid: (String) -> Boolean,
    private val onValueReceived: (String) -> Unit,
    private val onStopped: () -> Unit = {},
    /** The PIN that must be typed in the served form, to be displayed on the TV */
    val pin: String = generatePin(),
    /**
     * Whether the PIN can be skipped, decided once by the caller before this server even starts
     * (see the phone input session), rather than derived from anything the client sends per
     * request such as a cookie: a phone's cookie jar is not something this can rely on, since each
     * phone input starts a fresh server at a fresh address, and depending on the browser or QR
     * scanner preview used to open it, an earlier cookie might not carry over.
     */
    private val trusted: Boolean = false,
    /** Called after a first valid PIN submission, so the caller can trust later phone inputs too. */
    private val onTrustEstablished: () -> Unit = {},
) {

    /**
     * Localized texts for the served pages. When [linkUrl] and [linkLabel] are set, a plain link
     * is shown above the form, e.g. the Real-Debrid token page for the authentication flow.
     */
    data class Pages(
        val title: String,
        val fieldLabel: String,
        val pinLabel: String,
        val submitLabel: String,
        val successMessage: String,
        val errorMessage: String,
        val wrongPinMessage: String,
        val linkUrl: String? = null,
        val linkLabel: String? = null,
    )

    private var serverSocket: ServerSocket? = null

    /**
     * Bind the first free port in [PORT_RANGE] on the local network interface and start serving.
     *
     * @return the reachable http address, or null if no local network address or free port was
     *   found
     */
    fun start(): String? {
        val address = findSiteLocalAddress() ?: return null
        val socket = bindFirstFreePort(address) ?: return null
        socket.soTimeout = ACCEPT_TIMEOUT_MS
        serverSocket = socket
        thread(isDaemon = true, name = "unchained-token-server") { serve(socket) }
        return "http://${address.hostAddress}:${socket.localPort}"
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Timber.w(e, "Error closing the token server socket")
        }
        serverSocket = null
    }

    private fun serve(socket: ServerSocket) {
        val deadline = System.currentTimeMillis() + SERVER_LIFETIME_MS
        var pinFailures = 0
        try {
            while (!socket.isClosed) {
                val result =
                    try {
                        socket.accept().use { client -> handleClient(client) }
                    } catch (e: SocketTimeoutException) {
                        RequestResult.NONE
                    }
                if (result == RequestResult.WRONG_PIN) pinFailures++
                val quit =
                    when {
                        // a valid value was received, only one submission is accepted
                        result == RequestResult.VALUE_RECEIVED -> true
                        // too many wrong PINs, stop instead of allowing a brute force
                        pinFailures >= MAX_PIN_FAILURES -> true
                        // don't run forever if the screen stays open
                        System.currentTimeMillis() > deadline -> true
                        else -> false
                    }
                if (quit) {
                    stop()
                    onStopped()
                }
            }
        } catch (e: IOException) {
            // the server socket was closed, the serving thread ends
            Timber.d("Token server stopped: ${e.message}")
        }
    }

    private enum class RequestResult {
        NONE,
        WRONG_PIN,
        VALUE_RECEIVED,
    }

    /** Serve a single http request: the form page on GET /, the form processing on POST / */
    private fun handleClient(client: Socket): RequestResult {
        return try {
            client.soTimeout = CLIENT_TIMEOUT_MS
            val reader = client.getInputStream().bufferedReader()
            val requestLine = reader.readLine()?.take(MAX_REQUEST_LINE_LENGTH) ?: return RequestResult.NONE
            var contentLength = 0
            while (true) {
                val line = reader.readLine() ?: return RequestResult.NONE
                if (line.isBlank()) break
                if (line.startsWith("content-length:", ignoreCase = true)) {
                    contentLength = line.substringAfter(':').trim().toIntOrNull() ?: 0
                }
            }
            val parts = requestLine.split(' ')
            val method = parts.getOrNull(0).orEmpty()
            val path = parts.getOrNull(1).orEmpty().substringBefore('?')

            var result = RequestResult.NONE
            val response: Response =
                when {
                    path != "/" -> Response(404, messagePage(pages.errorMessage))
                    method.equals("GET", ignoreCase = true) -> Response(200, formPage(includePin = !trusted))
                    method.equals("POST", ignoreCase = true) -> {
                        val fields = readForm(reader, contentLength)
                        val value = fields["value"]?.trim()
                        when {
                            !trusted && !isPinValid(fields["pin"]?.trim()) -> {
                                result = RequestResult.WRONG_PIN
                                Response(401, messagePage(pages.wrongPinMessage))
                            }
                            value.isNullOrBlank() || !isValueValid(value) ->
                                Response(200, messagePage(pages.errorMessage))
                            else -> {
                                result = RequestResult.VALUE_RECEIVED
                                onValueReceived(value)
                                if (!trusted) onTrustEstablished()
                                Response(200, messagePage(pages.successMessage))
                            }
                        }
                    }
                    else -> Response(405, messagePage(pages.errorMessage))
                }

            val body = response.page.toByteArray(Charsets.UTF_8)
            val headers =
                "HTTP/1.1 ${response.status} ${statusName(response.status)}\r\n" +
                    "Content-Type: text/html; charset=utf-8\r\n" +
                    "Content-Length: ${body.size}\r\n" +
                    "Cache-Control: no-store\r\n" +
                    "X-Content-Type-Options: nosniff\r\n" +
                    "X-Frame-Options: DENY\r\n" +
                    "Referrer-Policy: no-referrer\r\n" +
                    "Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; " +
                    "form-action 'self'\r\n" +
                    "Connection: close\r\n\r\n"
            client.getOutputStream().apply {
                write(headers.toByteArray(Charsets.UTF_8))
                write(body)
                flush()
            }
            result
        } catch (e: IOException) {
            Timber.w(e, "Error handling a token server request")
            RequestResult.NONE
        }
    }

    private data class Response(val status: Int, val page: String)

    private fun statusName(status: Int): String =
        when (status) {
            200 -> "OK"
            401 -> "Unauthorized"
            404 -> "Not Found"
            else -> "Method Not Allowed"
        }

    /** Compare the submitted PIN in constant time */
    private fun isPinValid(submitted: String?): Boolean {
        if (submitted == null) return false
        return MessageDigest.isEqual(submitted.toByteArray(), pin.toByteArray())
    }

    /** Read an url encoded form body into its fields */
    private fun readForm(reader: BufferedReader, contentLength: Int): Map<String, String> {
        if (contentLength <= 0) return emptyMap()
        val buffer = CharArray(contentLength.coerceAtMost(MAX_BODY_LENGTH))
        var read = 0
        while (read < buffer.size) {
            val r = reader.read(buffer, read, buffer.size - read)
            if (r == -1) break
            read += r
        }
        return String(buffer, 0, read)
            .split('&')
            .mapNotNull { field ->
                val separator = field.indexOf('=')
                if (separator <= 0) null
                else
                    try {
                        field.take(separator) to
                            URLDecoder.decode(field.substring(separator + 1), "UTF-8")
                    } catch (e: IllegalArgumentException) {
                        // malformed url encoding
                        null
                    }
            }
            .toMap()
    }

    /** Find the local network (site local) ipv4 address of this device, if any */
    private fun findSiteLocalAddress(): InetAddress? =
        try {
            NetworkInterface.getNetworkInterfaces()
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.asSequence() }
                .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
        } catch (e: Exception) {
            Timber.w(e, "Error looking for the local network address")
            null
        }

    /** Bind the first free port of [PORT_RANGE], only on [address], not on all the interfaces */
    private fun bindFirstFreePort(address: InetAddress): ServerSocket? {
        for (port in PORT_RANGE) {
            try {
                return ServerSocket(port, BACKLOG, address)
            } catch (e: IOException) {
                // port already in use, try the next one
            }
        }
        Timber.w("No free port found for the token server in $PORT_RANGE")
        return null
    }

    private fun formPage(includePin: Boolean): String {
        val link =
            if (pages.linkUrl != null && pages.linkLabel != null)
                // CSP only restricts what the page loads or submits, not plain link navigation
                """<p><a href="${pages.linkUrl.escapeHtml()}" target="_blank" rel="noopener">""" +
                    """${pages.linkLabel.escapeHtml()}</a></p>"""
            else ""
        // a trusted phone (already passed the PIN this session) is not asked for it again
        val pinField =
            if (includePin)
                """<label for="pin">${pages.pinLabel.escapeHtml()}</label>""" +
                    """<input type="text" id="pin" name="pin" inputmode="numeric" autocomplete="off">"""
            else ""
        return """
        <!DOCTYPE html>
        <html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>${pages.title.escapeHtml()}</title>
        <style>$PAGE_STYLE</style></head>
        <body><h2>${pages.title.escapeHtml()}</h2>
        $link
        <form method="post" action="/">
        <label for="value">${pages.fieldLabel.escapeHtml()}</label>
        <input type="text" id="value" name="value" autocomplete="off" autofocus>
        $pinField
        <button type="submit">${pages.submitLabel.escapeHtml()}</button>
        </form></body></html>
        """
            .trimIndent()
    }

    private fun messagePage(message: String): String =
        """
        <!DOCTYPE html>
        <html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>${pages.title.escapeHtml()}</title>
        <style>$PAGE_STYLE</style></head>
        <body><h2>${pages.title.escapeHtml()}</h2>
        <p>${message.escapeHtml()}</p></body></html>
        """
            .trimIndent()

    private fun String.escapeHtml(): String =
        replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    companion object {
        /** A fresh random 6 digit PIN, generated with a cryptographically strong source */
        fun generatePin(): String = "%06d".format(SecureRandom().nextInt(1_000_000))

        // predictable ports, easy to type manually if the qr code cannot be scanned
        private val PORT_RANGE = 8080..8100
        private const val BACKLOG = 4
        private const val ACCEPT_TIMEOUT_MS = 15_000
        private const val CLIENT_TIMEOUT_MS = 5000
        private const val SERVER_LIFETIME_MS = 10 * 60 * 1000L
        private const val MAX_PIN_FAILURES = 5
        private const val MAX_REQUEST_LINE_LENGTH = 2000
        private const val MAX_BODY_LENGTH = 10_000
        private const val PAGE_STYLE =
            "body{font-family:sans-serif;margin:8vh auto;max-width:26em;padding:0 1em;" +
                "background:#121212;color:#eee}" +
                "input,button{font-size:1.1em;width:100%;box-sizing:border-box;margin-top:1em;" +
                "padding:0.6em;border-radius:8px;border:1px solid #666;background:#1e1e1e;color:#eee}" +
                "button{background:#7b5cd6;color:#fff;border:none}" +
                "label{display:block;margin-top:1em}" +
                "a{color:#a58cf0}"
    }
}
