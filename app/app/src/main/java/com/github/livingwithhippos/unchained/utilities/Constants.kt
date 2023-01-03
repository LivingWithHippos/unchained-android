package com.github.livingwithhippos.unchained.utilities

import com.github.livingwithhippos.unchained.R

const val OPEN_SOURCE_CLIENT_ID = "X245A4XAIBGVM"

const val OPEN_SOURCE_GRANT_TYPE = "http://oauth.net/grant_type/device/1.0"

const val BASE_URL = "https://api.real-debrid.com/rest/1.0/"
const val BASE_AUTH_URL = "https://api.real-debrid.com/oauth/v2/"
const val INSTANT_AVAILABILITY_ENDPOINT = "torrents/instantAvailability"
const val REFERRAL_LINK = "http://real-debrid.com/?id=78841"
const val ACCOUNT_LINK = "https://real-debrid.com/account"
const val PLUGINS_PACK_FOLDER = "pack"
const val DEFAULT_PLUGINS_REPOSITORY_LINK =
    "https://gitlab.com/LivingWithHippos/unchained-plugins/-/raw/main/repository/repository.json"
/**
 * Folder name for the plugins installed manually, not from a web repository
 */
const val MANUAL_PLUGINS_REPOSITORY_NAME = "common_repository"

// unofficial link to get streaming from a browser page
const val RD_STREAMING_URL = "https://real-debrid.com/streaming-"

const val PRIVATE_TOKEN: String = "private_token"

const val REMOTE_TRAFFIC_OFF: Int = 0
const val REMOTE_TRAFFIC_ON: Int = 1

const val MAGNET_PATTERN: String = "magnet:\\?xt=urn:btih:([a-zA-Z0-9]{32,})"
const val TORRENT_PATTERN: String = "https?://[^\\s]{7,}\\.torrent"
const val CONTAINER_PATTERN: String = "https?://[^\\s]{7,}\\.(rsdf|ccf3|ccf|dlc)"
const val CONTAINER_EXTENSION_PATTERN: String = "[^\\s]+\\.(rsdf|ccf3|ccf|dlc)$"

const val FEEDBACK_URL = "https://github.com/LivingWithHippos/unchained-android"
const val GPLV3_URL = "https://www.gnu.org/licenses/gpl-3.0.en.html"

const val SCHEME_MAGNET = "magnet"
const val SCHEME_HTTP = "http"
const val SCHEME_HTTPS = "https"

val errorMap = mapOf(
    -1 to "Internal error",
    1 to "Missing parameter",
    2 to "Bad parameter value",
    3 to "Unknown method",
    4 to "Method not allowed",
    5 to "Slow down",
    6 to "Resource unreachable",
    7 to "Resource not found",
    8 to "Bad token",
    9 to "Permission denied",
    10 to "Two-Factor authentication needed",
    11 to "Two-Factor authentication pending",
    12 to "Invalid login",
    13 to "Invalid password",
    14 to "Account locked",
    15 to "Account not activated",
    16 to "Unsupported hoster",
    17 to "Hoster in maintenance",
    18 to "Hoster limit reached",
    19 to "Hoster temporarily unavailable",
    20 to "Hoster not available for free users",
    21 to "Too many active downloads",
    22 to "IP Address not allowed",
    23 to "Traffic exhausted",
    24 to "File unavailable",
    25 to "Service unavailable",
    26 to "Upload too big",
    27 to "Upload error",
    28 to "File not allowed",
    29 to "Torrent too big",
    30 to "Torrent file invalid",
    31 to "Action already done",
    32 to "Image resolution error",
    33 to "Torrent already active"
)

// Torrent status list
// possible status are magnet_error, magnet_conversion, waiting_files_selection,
// queued, downloading, downloaded, error, virus, compressing, uploading, dead

/**
 * Statuses the torrent is not going to advance from
 */
val endedStatusList = listOf("magnet_error", "downloaded", "error", "virus", "dead")

/**
 * Statuses the torrent will advance from
 */
val loadingStatusList = listOf(
    "downloading",
    "magnet_conversion",
    "waiting_files_selection",
    "queued",
    "compressing",
    "uploading"
)

/**
 * Statuses where the torrent hasn't had its file selected yet
 */
val beforeSelectionStatusList = listOf(
    "magnet_conversion",
    "waiting_files_selection",
)

const val DOWNLOADS_TAB = 0
const val TORRENTS_TAB = 1

object SIGNATURE {
    const val URL =
        "https://gist.githubusercontent.com/LivingWithHippos/5525e73f0439d06c1c3ff4f9484e35dd/raw/f97b79e706aa67d729806039d49f80aba4042793/unchained_versions.json"
    const val PLAY_STORE = "31F17448AA3888B63ED04EB5F965E3F70C12592F"
    const val F_DROID = "412DABCABBDB75A82FF66F767C71EE045C02275B"
    const val GITHUB = "0E7BE3FA6B47C20394517C568570E10761A0A4FA"
}

object APP_LINK {
    const val PLAY_STORE =
        "https://play.google.com/store/apps/details?id=com.github.livingwithhippos.unchained"
    const val F_DROID = "https://f-droid.org/packages/com.github.livingwithhippos.unchained/"
    const val GITHUB = "https://github.com/LivingWithHippos/unchained-android/releases"
}

object PreferenceKeys {
    // todo: move all keys here
    object DownloadManager {
        const val KEY = "download_manager"
        const val SYSTEM = "download_manager_system"
        const val OKHTTP = "download_manager_okhttp"
        const val UNMETERED_ONLY_KEY = "download_only_on_unmetered"
    }
}

/**
 * Used to map file extension and their icon
 */
val extensionIconMap: Map<String, Int> = mapOf(
    // this will be used as default value if no extension is recognized
    "default" to R.drawable.icon_file,
    // ARCHIVES
    "zip" to R.drawable.icon_archive,
    "rar" to R.drawable.icon_archive,
    "7z" to R.drawable.icon_archive,
    "tar" to R.drawable.icon_archive,
    "gz" to R.drawable.icon_archive,
    "arj" to R.drawable.icon_archive,
    "deb" to R.drawable.icon_archive,
    "pkg" to R.drawable.icon_archive,
    "rpm" to R.drawable.icon_archive,
    // AUDIO
    "aif" to R.drawable.icon_audio,
    "cda" to R.drawable.icon_audio,
    "mid" to R.drawable.icon_audio,
    "midi" to R.drawable.icon_audio,
    "mp3" to R.drawable.icon_audio,
    "mpa" to R.drawable.icon_audio,
    "ogg" to R.drawable.icon_audio,
    "wav" to R.drawable.icon_audio,
    "wma" to R.drawable.icon_audio,
    "wpl" to R.drawable.icon_audio,
    // PICTURES
    "ai" to R.drawable.icon_image,
    "bmp" to R.drawable.icon_image,
    "gif" to R.drawable.icon_image,
    "ico" to R.drawable.icon_image,
    "jpeg" to R.drawable.icon_image,
    "jpg" to R.drawable.icon_image,
    "png" to R.drawable.icon_image,
    "psd" to R.drawable.icon_image,
    "ps" to R.drawable.icon_image,
    "svg" to R.drawable.icon_image,
    "tiff" to R.drawable.icon_image,
    "tif" to R.drawable.icon_image,
    "raw" to R.drawable.icon_image,
    // VIDEOS
    "3g2" to R.drawable.icon_movie,
    "3gp" to R.drawable.icon_movie,
    "avi" to R.drawable.icon_movie,
    "flv" to R.drawable.icon_movie,
    "h264" to R.drawable.icon_movie,
    "m4v" to R.drawable.icon_movie,
    "mkv" to R.drawable.icon_movie,
    "mov" to R.drawable.icon_movie,
    "mp4" to R.drawable.icon_movie,
    "mpg" to R.drawable.icon_movie,
    "mpeg" to R.drawable.icon_movie,
    "rm" to R.drawable.icon_movie,
    "swf" to R.drawable.icon_movie,
    "vob" to R.drawable.icon_movie,
    "wmv" to R.drawable.icon_movie,
    // CAPTIONS
    "srt" to R.drawable.icon_subtitles,
    "scc" to R.drawable.icon_subtitles,
    "vtt" to R.drawable.icon_subtitles,
    "itt" to R.drawable.icon_subtitles,
    "smi" to R.drawable.icon_subtitles,
    "sami" to R.drawable.icon_subtitles,
    "sbv" to R.drawable.icon_subtitles,
    "aaf" to R.drawable.icon_subtitles,
    "mcc" to R.drawable.icon_subtitles,
    "mxf" to R.drawable.icon_subtitles,
    "asc" to R.drawable.icon_subtitles,
    "stl" to R.drawable.icon_subtitles,
    "scr" to R.drawable.icon_subtitles,
    "sub" to R.drawable.icon_subtitles,
    "idx" to R.drawable.icon_subtitles,
)
