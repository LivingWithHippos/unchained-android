package com.github.livingwithhippos.unchained.start.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.AuthenticationState
import com.github.livingwithhippos.unchained.data.model.Credentials
import com.github.livingwithhippos.unchained.data.model.User
import com.github.livingwithhippos.unchained.data.repositoy.AuthenticationRepository
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.HostsRepository
import com.github.livingwithhippos.unchained.data.repositoy.PluginRepository
import com.github.livingwithhippos.unchained.data.repositoy.PluginRepository.Companion.TYPE_UNCHAINED
import com.github.livingwithhippos.unchained.data.repositoy.UserRepository
import com.github.livingwithhippos.unchained.data.repositoy.VariousApiRepository
import com.github.livingwithhippos.unchained.lists.view.ListsTabFragment
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.PRIVATE_TOKEN
import com.github.livingwithhippos.unchained.utilities.extension.getDownloadedFileUri
import com.github.livingwithhippos.unchained.utilities.extension.isMagnet
import com.github.livingwithhippos.unchained.utilities.extension.isTorrent
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * a [ViewModel] subclass.
 * Shared between the fragments to observe the authentication status and update it.
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthenticationRepository,
    private val credentialRepository: CredentialsRepository,
    private val userRepository: UserRepository,
    private val variousApiRepository: VariousApiRepository,
    private val hostsRepository: HostsRepository,
    private val pluginRepository: PluginRepository,
) : ViewModel() {

    val authenticationState = MutableLiveData<Event<AuthenticationState>>()

    val userLiveData = MutableLiveData<User?>()

    val externalLinkLiveData = MutableLiveData<Event<Uri>>()

    val downloadedFileLiveData = MutableLiveData<Event<Long>>()

    val notificationTorrentLiveData = MutableLiveData<Event<String>>()

    val listStateLiveData = MutableLiveData<Event<ListsTabFragment.ListState>>()

    val connectivityLiveData = MutableLiveData<Boolean>()
    // val currentNetworkLiveData = MutableLiveData<Network?>()

    // todo: use a better name to reflect the difference between this and externalLinkLiveData
    val linkLiveData = MutableLiveData<Event<String>>()

    val messageLiveData = MutableLiveData<Event<Int>>()

    private var refreshJob: Job? = null

    fun fetchFirstWorkingCredentials() {
        viewModelScope.launch {

            var user: User? = null

            val completeCredentials = credentialRepository
                .getAllCredentials()
                .filter { it.accessToken != null && it.clientId != null && it.clientSecret != null && it.deviceCode.isNotBlank() && it.refreshToken != null }

            if (completeCredentials.isNotEmpty()) {

                // step #1: test for private API token
                completeCredentials.firstOrNull { it.deviceCode == PRIVATE_TOKEN }?.let {
                    user = checkCredentials(it)
                }
                // step #2: test for open source credentials
                if (user == null) {
                    completeCredentials.firstOrNull { it.deviceCode != PRIVATE_TOKEN }?.let {
                        authRepository.refreshToken(it)?.let { token ->
                            val newCredentials = Credentials(
                                it.deviceCode,
                                it.clientId,
                                it.clientSecret,
                                token.accessToken,
                                token.refreshToken
                            )

                            user = userRepository.getUserInfo(token.accessToken)
                            if (user != null) {
                                // update the credentials
                                credentialRepository.updateCredentials(newCredentials)
                                // program the refresh of the token
                                programTokenRefresh(token.expiresIn)
                            }
                        }
                    }
                }
            }

            // pass whatever user was retrieved, or null if none was found
            userLiveData.postValue(user)
        }
    }

    private suspend fun checkCredentials(credentials: Credentials): User? {
        if (credentials.accessToken != null) {
            return userRepository.getUserInfo(credentials.accessToken)
        } else
            throw IllegalArgumentException("Credentials parameter has null access token")
        // todo: needs to check if it's a network error or if token has expired etc.
    }

    fun setAuthenticated() {
        authenticationState.postEvent(AuthenticationState.AUTHENTICATED)
    }

    fun setAuthenticatedNoPremium() {
        authenticationState.postEvent(AuthenticationState.AUTHENTICATED_NO_PREMIUM)
    }

    fun setUnauthenticated() {
        authenticationState.postEvent(AuthenticationState.UNAUTHENTICATED)
    }

    fun setBadToken() {
        authenticationState.postEvent(AuthenticationState.BAD_TOKEN)
    }

    fun logout() {
        viewModelScope.launch {
            credentialRepository.deleteAllCredentials()
            setUnauthenticated()
        }
    }

    fun invalidateOpenSourceToken() {
        viewModelScope.launch {
            credentialRepository.getFirstCredentials()?.let {
                if (it.refreshToken != null && it.refreshToken != PRIVATE_TOKEN) {
                    // setUnauthenticated()
                    variousApiRepository.disableToken(it.accessToken!!)
                }
            }
        }
    }

    suspend fun isTokenPrivate(): Boolean {
        val credentials = credentialRepository.getFirstCredentials()
        return credentials?.refreshToken == PRIVATE_TOKEN
    }

    suspend fun deleteIncompleteCredentials() = credentialRepository.deleteIncompleteCredentials()

    fun refreshToken() {

        viewModelScope.launch {

            // get the old credentials
            val oldCredentials = credentialRepository.getFirstCredentials()
            // check if they are private API token credentials
            if (oldCredentials != null && oldCredentials.refreshToken != PRIVATE_TOKEN) {
                // refresh the token
                authRepository.refreshToken(oldCredentials)?.let {
                    val newCredentials = Credentials(
                        oldCredentials.deviceCode,
                        oldCredentials.clientId,
                        oldCredentials.clientSecret,
                        it.accessToken,
                        it.refreshToken
                    )
                    // update the credentials
                    credentialRepository.updateCredentials(newCredentials)

                    // program the refresh of the token
                    programTokenRefresh(it.expiresIn)
                }
            }
        }
    }

    fun addLink(uri: Uri) {
        externalLinkLiveData.postEvent(uri)
    }

    fun setDownload(downloadID: Long) {
        savedStateHandle.set(KEY_TORRENT_DOWNLOAD_ID, downloadID)
    }

    fun setPluginDownload(downloadID: Long) {
        savedStateHandle.set(KEY_PLUGIN_DOWNLOAD_ID, downloadID)
    }

    fun checkTorrentDownload(downloadID: Long) {
        val torrentID = savedStateHandle.get<Long>(KEY_TORRENT_DOWNLOAD_ID)
        if (torrentID == downloadID)
            downloadedFileLiveData.postEvent(torrentID)
    }

    fun checkPluginDownload(context: Context, downloadID: Long) {
        val pluginID = savedStateHandle.get<Long>(KEY_PLUGIN_DOWNLOAD_ID)
        if (pluginID == downloadID) {

            val uri = context.getDownloadedFileUri(pluginID)
            // no need to recheck the extension since it was checked on download
            if (uri?.path != null) {
                viewModelScope.launch {

                    val pluginFile = File(uri.path!!)

                    var installed = false

                    if (pluginFile.exists()) {
                        pluginFile.bufferedReader().use { reader ->
                            try {
                                val source = reader.readText()
                                installed = pluginRepository.addExternalPlugin(context, source)
                            } catch (ex: Exception) {
                                Timber.e("Error reading file in path ${uri.path}, exception ${ex.message}")
                            }
                        }
                    }

                    if (installed)
                        messageLiveData.postEvent(R.string.plugin_install_installed)
                    else
                        messageLiveData.postEvent(R.string.plugin_install_not_installed)
                }
            }
        }
    }

    // todo: move this stuff to a shared navigationViewModel
    fun setListState(state: ListsTabFragment.ListState) {
        listStateLiveData.postEvent(state)
    }

    fun getLastBackPress(): Long {
        return savedStateHandle.get<Long>(KEY_LAST_BACK_PRESS) ?: 0
    }

    fun setLastBackPress(time: Long) {
        savedStateHandle.set(KEY_LAST_BACK_PRESS, time)
    }

    private fun programTokenRefresh(secondsDelay: Int) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            // secondsDelay*950L -> expiration time - 5%
            delay(secondsDelay * 950L)
            refreshToken()
        }
    }

    fun downloadSupportedLink(link: String) {
        viewModelScope.launch {
            when {
                link.isMagnet() -> linkLiveData.postEvent(link)
                link.isTorrent() -> linkLiveData.postEvent(link)
                link.endsWith(TYPE_UNCHAINED, ignoreCase = true) -> {
                    // only accept github links for now
                    val newLink = convertGithubToRaw(link)
                    if (newLink != null)
                        linkLiveData.postEvent(newLink)
                    else
                        messageLiveData.postEvent(R.string.invalid_url)
                }
                else -> {
                    var matchFound = false
                    // check the hosts regexs
                    for (hostRegex in hostsRepository.getHostsRegex()) {
                        val m: Matcher = Pattern.compile(hostRegex.regex).matcher(link)
                        if (m.matches()) {
                            matchFound = true
                            linkLiveData.postEvent(link)
                            break
                        }
                    }
                    // check the folders regexs
                    if (!matchFound) {
                        for (hostRegex in hostsRepository.getFoldersRegex()) {
                            val m: Matcher = Pattern.compile(hostRegex.regex).matcher(link)
                            if (m.matches()) {
                                matchFound = true
                                linkLiveData.postEvent(link)
                                break
                            }
                        }
                    }
                    if (!matchFound)
                        messageLiveData.postEvent(R.string.host_match_not_found)
                }
            }
        }
    }

    private fun convertGithubToRaw(github: String): String? {
        val username = "([^/]+)"
        val repo = "([^/]+)"
        val type = "(tree|blob)"
        val branch = "([^/]+)"
        val path = "(.+)"
        when {
            github.startsWith("https://www.github.com") || github.startsWith("https://github.com") -> {
                val regex =
                    "https?://(www.)?github.com/$username/$repo/$type/$branch/$path".toRegex()
                val match: MatchResult = regex.find(github) ?: return null
                return "https://raw.githubusercontent.com/" + match.groupValues[2] + "/" + match.groupValues[3] + "/" + match.groupValues[5] + "/" + match.groupValues[6]
            }
            github.startsWith("https://raw.githubusercontent.com") -> {
                return github
            }
            else -> return null
        }
    }

    fun addTorrentId(torrentID: String) {
        notificationTorrentLiveData.postEvent(torrentID)
    }

    fun setTokenRefreshing(refreshing: Boolean) {
        savedStateHandle.set(KEY_REFRESHING_TOKEN, refreshing)
    }

    fun isTokenRefreshing(): Boolean {
        return savedStateHandle.get<Boolean>(KEY_REFRESHING_TOKEN) ?: false
    }

    fun addPlugin(context: Context, data: Uri) {
        // check if the plugin is already installed/a newer version.
        viewModelScope.launch {
            val filename = data.path?.split("/")?.last()
            val tempPlugin = pluginRepository.readPassedPlugin(context, data)
            if (filename != null && tempPlugin != null) {
                val file = File(context.filesDir, filename)
                // check if we' re overwriting a plugin
                if (file.exists()) {
                    val existingPlugin: Plugin? =
                        pluginRepository.getExternalPlugin(context, filename)
                    // corrupted file probably, remove and write the new one
                    if (existingPlugin == null) {
                        file.delete()
                        val installed = pluginRepository.addExternalPlugin(context, data)
                        if (installed)
                            messageLiveData.postEvent(R.string.plugin_install_installed)
                        else
                            messageLiveData.postEvent(R.string.plugin_install_not_installed)
                    } else {
                        // is it the same plugin?
                        if (existingPlugin.name == tempPlugin.name) {
                            // is the version newer?
                            if (existingPlugin.version < tempPlugin.version) {
                                file.delete()
                                val installed = pluginRepository.addExternalPlugin(context, data)
                                if (installed)
                                    messageLiveData.postEvent(R.string.plugin_install_installed)
                                else
                                    messageLiveData.postEvent(R.string.plugin_install_not_installed)
                            } else {
                                // installed plugin is newer
                                messageLiveData.postEvent(R.string.plugin_install_error_newer)
                            }
                        } else {
                            // same file name for different plugins
                            val installed = pluginRepository.addExternalPlugin(
                                context, data,
                                "_$filename"
                            )
                            if (installed)
                                messageLiveData.postEvent(R.string.plugin_install_installed)
                            else
                                messageLiveData.postEvent(R.string.plugin_install_not_installed)
                        }
                    }
                } else {
                    val installed = pluginRepository.addExternalPlugin(context, data)
                    if (installed)
                        messageLiveData.postEvent(R.string.plugin_install_installed)
                    else
                        messageLiveData.postEvent(R.string.plugin_install_not_installed)
                }
            } else {
                messageLiveData.postEvent(R.string.plugin_install_not_installed)
            }
        }
    }

    private val networkCallback: ConnectivityManager.NetworkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // currentNetworkLiveData.postValue(network)
                connectivityLiveData.postValue(true)
            }

            override fun onLost(network: Network) {
                Timber.e(
                    "The application no longer has a default network. The last default network was %s",
                    network
                )
                // currentNetworkLiveData.postValue(null)
                connectivityLiveData.postValue(false)
            }
        }

    fun setupConnectivityCheck(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            checkConnectivity(context)
        }
    }

    fun checkConnectivity(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            var isConnected = false
            val networks = connectivityManager.allNetworks
            for (net in networks) {
                val netInfo = connectivityManager.getNetworkCapabilities(net)
                if (netInfo != null && netInfo.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    isConnected = true
                    break
                }
            }
            connectivityLiveData.postValue(isConnected)
        }
    }

    companion object {
        const val KEY_TORRENT_DOWNLOAD_ID = "torrent_download_id_key"
        const val KEY_PLUGIN_DOWNLOAD_ID = "plugin_download_id_key"
        const val KEY_LAST_BACK_PRESS = "last_back_press_key"
        const val KEY_REFRESHING_TOKEN = "refreshing_token_key"
    }
}
