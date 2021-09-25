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
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.*
import com.github.livingwithhippos.unchained.data.repositoy.*
import com.github.livingwithhippos.unchained.data.repositoy.PluginRepository.Companion.TYPE_UNCHAINED
import com.github.livingwithhippos.unchained.lists.view.ListsTabFragment
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationEvent
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationSideEffect
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationState
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.PRIVATE_TOKEN
import com.github.livingwithhippos.unchained.utilities.extension.getDownloadedFileUri
import com.github.livingwithhippos.unchained.utilities.extension.isMagnet
import com.github.livingwithhippos.unchained.utilities.extension.isTorrent
import com.github.livingwithhippos.unchained.utilities.postEvent
import com.tinder.StateMachine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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
    private val userRepository: UserRepository,
    private val variousApiRepository: VariousApiRepository,
    private val hostsRepository: HostsRepository,
    private val pluginRepository: PluginRepository,
    private val credentialsRepository: CredentialsRepository,
    private val protoStore: ProtoStore,
) : ViewModel() {

    val newAuthenticationState = MutableLiveData<Event<AuthenticationStatus>>()
    val fsmAuthenticationState = MutableLiveData<Event<FSMAuthenticationState>>()

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

    private val authStateMachine: StateMachine<
            FSMAuthenticationState,
            FSMAuthenticationEvent,
            FSMAuthenticationSideEffect
            > = StateMachine.create {

        initialState(FSMAuthenticationState.Start)

        state<FSMAuthenticationState.Start> {
            on<FSMAuthenticationEvent.OnAvailableCredentials> {
                transitionTo(
                    FSMAuthenticationState.CheckCredentials,
                    FSMAuthenticationSideEffect.CheckingCredentials
                )
            }
            on<FSMAuthenticationEvent.OnMissingCredentials> {
                transitionTo(
                    FSMAuthenticationState.StartNewLogin,
                    FSMAuthenticationSideEffect.PostNewLogin
                )
            }
        }

        state<FSMAuthenticationState.CheckCredentials> {
            on<FSMAuthenticationEvent.OnWorkingOpenToken> {
                transitionTo(
                    FSMAuthenticationState.AuthenticatedOpenToken,
                    FSMAuthenticationSideEffect.PostAuthenticatedPrivate
                )
            }
            on<FSMAuthenticationEvent.OnExpiredOpenToken> {
                transitionTo(
                    FSMAuthenticationState.RefreshingOpenToken,
                    FSMAuthenticationSideEffect.PostRefreshingToken
                )
            }
            on<FSMAuthenticationEvent.OnWorkingPrivateToken> {
                transitionTo(
                    FSMAuthenticationState.AuthenticatedPrivateToken,
                    FSMAuthenticationSideEffect.PostAuthenticatedOpen
                )
            }
            on<FSMAuthenticationEvent.OnNotWorking> {
                transitionTo(
                    FSMAuthenticationState.StartNewLogin,
                    FSMAuthenticationSideEffect.PostNewLogin
                )
            }
            on<FSMAuthenticationEvent.OnUserActionNeeded> {
                transitionTo(
                    FSMAuthenticationState.WaitingUserAction(null),
                    FSMAuthenticationSideEffect.PostActionNeeded
                )
            }
        }

        state<FSMAuthenticationState.WaitingUserAction> {
            on<FSMAuthenticationEvent.OnUserActionRetry> {
                transitionTo(
                    FSMAuthenticationState.CheckCredentials,
                    FSMAuthenticationSideEffect.CheckingCredentials
                )
            }
            on<FSMAuthenticationEvent.OnUserActionReset> {
                transitionTo(
                    FSMAuthenticationState.StartNewLogin,
                    FSMAuthenticationSideEffect.ResetAuthentication
                )
            }
        }

        state<FSMAuthenticationState.StartNewLogin> {
            on<FSMAuthenticationEvent.OnAuthLoaded> {
                transitionTo(
                    FSMAuthenticationState.WaitingUserConfirmation,
                    FSMAuthenticationSideEffect.PostWaitUserConfirmation
                )
            }
            // I can get a private token on this state too
            on<FSMAuthenticationEvent.OnPrivateToken> {
                transitionTo(
                    FSMAuthenticationState.CheckCredentials,
                    FSMAuthenticationSideEffect.CheckingCredentials
                )
            }
        }

        state<FSMAuthenticationState.WaitingUserConfirmation> {
            on<FSMAuthenticationEvent.OnUserConfirmationLoaded> {
                transitionTo(
                    FSMAuthenticationState.WaitingToken,
                    FSMAuthenticationSideEffect.PostWaitToken
                )
            }
            on<FSMAuthenticationEvent.OnUserConfirmationMissing> {
                transitionTo(
                    FSMAuthenticationState.WaitingUserConfirmation,
                    FSMAuthenticationSideEffect.PostWaitUserConfirmation
                )
            }
            // I can get a private token on this state too
            on<FSMAuthenticationEvent.OnPrivateToken> {
                transitionTo(
                    FSMAuthenticationState.CheckCredentials,
                    FSMAuthenticationSideEffect.CheckingCredentials
                )
            }
        }

        state<FSMAuthenticationState.WaitingToken> {
            on<FSMAuthenticationEvent.OnOpenTokenLoaded> {
                transitionTo(
                    FSMAuthenticationState.CheckCredentials,
                    FSMAuthenticationSideEffect.CheckingCredentials
                )
            }
            // I can get a private token on this state too
            on<FSMAuthenticationEvent.OnPrivateToken> {
                transitionTo(
                    FSMAuthenticationState.CheckCredentials,
                    FSMAuthenticationSideEffect.CheckingCredentials
                )
            }
        }

        state<FSMAuthenticationState.AuthenticatedOpenToken> {
            on<FSMAuthenticationEvent.OnExpiredOpenToken> {
                transitionTo(
                    FSMAuthenticationState.RefreshingOpenToken,
                    FSMAuthenticationSideEffect.PostRefreshingToken
                )
            }
        }

        state<FSMAuthenticationState.RefreshingOpenToken> {
            on<FSMAuthenticationEvent.OnRefreshed> {
                transitionTo(
                    FSMAuthenticationState.AuthenticatedOpenToken,
                    FSMAuthenticationSideEffect.PostAuthenticatedOpen
                )
            }
        }

        state<FSMAuthenticationState.AuthenticatedPrivateToken> {
        }

        onTransition {
            val validTransition = it as? StateMachine.Transition.Valid ?: return@onTransition
            Timber.d("statemachine", validTransition)
            when (validTransition.sideEffect) {
                null -> {
                    // do nothing
                }
                is FSMAuthenticationSideEffect.CheckingCredentials -> {
                    fsmAuthenticationState.postEvent(FSMAuthenticationState.CheckCredentials)
                }
                FSMAuthenticationSideEffect.PostNewLogin -> {
                    fsmAuthenticationState.postEvent(FSMAuthenticationState.StartNewLogin)
                }
                FSMAuthenticationSideEffect.PostAuthenticatedOpen -> {
                    fsmAuthenticationState.postEvent(FSMAuthenticationState.AuthenticatedOpenToken)
                }
                FSMAuthenticationSideEffect.PostRefreshingToken -> {
                    fsmAuthenticationState.postEvent(FSMAuthenticationState.RefreshingOpenToken)
                }
                FSMAuthenticationSideEffect.PostAuthenticatedPrivate -> {
                    fsmAuthenticationState.postEvent(FSMAuthenticationState.AuthenticatedPrivateToken)
                }
                FSMAuthenticationSideEffect.PostWaitToken -> {
                    fsmAuthenticationState.postEvent(FSMAuthenticationState.WaitingToken)
                }
                FSMAuthenticationSideEffect.PostWaitUserConfirmation -> {
                    fsmAuthenticationState.postEvent(FSMAuthenticationState.WaitingUserConfirmation)
                }
                FSMAuthenticationSideEffect.PostActionNeeded -> {
                    val action =
                        (validTransition.event as FSMAuthenticationEvent.OnUserActionNeeded).action
                    fsmAuthenticationState.postEvent(FSMAuthenticationState.WaitingUserAction(action))
                }
                FSMAuthenticationSideEffect.ResetAuthentication -> {
                    // delete the current credentials and restart a login process
                    viewModelScope.launch {
                        protoStore.deleteCredentials()
                        fsmAuthenticationState.postEvent(FSMAuthenticationState.StartNewLogin)
                    }
                }
            }
        }
    }

    fun checkCredentials() {
        viewModelScope.launch {
            val credentials = getCurrentCredentials().single()
            val userResult = userRepository.getUserOrError(credentials.accessToken)
            parseUserResult(userResult, credentials.deviceCode == PRIVATE_TOKEN)
        }
    }

    private fun parseUserResult(
        user: EitherResult<UnchainedNetworkException, User>,
        isPrivateToken: Boolean
    ) {
        when (user) {
            is EitherResult.Failure -> {
                // check errors, either ask for a retry or go to login
                when (user.failure) {
                    is APIError -> {
                        when (user.failure.errorCode) {
                            8 -> {
                                transitionAuthenticationMachine(FSMAuthenticationEvent.OnExpiredOpenToken)
                            }
                            9 -> {
                                // todo: add hint for user action needed
                                transitionAuthenticationMachine(
                                    FSMAuthenticationEvent.OnUserActionNeeded(
                                        UserAction.PERMISSION_DENIED
                                    )
                                )
                            }
                            10 -> {
                                transitionAuthenticationMachine(
                                    FSMAuthenticationEvent.OnUserActionNeeded(
                                        UserAction.TFA_NEEDED
                                    )
                                )
                            }
                            11 -> {
                                transitionAuthenticationMachine(
                                    FSMAuthenticationEvent.OnUserActionNeeded(
                                        UserAction.TFA_PENDING
                                    )
                                )
                            }
                            12 -> {
                                transitionAuthenticationMachine(FSMAuthenticationEvent.OnNotWorking)
                            }
                            13 -> {
                                transitionAuthenticationMachine(FSMAuthenticationEvent.OnNotWorking)
                            }
                            14 -> {
                                transitionAuthenticationMachine(FSMAuthenticationEvent.OnNotWorking)
                            }
                            15 -> {
                                transitionAuthenticationMachine(FSMAuthenticationEvent.OnNotWorking)
                            }
                            22 -> {
                                transitionAuthenticationMachine(
                                    FSMAuthenticationEvent.OnUserActionNeeded(
                                        UserAction.IP_NOT_ALLOWED
                                    )
                                )
                            }
                            else -> {
                                transitionAuthenticationMachine(
                                    FSMAuthenticationEvent.OnUserActionNeeded(
                                        UserAction.UNKNOWN
                                    )
                                )
                            }
                        }
                    }
                    is EmptyBodyError -> {
                        // should not happen
                        transitionAuthenticationMachine(
                            FSMAuthenticationEvent.OnUserActionNeeded(
                                UserAction.UNKNOWN
                            )
                        )
                    }
                    is NetworkError -> {
                        transitionAuthenticationMachine(
                            FSMAuthenticationEvent.OnUserActionNeeded(
                                UserAction.NETWORK_ERROR
                            )
                        )
                    }
                    is ApiConversionError -> {
                        transitionAuthenticationMachine(
                            FSMAuthenticationEvent.OnUserActionNeeded(
                                UserAction.RETRY_LATER
                            )
                        )
                    }
                }
            }
            is EitherResult.Success -> {

                if (isPrivateToken) {
                    transitionAuthenticationMachine(FSMAuthenticationEvent.OnWorkingPrivateToken)
                } else {
                    // todo: check if always posting Expired token makes sense. The idea is that this way I can manage the expiration time better
                    // transitionAuthenticationMachine(FSMAuthenticationEvent.OnWorkingOpenToken)
                    transitionAuthenticationMachine(FSMAuthenticationEvent.OnExpiredOpenToken)
                }
            }
        }
    }

    fun setAuthStatus(status: AuthenticationStatus) {
        newAuthenticationState.postEvent(status)
    }

    fun logout() {
        viewModelScope.launch {
            protoStore.deleteCredentials()
            setAuthStatus(AuthenticationStatus.Unauthenticated)
        }
    }

    /**
     * Used for testing and debugging if the token refresh works. Disables the current token.
     *
     */
    fun invalidateOpenSourceToken() {
        viewModelScope.launch {
            val c = protoStore.getCredentials()
            if (!c.refreshToken.isNullOrEmpty() && c.refreshToken != PRIVATE_TOKEN) {
                // setUnauthenticated()
                variousApiRepository.disableToken(c.accessToken)
            }
        }
    }

    suspend fun isTokenPrivate(): Boolean {
        val credentials = protoStore.getCredentials()
        return credentials.refreshToken == PRIVATE_TOKEN
    }

    fun refreshToken() {

        viewModelScope.launch {
            val credentials = protoStore.credentialsFlow.single()
            if (!credentials.refreshToken.isNullOrBlank() && credentials.refreshToken != PRIVATE_TOKEN) {
                // todo: add EitherResult to check for errors and retry eventually
                val newToken = authRepository.refreshToken(credentials)
                if (newToken != null) {
                    protoStore.setCredentials(
                        deviceCode = credentials.deviceCode,
                        clientId = credentials.clientId,
                        clientSecret = credentials.clientSecret,
                        accessToken = newToken.accessToken,
                        refreshToken = newToken.refreshToken
                    )

                    // program the refresh of the token
                    programTokenRefresh(newToken.expiresIn)

                    if (getAuthenticationMachineState() is FSMAuthenticationState.RefreshingOpenToken)
                        transitionAuthenticationMachine(FSMAuthenticationEvent.OnRefreshed)
                    // else I'm just refreshing before it expires
                    // todo: just set it to refreshing at the start of this function
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

    private fun checkConnectivity(context: Context) {
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

    suspend fun startAuthenticationFlow(currentCredentials: com.github.livingwithhippos.unchained.data.local.Credentials.CurrentCredential) {
        Timber.e("collecting credentials")
        // todo: check what happens with null values
        // default value for strings is an empty string, it means no currentCredentials available
        if (currentCredentials.accessToken.isEmpty()) {
            // 2. check credentials in room
            // todo: after a couple of releases just remove Credentials from the room db
            credentialsRepository.deleteIncompleteCredentials()
            val dbCredentials = credentialsRepository.getAllCredentials()
            // get the first available credentials
            val availableCredentials: Credentials? =
                dbCredentials.firstOrNull { it.deviceCode == PRIVATE_TOKEN }
                    ?: dbCredentials.firstOrNull { it.deviceCode != PRIVATE_TOKEN }

            // 3. if they are missing we don't have any credentials
            // set as unauthenticated to go to authentication flow
            if (availableCredentials == null) {
                if (newAuthenticationState.value?.peekContent() != AuthenticationStatus.Unauthenticated)
                    newAuthenticationState.postEvent(AuthenticationStatus.Unauthenticated)
            } else {
                // 4 copy the db credentials to the datastore and then check them
                //todo : check if this works and if it triggers a collect update otherwise run another collect below
                protoStore.setCredentials(
                    deviceCode = availableCredentials.deviceCode,
                    clientId = availableCredentials.clientId,
                    clientSecret = availableCredentials.clientSecret,
                    accessToken = availableCredentials.accessToken,
                    refreshToken = availableCredentials.refreshToken
                )
                getUser(updateAuthStatus = true).collect { }
            }
        } else {
            // 5 test the datastore credentials
            getUser(updateAuthStatus = true).collect { }
        }
    }

    fun getCurrentCredentials(): Flow<com.github.livingwithhippos.unchained.data.local.Credentials.CurrentCredential> =
        protoStore.credentialsFlow


    fun startAuthenticationFlow() {

        viewModelScope.launch {
            // 1. retrieve the datastore credentials (will return en empty instance if none)
            protoStore.credentialsFlow.collect { currentCredentials ->
                startAuthenticationFlow(currentCredentials)
            }
        }
    }

    private suspend fun getUser(updateAuthStatus: Boolean = true) = flow {

        protoStore.credentialsFlow.collect { credentials ->
            if (credentials.accessToken.isNotBlank()) {

                val userResult = userRepository.getUserOrError(credentials.accessToken)
                when (userResult) {
                    is EitherResult.Failure -> {
                        if (updateAuthStatus) {
                            when (userResult.failure) {
                                is APIError -> {
                                    when (userResult.failure.errorCode) {
                                        8 -> {
                                            newAuthenticationState.postEvent(
                                                AuthenticationStatus.RefreshToken
                                            )
                                        }
                                        9 -> {
                                            newAuthenticationState.postEvent(
                                                AuthenticationStatus.NeedUserAction(
                                                    UserAction.PERMISSION_DENIED
                                                )
                                            )
                                        }
                                        10 -> {
                                            newAuthenticationState.postEvent(
                                                AuthenticationStatus.NeedUserAction(
                                                    UserAction.TFA_NEEDED
                                                )
                                            )
                                        }
                                        11 -> {
                                            newAuthenticationState.postEvent(
                                                AuthenticationStatus.NeedUserAction(
                                                    UserAction.TFA_PENDING
                                                )
                                            )
                                        }
                                        12 -> {
                                            newAuthenticationState.postEvent(AuthenticationStatus.Unauthenticated)
                                        }
                                        13 -> {
                                            newAuthenticationState.postEvent(AuthenticationStatus.Unauthenticated)
                                        }
                                        14 -> {
                                            newAuthenticationState.postEvent(AuthenticationStatus.Unauthenticated)
                                        }
                                        15 -> {
                                            newAuthenticationState.postEvent(AuthenticationStatus.Unauthenticated)
                                        }
                                        22 -> {
                                            newAuthenticationState.postEvent(
                                                AuthenticationStatus.NeedUserAction(
                                                    UserAction.IP_NOT_ALLOWED
                                                )
                                            )
                                        }
                                        else -> {
                                            newAuthenticationState.postEvent(
                                                AuthenticationStatus.NeedUserAction(
                                                    UserAction.UNKNOWN
                                                )
                                            )
                                        }
                                    }
                                }
                                is EmptyBodyError -> {
                                    // should not happen
                                    newAuthenticationState.postEvent(
                                        AuthenticationStatus.NeedUserAction(
                                            UserAction.UNKNOWN
                                        )
                                    )
                                }
                                is NetworkError -> {
                                    newAuthenticationState.postEvent(
                                        AuthenticationStatus.NeedUserAction(
                                            UserAction.NETWORK_ERROR
                                        )
                                    )
                                }
                                is ApiConversionError -> {
                                    newAuthenticationState.postEvent(
                                        AuthenticationStatus.NeedUserAction(
                                            UserAction.RETRY_LATER
                                        )
                                    )
                                }
                            }
                        }
                        emit(null)
                    }
                    is EitherResult.Success -> {
                        if (updateAuthStatus) {
                            if (userResult.success.premium > 0)
                                newAuthenticationState.postEvent(
                                    AuthenticationStatus.Authenticated(
                                        userResult.success
                                    )
                                )
                            else
                                newAuthenticationState.postEvent(
                                    AuthenticationStatus.AuthenticatedNoPremium(
                                        userResult.success
                                    )
                                )
                        }
                        emit(userResult.success)
                    }
                }
            } else {
                if (updateAuthStatus) {
                    newAuthenticationState.postEvent(
                        AuthenticationStatus.Unauthenticated
                    )
                }
                emit(null)
            }
        }
    }

    suspend fun updateCredentials(
        deviceCode: String? = null,
        clientId: String? = null,
        clientSecret: String? = null,
        accessToken: String? = null,
        refreshToken: String? = null
    ) {

        protoStore.updateCredentials(
            deviceCode, clientId, clientSecret, accessToken, refreshToken
        )

    }

    /**************************
     * AUTH MACHINE FUNCTIONS *
     **************************/

    /**
     * Start the authentication machine flow
     *
     */
    fun startAuthenticationMachine() {
        viewModelScope.launch {
            // retrieve the datastore credentials (will return en empty instance if none)
            val protoCredentials = protoStore.credentialsFlow.singleOrNull()
            if (protoCredentials != null) {
                authStateMachine.transition(FSMAuthenticationEvent.OnAvailableCredentials)
            } else {
                authStateMachine.transition(FSMAuthenticationEvent.OnMissingCredentials)
            }
        }
    }

    fun getAuthenticationMachineState(): FSMAuthenticationState {
        return authStateMachine.state
    }

    fun transitionAuthenticationMachine(event: FSMAuthenticationEvent) {
        authStateMachine.transition(event)
    }

    companion object {
        const val KEY_TORRENT_DOWNLOAD_ID = "torrent_download_id_key"
        const val KEY_PLUGIN_DOWNLOAD_ID = "plugin_download_id_key"
        const val KEY_LAST_BACK_PRESS = "last_back_press_key"
        const val KEY_REFRESHING_TOKEN = "refreshing_token_key"
    }
}
