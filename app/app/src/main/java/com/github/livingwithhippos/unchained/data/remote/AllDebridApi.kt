package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.Authentication
import com.github.livingwithhippos.unchained.data.model.AvailableHost
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.InnerTorrentFile
import com.github.livingwithhippos.unchained.data.model.Secrets
import com.github.livingwithhippos.unchained.data.model.Token
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UploadedTorrent
import com.github.livingwithhippos.unchained.data.model.User
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AllDebridAuthenticationApi {
    @GET("v4.1/pin/get")
    suspend fun getAuthentication(): Response<AllDebridPinGetResponse>

    @FormUrlEncoded
    @POST("v4/pin/check")
    suspend fun checkPin(
        @Field("check") check: String,
        @Field("pin") pin: String,
    ): Response<AllDebridPinCheckResponse>
}

interface AllDebridUserApi {
    @GET("v4/user")
    suspend fun getUser(@Header("Authorization") token: String): Response<AllDebridUserResponse>
}

interface AllDebridUnrestrictApi {
    @FormUrlEncoded
    @POST("v4/link/unlock")
    suspend fun unlockLink(
        @Header("Authorization") token: String,
        @Field("link") link: String,
        @Field("password") password: String? = null,
    ): Response<AllDebridLinkUnlockResponse>

    @FormUrlEncoded
    @POST("v4/link/redirector")
    suspend fun getRedirectorLinks(
        @Header("Authorization") token: String,
        @Field("links[]") links: List<String>,
    ): Response<AllDebridRedirectorResponse>
}

interface AllDebridUserLinksApi {
    @GET("v4/user/history")
    suspend fun getUserHistory(
        @Header("Authorization") token: String,
    ): Response<AllDebridUserHistoryResponse>

    @FormUrlEncoded
    @POST("v4/user/links/save")
    suspend fun saveLinks(
        @Header("Authorization") token: String,
        @Field("links[]") links: List<String>,
    ): Response<AllDebridMessageResponse>

    @FormUrlEncoded
    @POST("v4/user/links/delete")
    suspend fun deleteLinks(
        @Header("Authorization") token: String,
        @Field("links[]") links: List<String>,
    ): Response<AllDebridMessageResponse>
}

interface AllDebridTorrentsApi {
    @GET("v4/hosts")
    suspend fun getHosts(): Response<AllDebridHostsResponse>

    @FormUrlEncoded
    @POST("v4/magnet/upload")
    suspend fun addMagnet(
        @Header("Authorization") token: String,
        @Field("magnets[]") magnets: List<String>,
    ): Response<AllDebridMagnetUploadResponse>

    @Multipart
    @POST("v4/magnet/upload/file")
    suspend fun addTorrent(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
    ): Response<AllDebridMagnetUploadFileResponse>

    @FormUrlEncoded
    @POST("v4.1/magnet/status")
    suspend fun getMagnetStatus(
        @Header("Authorization") token: String,
        @Field("id") id: String? = null,
        @Field("status") status: String? = null,
    ): Response<AllDebridMagnetStatusResponse>

    @FormUrlEncoded
    @POST("v4/magnet/files")
    suspend fun getMagnetFiles(
        @Header("Authorization") token: String,
        @Field("id[]") ids: List<String>,
    ): Response<AllDebridMagnetFilesResponse>

    @FormUrlEncoded
    @POST("v4/magnet/delete")
    suspend fun deleteMagnet(
        @Header("Authorization") token: String,
        @Field("id") id: String,
    ): Response<AllDebridMessageResponse>
}

@JsonClass(generateAdapter = true)
data class AllDebridErrorEnvelope(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "error") val error: AllDebridError?,
)

@JsonClass(generateAdapter = true)
data class AllDebridError(
    @param:Json(name = "code") val code: String,
    @param:Json(name = "message") val message: String,
    @param:Json(name = "token") val token: String? = null,
)

@JsonClass(generateAdapter = true)
data class AllDebridPinGetResponse(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "data") val data: AllDebridPinGetData?,
    @param:Json(name = "error") val error: AllDebridError?,
)

@JsonClass(generateAdapter = true)
data class AllDebridPinGetData(
    @param:Json(name = "pin") val pin: String,
    @param:Json(name = "check") val check: String,
    @param:Json(name = "expires_in") val expiresIn: Int,
    @param:Json(name = "user_url") val userUrl: String,
    @param:Json(name = "base_url") val baseUrl: String,
)

@JsonClass(generateAdapter = true)
data class AllDebridPinCheckResponse(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "data") val data: AllDebridPinCheckData?,
    @param:Json(name = "error") val error: AllDebridError?,
)

@JsonClass(generateAdapter = true)
data class AllDebridPinCheckData(
    @param:Json(name = "activated") val activated: Boolean,
    @param:Json(name = "expires_in") val expiresIn: Int,
    @param:Json(name = "apikey") val apiKey: String? = null,
)

@JsonClass(generateAdapter = true)
data class AllDebridUserResponse(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "data") val data: AllDebridUserData?,
    @param:Json(name = "error") val error: AllDebridError?,
)

@JsonClass(generateAdapter = true)
data class AllDebridUserData(@param:Json(name = "user") val user: AllDebridUserInfo?)

@JsonClass(generateAdapter = true)
data class AllDebridUserInfo(
    @param:Json(name = "username") val username: String,
    @param:Json(name = "email") val email: String,
    @param:Json(name = "isPremium") val isPremium: Boolean,
    @param:Json(name = "isSubscribed") val isSubscribed: Boolean,
    @param:Json(name = "premiumUntil") val premiumUntil: String,
    @param:Json(name = "lang") val lang: String,
    @param:Json(name = "fidelityPoints") val fidelityPoints: Int,
)

@JsonClass(generateAdapter = true)
data class AllDebridLinkUnlockResponse(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "data") val data: AllDebridLinkUnlockData?,
    @param:Json(name = "error") val error: AllDebridError?,
)

@JsonClass(generateAdapter = true)
data class AllDebridLinkUnlockData(
    @param:Json(name = "link") val download: String?,
    @param:Json(name = "host") val host: String,
    @param:Json(name = "filename") val filename: String,
    @param:Json(name = "filesize") val filesize: Long,
    @param:Json(name = "id") val id: String?,
    @param:Json(name = "hostDomain") val hostDomain: String?,
    @param:Json(name = "streams") val streams: List<AllDebridStream>?,
)

@JsonClass(generateAdapter = true)
data class AllDebridStream(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "ext") val ext: String?,
    @param:Json(name = "quality") val quality: String?,
    @param:Json(name = "name") val name: String?,
)

@JsonClass(generateAdapter = true)
data class AllDebridHostsResponse(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "data") val data: AllDebridHostsData?,
    @param:Json(name = "error") val error: AllDebridError?,
)

@JsonClass(generateAdapter = true)
data class AllDebridHostsData(
    @param:Json(name = "hosts") val hosts: Map<String, AllDebridHostService>?,
    @param:Json(name = "streams") val streams: Map<String, AllDebridHostService>?,
    @param:Json(name = "redirectors") val redirectors: Map<String, AllDebridHostService>?,
)

@JsonClass(generateAdapter = true)
data class AllDebridHostService(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "domains") val domains: List<String>?,
    @param:Json(name = "regexp") val regexp: String?,
    @param:Json(name = "regexps") val regexps: List<String>?,
)

@JsonClass(generateAdapter = true)
data class AllDebridMagnetUploadResponse(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "data") val data: AllDebridMagnetUploadData?,
    @param:Json(name = "error") val error: AllDebridError?,
)

@JsonClass(generateAdapter = true)
data class AllDebridMagnetUploadData(
    @param:Json(name = "magnets") val magnets: List<AllDebridUploadedMagnet>?,
)

@JsonClass(generateAdapter = true)
data class AllDebridUploadedMagnet(
    @param:Json(name = "magnet") val magnet: String?,
    @param:Json(name = "file") val file: String?,
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "id") val id: Long?,
    @param:Json(name = "error") val error: AllDebridError?,
)

@JsonClass(generateAdapter = true)
data class AllDebridMagnetUploadFileResponse(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "data") val data: AllDebridMagnetUploadFileData?,
    @param:Json(name = "error") val error: AllDebridError?,
)

@JsonClass(generateAdapter = true)
data class AllDebridMagnetUploadFileData(
    @param:Json(name = "files") val files: List<AllDebridUploadedMagnet>?,
)

@JsonClass(generateAdapter = true)
data class AllDebridMagnetStatusResponse(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "data") val data: AllDebridMagnetStatusData?,
    @param:Json(name = "error") val error: AllDebridError?,
)

@JsonClass(generateAdapter = true)
data class AllDebridMagnetStatusData(
    @param:Json(name = "magnets") val magnets: List<AllDebridMagnet>?,
)

@JsonClass(generateAdapter = true)
data class AllDebridMagnet(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "filename") val filename: String,
    @param:Json(name = "size") val size: Long,
    @param:Json(name = "status") val status: String,
    @param:Json(name = "statusCode") val statusCode: Int,
    @param:Json(name = "downloaded") val downloaded: Long? = null,
    @param:Json(name = "seeders") val seeders: Int? = null,
    @param:Json(name = "downloadSpeed") val downloadSpeed: Int? = null,
    @param:Json(name = "uploadDate") val uploadDate: Long? = null,
    @param:Json(name = "completionDate") val completionDate: Long? = null,
)

@JsonClass(generateAdapter = true)
data class AllDebridMagnetFilesResponse(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "data") val data: AllDebridMagnetFilesData?,
    @param:Json(name = "error") val error: AllDebridError?,
)

@JsonClass(generateAdapter = true)
data class AllDebridMagnetFilesData(
    @param:Json(name = "magnets") val magnets: List<AllDebridMagnetFilesEntry>?,
)

@JsonClass(generateAdapter = true)
data class AllDebridMagnetFilesEntry(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "files") val files: List<AllDebridMagnetFileNode>?,
    @param:Json(name = "error") val error: AllDebridError?,
)

@JsonClass(generateAdapter = true)
data class AllDebridMagnetFileNode(
    @param:Json(name = "n") val name: String,
    @param:Json(name = "s") val size: Long? = null,
    @param:Json(name = "l") val link: String? = null,
    @param:Json(name = "e") val entries: List<AllDebridMagnetFileNode>? = null,
)

@JsonClass(generateAdapter = true)
data class AllDebridMessageResponse(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "data") val data: AllDebridMessageData?,
    @param:Json(name = "error") val error: AllDebridError?,
)

@JsonClass(generateAdapter = true)
data class AllDebridMessageData(@param:Json(name = "message") val message: String?)

@JsonClass(generateAdapter = true)
data class AllDebridPinCheckResult(
    val activated: Boolean,
    val expiresIn: Int,
    val apiKey: String?,
)

@JsonClass(generateAdapter = true)
data class AllDebridUserHistoryResponse(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "data") val data: AllDebridUserHistoryData?,
    @param:Json(name = "error") val error: AllDebridError?,
)

@JsonClass(generateAdapter = true)
data class AllDebridUserHistoryData(
    @param:Json(name = "links") val links: List<AllDebridUserLink>?,
)

@JsonClass(generateAdapter = true)
data class AllDebridUserLink(
    @param:Json(name = "link") val link: String,
    @param:Json(name = "filename") val filename: String,
    @param:Json(name = "size") val size: Long,
    @param:Json(name = "date") val date: Long,
    @param:Json(name = "host") val host: String,
)

@JsonClass(generateAdapter = true)
data class AllDebridRedirectorResponse(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "data") val data: AllDebridRedirectorData?,
    @param:Json(name = "error") val error: AllDebridError?,
)

@JsonClass(generateAdapter = true)
data class AllDebridRedirectorData(
    @param:Json(name = "links") val links: List<AllDebridRedirectorLink>?,
)

@JsonClass(generateAdapter = true)
data class AllDebridRedirectorLink(
    @param:Json(name = "link") val link: String?,
    @param:Json(name = "host") val host: String?,
    @param:Json(name = "filename") val filename: String?,
    @param:Json(name = "filesize") val filesize: Long?,
)

private val apiErrorAdapter = Moshi.Builder().build().adapter(APIError::class.java)

fun mapAllDebridErrorCode(code: String?): Int =
    when (code) {
        "AUTH_MISSING_APIKEY",
        "AUTH_BAD_APIKEY" -> 8
        "AUTH_BLOCKED" -> 22
        "MUST_BE_PREMIUM",
        "MAGNET_MUST_BE_PREMIUM" -> 20
        "LINK_HOST_NOT_SUPPORTED",
        "LINK_NOT_SUPPORTED" -> 16
        "LINK_HOST_UNAVAILABLE" -> 17
        "LINK_HOST_LIMIT_REACHED" -> 18
        "LINK_TEMPORARY_UNAVAILABLE" -> 19
        "LINK_TOO_MANY_DOWNLOADS",
        "MAGNET_TOO_MANY_ACTIVE" -> 21
        "LINK_DOWN" -> 24
        "MAINTENANCE" -> 25
        "MAGNET_TOO_LARGE" -> 29
        "MAGNET_INVALID_FILE" -> 30
        "LINK_HOST_FULL",
        "PIN_EXPIRED",
        "PIN_INVALID",
        "FREE_TRIAL_LIMIT_REACHED" -> 34
        else -> -1
    }

fun <T> allDebridErrorResponse(
    error: AllDebridError?,
    code: Int = 400,
): Response<T> {
    val body =
        apiErrorAdapter
            .toJson(
                APIError(
                    error = error?.message ?: "Unknown AllDebrid error",
                    errorDetails = error?.code,
                    errorCode = mapAllDebridErrorCode(error?.code),
                )
            )
            .toResponseBody("application/json".toMediaType())
    return Response.error(code, body)
}

fun AllDebridPinGetData.toAuthentication(): Authentication =
    Authentication(
        deviceCode = check,
        userCode = pin,
        interval = 5,
        expiresIn = expiresIn,
        verificationUrl = userUrl,
        directVerificationUrl = userUrl,
    )

fun AllDebridPinCheckData.toResult(): AllDebridPinCheckResult =
    AllDebridPinCheckResult(activated = activated, expiresIn = expiresIn, apiKey = apiKey)

fun AllDebridUserInfo.toUser(nowEpochSeconds: Long): User {
    val premiumUntilEpoch = premiumUntil.toLongOrNull() ?: 0L
    val premiumSeconds =
        if (premiumUntilEpoch > nowEpochSeconds) {
            (premiumUntilEpoch - nowEpochSeconds).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        } else {
            0
        }
    return User(
        id = 0,
        username = username,
        email = email,
        points = fidelityPoints,
        locale = lang,
        avatar = "",
        type = if (isSubscribed) "premium" else "free",
        premium = premiumSeconds,
        expiration = premiumUntil,
    )
}

fun AllDebridLinkUnlockData.toDownloadItem(sourceLink: String): DownloadItem =
    DownloadItem(
        id = id ?: download ?: sourceLink,
        filename = filename,
        mimeType = null,
        fileSize = filesize,
        link = sourceLink,
        host = hostDomain ?: host,
        hostIcon = null,
        chunks = 1,
        crc = null,
        download = download ?: sourceLink,
        streamable = 0,
        generated = null,
        type = null,
        alternative = null,
    )

fun AllDebridHostsData.toRegexList(): List<String> =
    listOfNotNull(hosts, streams, redirectors)
        .flatMap { it.values }
        .flatMap { service -> service.regexps ?: listOfNotNull(service.regexp) }
        .distinct()

fun placeholderAllDebridHosts(): List<AvailableHost> =
    listOf(AvailableHost(host = "all", maxFileSize = Int.MAX_VALUE))

fun AllDebridUploadedMagnet.toUploadedTorrent(): UploadedTorrent? =
    id?.let { UploadedTorrent(id = it.toString(), uri = magnet ?: file ?: name ?: it.toString()) }

fun AllDebridMagnet.toTorrentItem(filesEntry: AllDebridMagnetFilesEntry? = null): TorrentItem {
    val flattenedFiles = filesEntry?.files?.flattenMagnetNodes().orEmpty()
    val links = flattenedFiles.mapNotNull { it.second.link }.distinct()
    val progress =
        when {
            size <= 0L -> if (statusCode == 4) 100f else 0f
            else -> (((downloaded ?: 0L).toDouble() / size.toDouble()) * 100.0).toFloat()
        }
    return TorrentItem(
        id = id.toString(),
        filename = filename,
        originalFilename = filename,
        hash = id.toString(),
        bytes = downloaded ?: size,
        originalBytes = size,
        host = "alldebrid.com",
        split = 0,
        progress = progress.coerceIn(0f, 100f),
        status = mapAllDebridMagnetStatus(statusCode),
        added = uploadDate?.toString().orEmpty(),
        files =
            flattenedFiles.mapIndexed { index, pair ->
                InnerTorrentFile(
                    id = index + 1,
                    path = pair.first,
                    bytes = pair.second.size ?: 0L,
                    selected = 1,
                )
            },
        links = links,
        ended = completionDate?.takeIf { it > 0 }?.toString(),
        speed = downloadSpeed,
        seeders = seeders,
    )
}

private fun mapAllDebridMagnetStatus(statusCode: Int): String =
    when (statusCode) {
        0 -> "queued"
        1 -> "downloading"
        2 -> "compressing"
        3 -> "uploading"
        4 -> "ready"
        15 -> "dead"
        else -> "error"
    }

private fun List<AllDebridMagnetFileNode>.flattenMagnetNodes(
    currentPath: String = "",
): List<Pair<String, AllDebridMagnetFileNode>> =
    flatMap { node ->
        val nextPath = if (currentPath.isBlank()) "/${node.name}" else "$currentPath/${node.name}"
        if (!node.entries.isNullOrEmpty()) node.entries.flattenMagnetNodes(nextPath)
        else listOf(Pair(nextPath, node))
    }

fun allDebridSecretsPlaceholder(): Secrets =
    Secrets(clientId = "alldebrid", clientSecret = "alldebrid")

// AllDebrid API keys do not expire; use Int.MAX_VALUE so the token-refresh flow is never
// triggered.  The key is stored with refreshToken == PRIVATE_TOKEN, so isTokenPrivate()
// returns true and the app treats it as a permanent credential.
fun allDebridTokenPlaceholder(apiKey: String): Token =
    Token(
        accessToken = apiKey,
        expiresIn = Int.MAX_VALUE,
        tokenType = "Bearer",
        refreshToken = apiKey,
    )

fun AllDebridUserLink.toDownloadItem(): DownloadItem =
    DownloadItem(
        id = link,
        filename = filename,
        mimeType = null,
        fileSize = size,
        link = link,
        host = host,
        hostIcon = null,
        chunks = 1,
        crc = null,
        download = link,
        streamable = 0,
        generated = date.toString(),
        type = null,
        alternative = null,
    )

fun AllDebridRedirectorData.toLinkList(): List<String> =
    links?.mapNotNull { it.link } ?: emptyList()
