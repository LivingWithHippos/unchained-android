package com.github.livingwithhippos.unchained.start.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.local.Credentials
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.data.local.RemoteService
import com.github.livingwithhippos.unchained.data.local.RemoteServiceType
import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.ApiConversionError
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.model.User
import com.github.livingwithhippos.unchained.data.model.UserAction
import com.github.livingwithhippos.unchained.data.repository.AuthenticationRepository
import com.github.livingwithhippos.unchained.data.repository.CustomDownloadRepository
import com.github.livingwithhippos.unchained.data.repository.HostsRepository
import com.github.livingwithhippos.unchained.data.repository.InstallResult
import com.github.livingwithhippos.unchained.data.repository.KodiDeviceRepository
import com.github.livingwithhippos.unchained.data.repository.PluginRepository
import com.github.livingwithhippos.unchained.data.repository.PluginRepository.Companion.TYPE_UNCHAINED
import com.github.livingwithhippos.unchained.data.repository.RemoteDeviceRepository
import com.github.livingwithhippos.unchained.data.repository.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repository.UpdateRepository
import com.github.livingwithhippos.unchained.data.repository.UserRepository
import com.github.livingwithhippos.unchained.data.repository.VariousApiRepository
import com.github.livingwithhippos.unchained.lists.view.ListState
import com.github.livingwithhippos.unchained.statemachine.authentication.CurrentFSMAuthentication
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationEvent
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationSideEffect
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationState
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.MAGNET_PATTERN
import com.github.livingwithhippos.unchained.utilities.PRIVATE_TOKEN
import com.github.livingwithhippos.unchained.utilities.PreferenceKeys
import com.github.livingwithhippos.unchained.utilities.SIGNATURE
import com.github.livingwithhippos.unchained.utilities.download.DownloadWorker
import com.github.livingwithhippos.unchained.utilities.extension.isMagnet
import com.github.livingwithhippos.unchained.utilities.extension.isTorrent
import com.github.livingwithhippos.unchained.utilities.postEvent
import com.tinder.StateMachine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * a [ViewModel] subclass. Shared between the fragments to observe the authentication status and
 * update it.
 */
@HiltViewModel
class MainActivityViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    private val preferences: SharedPreferences,
    private val protoStore: ProtoStore,
    private val authRepository: AuthenticationRepository,
    private val userRepository: UserRepository,
    private val variousApiRepository: VariousApiRepository,
    private val hostsRepository: HostsRepository,
    private val pluginRepository: PluginRepository,
    private val kodiDeviceRepository: KodiDeviceRepository,
    private val customDownloadRepository: CustomDownloadRepository,
    private val torrentsRepository: TorrentsRepository,
    private val updateRepository: UpdateRepository,
    private val remoteDeviceRepository: RemoteDeviceRepository,
    @ApplicationContext applicationContext: Context,
) : ViewModel() {

    private val magnetPattern = Regex(MAGNET_PATTERN, RegexOption.IGNORE_CASE)

    val fsmAuthenticationState = MutableLiveData<Event<FSMAuthenticationState>?>()

    val userLiveData = MutableLiveData<Event<User>>()

    val externalLinkLiveData = MutableLiveData<Event<Uri>>()

    val downloadedFileLiveData = MutableLiveData<Event<Long>>()

    val notificationTorrentLiveData = MutableLiveData<Event<TorrentItem>>()

    val listStateLiveData = MutableLiveData<Event<ListState>>()

    val connectivityLiveData = MutableLiveData<Boolean?>()
    // val currentNetworkLiveData = MutableLiveData<Network?>()

    val jumpTabLiveData = MutableLiveData<Event<String>>()

    // todo: use a better name to reflect the difference between this and externalLinkLiveData
    val linkLiveData = MutableLiveData<Event<String>?>()

    val messageLiveData = MutableLiveData<Event<MainActivityMessage>?>()

    private val workManager = WorkManager.getInstance(applicationContext)

    private var refreshJob: Job? = null

    private val authStateMachine:
        StateMachine<FSMAuthenticationState, FSMAuthenticationEvent, FSMAuthenticationSideEffect> =
        StateMachine.create {
            initialState(FSMAuthenticationState.Start)

            state<FSMAuthenticationState.Start> {
                on<FSMAuthenticationEvent.OnAvailableCredentials> {
                    transitionTo(
                        FSMAuthenticationState.CheckCredentials,
                        FSMAuthenticationSideEffect.CheckingCredentials,
                    )
                }
                on<FSMAuthenticationEvent.OnMissingCredentials> {
                    transitionTo(
                        FSMAuthenticationState.StartNewLogin,
                        FSMAuthenticationSideEffect.PostNewLogin,
                    )
                }
            }

            state<FSMAuthenticationState.CheckCredentials> {
                on<FSMAuthenticationEvent.OnWorkingOpenToken> {
                    transitionTo(
                        FSMAuthenticationState.AuthenticatedOpenToken,
                        FSMAuthenticationSideEffect.PostAuthenticatedOpen,
                    )
                }
                on<FSMAuthenticationEvent.OnExpiredOpenToken> {
                    transitionTo(
                        FSMAuthenticationState.RefreshingOpenToken,
                        FSMAuthenticationSideEffect.PostRefreshingToken,
                    )
                }
                on<FSMAuthenticationEvent.OnWorkingPrivateToken> {
                    transitionTo(
                        FSMAuthenticationState.AuthenticatedPrivateToken,
                        FSMAuthenticationSideEffect.PostAuthenticatedPrivate,
                    )
                }
                on<FSMAuthenticationEvent.OnNotWorking> {
                    transitionTo(
                        FSMAuthenticationState.StartNewLogin,
                        FSMAuthenticationSideEffect.ResetAuthentication,
                    )
                }
                on<FSMAuthenticationEvent.OnUserActionNeeded> {
                    transitionTo(
                        FSMAuthenticationState.WaitingUserAction(null),
                        FSMAuthenticationSideEffect.PostActionNeeded,
                    )
                }
            }

            state<FSMAuthenticationState.WaitingUserAction> {
                on<FSMAuthenticationEvent.OnUserActionRetry> {
                    transitionTo(
                        FSMAuthenticationState.CheckCredentials,
                        FSMAuthenticationSideEffect.CheckingCredentials,
                    )
                }
                on<FSMAuthenticationEvent.OnUserActionReset> {
                    transitionTo(
                        FSMAuthenticationState.StartNewLogin,
                        FSMAuthenticationSideEffect.ResetAuthentication,
                    )
                }
            }

            state<FSMAuthenticationState.StartNewLogin> {
                on<FSMAuthenticationEvent.OnAuthLoaded> {
                    transitionTo(
                        FSMAuthenticationState.WaitingUserConfirmation,
                        FSMAuthenticationSideEffect.PostWaitUserConfirmation,
                    )
                }
                // I can get a private token on this state too
                on<FSMAuthenticationEvent.OnPrivateToken> {
                    transitionTo(
                        FSMAuthenticationState.CheckCredentials,
                        FSMAuthenticationSideEffect.CheckingCredentials,
                    )
                }
            }

            state<FSMAuthenticationState.WaitingUserConfirmation> {
                on<FSMAuthenticationEvent.OnUserConfirmationExpired> {
                    transitionTo(
                        FSMAuthenticationState.StartNewLogin,
                        FSMAuthenticationSideEffect.PostNewLogin,
                    )
                }
                on<FSMAuthenticationEvent.OnUserConfirmationLoaded> {
                    transitionTo(
                        FSMAuthenticationState.WaitingToken,
                        FSMAuthenticationSideEffect.PostWaitToken,
                    )
                }
                on<FSMAuthenticationEvent.OnUserConfirmationMissing> {
                    transitionTo(
                        FSMAuthenticationState.WaitingUserConfirmation,
                        FSMAuthenticationSideEffect.PostWaitUserConfirmation,
                    )
                }
                // I can get a private token on this state too
                on<FSMAuthenticationEvent.OnPrivateToken> {
                    transitionTo(
                        FSMAuthenticationState.CheckCredentials,
                        FSMAuthenticationSideEffect.CheckingCredentials,
                    )
                }
            }

            state<FSMAuthenticationState.WaitingToken> {
                on<FSMAuthenticationEvent.OnOpenTokenLoaded> {
                    transitionTo(
                        FSMAuthenticationState.CheckCredentials,
                        FSMAuthenticationSideEffect.CheckingCredentials,
                    )
                }
                // I can get a private token on this state too
                on<FSMAuthenticationEvent.OnPrivateToken> {
                    transitionTo(
                        FSMAuthenticationState.CheckCredentials,
                        FSMAuthenticationSideEffect.CheckingCredentials,
                    )
                }
            }

            state<FSMAuthenticationState.AuthenticatedOpenToken> {
                on<FSMAuthenticationEvent.OnExpiredOpenToken> {
                    transitionTo(
                        FSMAuthenticationState.RefreshingOpenToken,
                        FSMAuthenticationSideEffect.PostRefreshingToken,
                    )
                }
                on<FSMAuthenticationEvent.OnAuthenticationError> {
                    transitionTo(
                        FSMAuthenticationState.WaitingUserAction(null),
                        FSMAuthenticationSideEffect.PostActionNeeded,
                    )
                }
                on<FSMAuthenticationEvent.OnLogout> {
                    transitionTo(
                        FSMAuthenticationState.StartNewLogin,
                        FSMAuthenticationSideEffect.PostNewLogin,
                    )
                }
            }

            state<FSMAuthenticationState.RefreshingOpenToken> {
                on<FSMAuthenticationEvent.OnRefreshed> {
                    transitionTo(
                        FSMAuthenticationState.AuthenticatedOpenToken,
                        FSMAuthenticationSideEffect.PostAuthenticatedOpen,
                    )
                }
                on<FSMAuthenticationEvent.OnLogout> {
                    transitionTo(
                        FSMAuthenticationState.StartNewLogin,
                        FSMAuthenticationSideEffect.PostNewLogin,
                    )
                }
                on<FSMAuthenticationEvent.OnAuthenticationError> {
                    transitionTo(
                        FSMAuthenticationState.WaitingUserAction(null),
                        FSMAuthenticationSideEffect.PostActionNeeded,
                    )
                }
                on<FSMAuthenticationEvent.OnNotWorking> {
                    transitionTo(
                        FSMAuthenticationState.StartNewLogin,
                        FSMAuthenticationSideEffect.ResetAuthentication,
                    )
                }
            }

            state<FSMAuthenticationState.AuthenticatedPrivateToken> {
                on<FSMAuthenticationEvent.OnLogout> {
                    transitionTo(
                        FSMAuthenticationState.StartNewLogin,
                        FSMAuthenticationSideEffect.PostNewLogin,
                    )
                }
                on<FSMAuthenticationEvent.OnAuthenticationError> {
                    transitionTo(
                        FSMAuthenticationState.WaitingUserAction(null),
                        FSMAuthenticationSideEffect.PostActionNeeded,
                    )
                }
            }

            onTransition {
                if (it !is StateMachine.Transition.Valid) {
                    Timber.e("Wrong transition ${it.event} for state ${it.fromState}")
                    return@onTransition
                }
                when (it.sideEffect) {
                    null -> {
                        // do nothing
                    }
                    is FSMAuthenticationSideEffect.CheckingCredentials -> {
                        fsmAuthenticationState.postValue(
                            Event(FSMAuthenticationState.CheckCredentials)
                        )
                    }
                    FSMAuthenticationSideEffect.PostNewLogin -> {
                        fsmAuthenticationState.postValue(
                            Event(FSMAuthenticationState.StartNewLogin)
                        )
                    }
                    FSMAuthenticationSideEffect.PostAuthenticatedOpen -> {
                        fsmAuthenticationState.postValue(
                            Event(FSMAuthenticationState.AuthenticatedOpenToken)
                        )
                    }
                    FSMAuthenticationSideEffect.PostRefreshingToken -> {
                        fsmAuthenticationState.postValue(
                            Event(FSMAuthenticationState.RefreshingOpenToken)
                        )
                    }
                    FSMAuthenticationSideEffect.PostAuthenticatedPrivate -> {
                        fsmAuthenticationState.postValue(
                            Event(FSMAuthenticationState.AuthenticatedPrivateToken)
                        )
                    }
                    FSMAuthenticationSideEffect.PostWaitToken -> {
                        fsmAuthenticationState.postValue(Event(FSMAuthenticationState.WaitingToken))
                    }
                    FSMAuthenticationSideEffect.PostWaitUserConfirmation -> {
                        fsmAuthenticationState.postValue(
                            Event(FSMAuthenticationState.WaitingUserConfirmation)
                        )
                    }
                    FSMAuthenticationSideEffect.PostActionNeeded -> {
                        when (it.event) {
                            is FSMAuthenticationEvent.OnUserActionNeeded -> {
                                val action =
                                    (it.event as FSMAuthenticationEvent.OnUserActionNeeded).action

                                fsmAuthenticationState.postValue(
                                    Event(FSMAuthenticationState.WaitingUserAction(action))
                                )
                            }
                            is FSMAuthenticationEvent.OnAuthenticationError -> {
                                fsmAuthenticationState.postValue(
                                    Event(FSMAuthenticationState.WaitingUserAction(null))
                                )
                            }
                            else -> {
                                Timber.e("Wrong PostActionNeeded event: ${it.event}")
                            }
                        }
                    }
                    FSMAuthenticationSideEffect.ResetAuthentication -> {
                        // delete the current credentials and restart a login process
                        viewModelScope.launch {
                            protoStore.deleteCredentials()
                            fsmAuthenticationState.postValue(
                                Event(FSMAuthenticationState.StartNewLogin)
                            )
                        }
                    }
                }
            }
        }

    private fun setCachedUser(user: User?) {
        savedStateHandle["current_user_key"] = user
    }

    fun getCachedUser(): User? {
        return savedStateHandle["current_user_key"]
    }

    fun fetchUser() {
        viewModelScope.launch {
            val credentials = protoStore.getCredentials()
            val user = userRepository.getUserInfo(credentials.accessToken)
            if (user != null) {
                setCachedUser(user)
                userLiveData.postEvent(user)
            }
        }
    }

    fun checkCredentials() {
        viewModelScope.launch {
            // todo: how to do this
            val credentials: Credentials.CurrentCredential? =
                protoStore.credentialsFlow.firstOrNull { it.accessToken.isNotBlank() }
            if (credentials == null) {
                recheckAuthenticationStatus()
            } else {
                val userResult = userRepository.getUserOrError(credentials.accessToken)
                parseUserResult(userResult, credentials.refreshToken == PRIVATE_TOKEN)
            }
        }
    }

    private fun parseUserResult(
        user: EitherResult<UnchainedNetworkException, User>,
        isPrivateToken: Boolean,
    ) {
        when (user) {
            is EitherResult.Success -> {
                setCachedUser(user.success)
                if (isPrivateToken) {
                    transitionAuthenticationMachine(FSMAuthenticationEvent.OnWorkingPrivateToken)
                } else {
                    // todo: check if always posting Expired token makes sense. The idea is that
                    // this way I
                    // can manage the expiration time better
                    // transitionAuthenticationMachine(FSMAuthenticationEvent.OnWorkingOpenToken)
                    transitionAuthenticationMachine(FSMAuthenticationEvent.OnExpiredOpenToken)
                }
            }
            is EitherResult.Failure -> {
                // check errors, either ask for a retry or go to login
                onParseCallFailure(user.failure, isPrivateToken)
            }
        }
    }

    private fun recheckAuthenticationStatus() {
        when (getAuthenticationMachineState()) {
            FSMAuthenticationState.AuthenticatedOpenToken,
            FSMAuthenticationState.AuthenticatedPrivateToken,
            FSMAuthenticationState.RefreshingOpenToken -> {
                transitionAuthenticationMachine(FSMAuthenticationEvent.OnLogout)
            }
            else -> {
                Timber.e(
                    "Asked for logout while in a wrong state: ${getAuthenticationMachineState()}"
                )
            }
        }
    }

    /** Used for testing and debugging if the token refresh works. Disables the current token. */
    fun invalidateOpenSourceToken() {
        viewModelScope.launch {
            val c = protoStore.getCredentials()
            if (!c.refreshToken.isNullOrEmpty() && c.refreshToken != PRIVATE_TOKEN) {
                // setUnauthenticated()
                variousApiRepository.disableToken()
            }
        }
    }

    suspend fun isTokenPrivate(): Boolean {
        val credentials = protoStore.getCredentials()
        return credentials.refreshToken == PRIVATE_TOKEN
    }

    fun refreshToken() {

        viewModelScope.launch {
            val credentials = protoStore.getCredentials()
            if (
                !credentials.refreshToken.isNullOrBlank() &&
                    credentials.refreshToken != PRIVATE_TOKEN
            ) {
                // todo: add EitherResult to check for errors and retry eventually
                when (val newToken = authRepository.refreshTokenWithError(credentials)) {
                    is EitherResult.Success -> {
                        protoStore.setCredentials(
                            deviceCode = credentials.deviceCode,
                            clientId = credentials.clientId,
                            clientSecret = credentials.clientSecret,
                            accessToken = newToken.success.accessToken,
                            refreshToken = newToken.success.refreshToken,
                        )

                        // program the refresh of the token
                        programTokenRefresh(newToken.success.expiresIn)

                        if (
                            getAuthenticationMachineState()
                                is FSMAuthenticationState.RefreshingOpenToken
                        )
                            transitionAuthenticationMachine(FSMAuthenticationEvent.OnRefreshed)
                        // else I'm just refreshing before it expires
                        // todo: just set it to refreshing at the start of this function
                    }
                    is EitherResult.Failure -> {
                        onParseCallFailure(
                            newToken.failure,
                            credentials.deviceCode == PRIVATE_TOKEN,
                        )
                    }
                }
            }
        }
    }

    private fun onParseCallFailure(failure: UnchainedNetworkException, isPrivateToken: Boolean) {
        when (failure) {
            is APIError -> {
                when (failure.errorCode) {
                    8 -> {
                        when (getAuthenticationMachineState()) {
                            FSMAuthenticationState.AuthenticatedOpenToken -> {
                                // refresh token
                                transitionAuthenticationMachine(
                                    FSMAuthenticationEvent.OnExpiredOpenToken
                                )
                            }
                            FSMAuthenticationState.CheckCredentials -> {
                                if (isPrivateToken)
                                    transitionAuthenticationMachine(
                                        FSMAuthenticationEvent.OnNotWorking
                                    )
                                else
                                    transitionAuthenticationMachine(
                                        FSMAuthenticationEvent.OnExpiredOpenToken
                                    )
                            }
                            FSMAuthenticationState.AuthenticatedPrivateToken -> {
                                // a private token was incorrect
                                // todo: should recover from this according to the current fragment
                                transitionAuthenticationMachine(FSMAuthenticationEvent.OnNotWorking)
                            }
                            else -> {
                                // do nothing
                            }
                        }
                    }
                    9 -> {
                        Timber.e("onParseCallFailure " + 9)
                        // 9 is permission denied which should mean the token is not valid at all
                        // and should be
                        // discarded
                        transitionAuthenticationMachine(FSMAuthenticationEvent.OnNotWorking)
                    }
                    10 -> {
                        transitionAuthenticationMachine(
                            FSMAuthenticationEvent.OnUserActionNeeded(UserAction.TFA_NEEDED)
                        )
                    }
                    11 -> {
                        transitionAuthenticationMachine(
                            FSMAuthenticationEvent.OnUserActionNeeded(UserAction.TFA_PENDING)
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
                            FSMAuthenticationEvent.OnUserActionNeeded(UserAction.IP_NOT_ALLOWED)
                        )
                    }
                    else -> {
                        transitionAuthenticationMachine(
                            FSMAuthenticationEvent.OnUserActionNeeded(UserAction.UNKNOWN)
                        )
                    }
                }
            }
            is EmptyBodyError -> {
                // should not happen
                transitionAuthenticationMachine(
                    FSMAuthenticationEvent.OnUserActionNeeded(UserAction.UNKNOWN)
                )
            }
            is NetworkError -> {
                transitionAuthenticationMachine(
                    FSMAuthenticationEvent.OnUserActionNeeded(UserAction.NETWORK_ERROR)
                )
            }
            is ApiConversionError -> {
                transitionAuthenticationMachine(
                    FSMAuthenticationEvent.OnUserActionNeeded(UserAction.RETRY_LATER)
                )
            }
        }
    }

    fun addLink(uri: Uri) {
        externalLinkLiveData.postEvent(uri)
    }

    fun checkTorrentDownload(downloadID: Long) {
        val torrentID = savedStateHandle.get<Long>(KEY_TORRENT_DOWNLOAD_ID)
        if (torrentID == downloadID) downloadedFileLiveData.postEvent(torrentID)
    }

    // todo: move this stuff to a shared navigationViewModel
    fun setListState(state: ListState) {
        listStateLiveData.postEvent(state)
    }

    fun getLastBackPress(): Long {
        return savedStateHandle.get<Long>(KEY_LAST_BACK_PRESS) ?: 0
    }

    fun setLastBackPress(time: Long) {
        savedStateHandle[KEY_LAST_BACK_PRESS] = time
    }

    private fun programTokenRefresh(secondsDelay: Int) {
        refreshJob?.cancel()
        refreshJob =
            viewModelScope.launch {
                // secondsDelay*950L -> expiration time - 5%
                delay(secondsDelay * 950L)
                if (isActive) refreshToken()
            }
    }

    fun downloadSupportedLink(link: String) {
        viewModelScope.launch {
            when {
                link.isMagnet() -> linkLiveData.postValue(Event(link))
                link.isTorrent() -> linkLiveData.postValue(Event(link))
                link.endsWith(TYPE_UNCHAINED, ignoreCase = true) -> {
                    // only accept github links for now
                    val newLink = convertGithubToRaw(link)
                    if (newLink != null) linkLiveData.postValue(Event(newLink))
                    else
                        messageLiveData.postValue(
                            Event(MainActivityMessage.StringID(R.string.invalid_url))
                        )
                }
                else -> {
                    var matchFound = false
                    // check the hosts regexs
                    for (hostRegex in hostsRepository.getHostsRegex()) {
                        val m: Matcher = Pattern.compile(hostRegex.regex).matcher(link)
                        if (m.matches()) {
                            matchFound = true
                            linkLiveData.postValue(Event(link))
                            break
                        }
                    }
                    // check the folders regexs
                    if (!matchFound) {
                        for (hostRegex in hostsRepository.getFoldersRegex()) {
                            val m: Matcher = Pattern.compile(hostRegex.regex).matcher(link)
                            if (m.matches()) {
                                matchFound = true
                                linkLiveData.postValue(Event(link))
                                break
                            }
                        }
                    }
                    if (!matchFound)
                        messageLiveData.postValue(
                            Event(MainActivityMessage.StringID(R.string.host_match_not_found))
                        )
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
            github.startsWith("https://www.github.com") ||
                github.startsWith("https://github.com") -> {
                val regex =
                    "https?://(www.)?github.com/$username/$repo/$type/$branch/$path".toRegex()
                val match: MatchResult = regex.find(github) ?: return null
                return "https://raw.githubusercontent.com/" +
                    match.groupValues[2] +
                    "/" +
                    match.groupValues[3] +
                    "/" +
                    match.groupValues[5] +
                    "/" +
                    match.groupValues[6]
            }
            github.startsWith("https://raw.githubusercontent.com") -> {
                return github
            }
            else -> return null
        }
    }

    fun addTorrentId(torrentID: String) {
        viewModelScope.launch {
            val torrent: TorrentItem? = torrentsRepository.getTorrentInfo(torrentID)
            if (torrent != null) notificationTorrentLiveData.postEvent(torrent)
            else
                Timber.e(
                    "Could not retrieve torrent data from click on notification, id $torrentID"
                )
        }
    }

    fun addPluginFromDisk(context: Context, data: Uri) {
        viewModelScope.launch {
            postPluginInstallResult(pluginRepository.savePluginFromDisk(context, data))
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
                    network,
                )
                // currentNetworkLiveData.postValue(null)
                connectivityLiveData.postValue(false)
            }
        }

    fun addConnectivityCheck(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            checkConnectivity(context)
        }
    }

    fun removeConnectivityCheck(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    @Suppress("DEPRECATION")
    private fun checkConnectivity(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            var isConnected = false
            val networks = connectivityManager.allNetworks
            for (net in networks) {
                val netInfo = connectivityManager.getNetworkCapabilities(net)
                if (
                    netInfo != null &&
                        netInfo.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                ) {
                    isConnected = true
                    break
                }
            }
            connectivityLiveData.postValue(isConnected)
        }
    }

    fun updateCredentials(
        deviceCode: String? = null,
        clientId: String? = null,
        clientSecret: String? = null,
        accessToken: String? = null,
        refreshToken: String? = null,
    ) {
        viewModelScope.launch {
            protoStore.updateCredentials(
                deviceCode,
                clientId,
                clientSecret,
                accessToken,
                refreshToken,
            )
        }
    }

    fun updateCredentialsDeviceCode(deviceCode: String) {
        viewModelScope.launch { protoStore.updateDeviceCode(deviceCode) }
    }

    fun updateCredentialsAccessToken(accessToken: String) {
        viewModelScope.launch { protoStore.updateAccessToken(accessToken) }
    }

    fun updateCredentialsRefreshToken(refreshToken: String) {
        viewModelScope.launch { protoStore.updateRefreshToken(refreshToken) }
    }

    /**
     * ***********************
     * AUTH MACHINE FUNCTIONS *
     * ************************
     */

    /** Start the authentication machine flow */
    fun startAuthenticationMachine() {
        viewModelScope.launch {
            // retrieve the datastore credentials (will return en empty instance if none)
            val protoCredentials = protoStore.getCredentials()
            if (protoCredentials.accessToken.isNotBlank()) {
                transitionAuthenticationMachine(FSMAuthenticationEvent.OnAvailableCredentials)
            } else {
                transitionAuthenticationMachine(FSMAuthenticationEvent.OnMissingCredentials)
            }
        }
    }

    /**
     * Returns the current finite state authentication machine state
     *
     * @return
     */
    fun getAuthenticationMachineState(): FSMAuthenticationState {
        return authStateMachine.state
    }

    /**
     * Returns a more generic status of the authentication machine as ready, checking and not ready.
     *
     * @return CurrentFSMAuthentication
     */
    fun getCurrentAuthenticationStatus(): CurrentFSMAuthentication =
        when (getAuthenticationMachineState()) {
            FSMAuthenticationState.AuthenticatedPrivateToken,
            FSMAuthenticationState.AuthenticatedOpenToken -> CurrentFSMAuthentication.Authenticated
            FSMAuthenticationState.Start,
            FSMAuthenticationState.CheckCredentials,
            FSMAuthenticationState.RefreshingOpenToken -> CurrentFSMAuthentication.Waiting
            else -> CurrentFSMAuthentication.Unauthenticated
        }

    fun transitionAuthenticationMachine(event: FSMAuthenticationEvent) {
        authStateMachine.transition(event)
    }

    /** Jump to the main screen set in the settings */
    fun goToStartUpScreen() {
        jumpTabLiveData.postEvent(preferences.getString("main_screen", "user") ?: "user")
    }

    /** Loads the old saved kodi preference into the new db one and then deletes it */
    fun migrateKodiPreferences() {
        viewModelScope.launch {
            // there was another migration from the old preferences but it's been out a lot so we
            // removed it

            val oldKodiDevices = kodiDeviceRepository.getDevices()

            var migratedCounter = 0

            if (oldKodiDevices.isNotEmpty()) {
                for (kodi in oldKodiDevices) {
                    // since we delete them and the user won't be able to create new ones
                    // we don't need to check if they already are in the RemoteDevice table

                    val newDevice =
                        RemoteDevice(
                            // id = 0 should be fine since it's autoincrement
                            id = 0,
                            name = kodi.name,
                            address = kodi.address,
                            isDefault = false,
                        )

                    val insertedRow = remoteDeviceRepository.insertDevice(newDevice)
                    val deviceID = remoteDeviceRepository.getDeviceIDByRow(insertedRow)

                    if (deviceID == null) {
                        Timber.e("Error inserting remote device $newDevice")
                        continue
                    }

                    val newService =
                        RemoteService(
                            id = 0,
                            device = deviceID,
                            name = "Kodi",
                            port = kodi.port,
                            username = kodi.username,
                            password = kodi.password,
                            type = RemoteServiceType.KODI.value,
                        )

                    remoteDeviceRepository.insertService(newService)

                    migratedCounter += 1
                }

                kodiDeviceRepository.deleteAll()
                Timber.i("Migrated $migratedCounter kodi devices out of ${oldKodiDevices.size}")
            }
        }
    }

    fun downloadFileToCache(
        link: String,
        fileName: String,
        cacheDir: File,
        suffix: String? = null,
    ) = customDownloadRepository.downloadToCache(link, fileName, cacheDir, suffix).asLiveData()

    fun clearCache(cacheDir: File) {
        cacheDir.listFiles()?.forEach { if (it.name != "image_cache") it.deleteRecursively() }
    }

    private fun checkUpdateVersion(
        localVersion: Int,
        remoteVersion: Int?,
        lastVersionChecked: Int,
        signature: String,
    ) {
        if (remoteVersion != null) {
            if (remoteVersion > localVersion && remoteVersion > lastVersionChecked) {
                messageLiveData.postValue(Event(MainActivityMessage.UpdateFound(signature)))
            }
            with(preferences.edit()) {
                putInt(KEY_LAST_UPDATE_VERSION_CHECKED, remoteVersion)
                apply()
            }
        }
    }

    fun checkUpdates(versionCode: Int, signatures: List<String>) {
        viewModelScope.launch {
            // ignore errors getting updates?
            // todo: Add a toast if a button to check updates is added
            val updates = updateRepository.getUpdates(SIGNATURE.URL)
            if (updates != null) {
                val lastVersionChecked = preferences.getInt(KEY_LAST_UPDATE_VERSION_CHECKED, -1)
                for (signature in signatures) {
                    when (val upperSignature = signature.uppercase()) {
                        SIGNATURE.F_DROID -> {
                            checkUpdateVersion(
                                versionCode,
                                updates.fDroid?.versionCode,
                                lastVersionChecked,
                                upperSignature,
                            )
                            break
                        }
                        SIGNATURE.GITHUB -> {
                            checkUpdateVersion(
                                versionCode,
                                updates.github?.versionCode,
                                lastVersionChecked,
                                upperSignature,
                            )
                            break
                        }
                        SIGNATURE.PLAY_STORE -> {
                            checkUpdateVersion(
                                versionCode,
                                updates.playStore?.versionCode,
                                lastVersionChecked,
                                upperSignature,
                            )
                            break
                        }
                        else -> {
                            Timber.w("Unknown apk signature, may be debugging: $upperSignature")
                        }
                    }
                }
            }
        }
    }

    fun setDownloadFolder(uri: Uri) {
        uri.describeContents()
        with(preferences.edit()) {
            putString(KEY_DOWNLOAD_FOLDER, uri.toString())
            apply()
        }
    }

    fun getDownloadFolder(): Uri? {
        val folder = preferences.getString(KEY_DOWNLOAD_FOLDER, null)
        if (folder != null) {
            try {
                return Uri.parse(folder)
            } catch (e: Exception) {
                Timber.e("Error parsing the saved folder Uri $folder")
            }
        }
        return null
    }

    fun requireDownloadFolder() {
        messageLiveData.postValue(Event(MainActivityMessage.RequireDownloadFolder))
    }

    fun requireDownloadPermissions() {
        messageLiveData.postValue(Event(MainActivityMessage.RequireDownloadPermissions))
    }

    fun requireNotificationPermissions(callDelay: Boolean = true) {
        viewModelScope.launch {
            if (callDelay) {
                delay(500)
            }
            messageLiveData.postValue(Event(MainActivityMessage.RequireNotificationPermissions))
        }
    }

    fun getDownloadManagerPreference(): String {
        return preferences.getString(
            PreferenceKeys.DownloadManager.KEY,
            PreferenceKeys.DownloadManager.SYSTEM,
        ) ?: PreferenceKeys.DownloadManager.SYSTEM
    }

    fun startDownloadWorker(content: MainActivityMessage.DownloadEnqueued, folder: Uri) {

        val unmeteredConnectionOnly =
            preferences.getBoolean(PreferenceKeys.DownloadManager.UNMETERED_ONLY_KEY, false)

        val constraints =
            Constraints.Builder()
                .apply {
                    if (unmeteredConnectionOnly) setRequiredNetworkType(NetworkType.UNMETERED)
                    else setRequiredNetworkType(NetworkType.CONNECTED)
                }
                .setRequiresStorageNotLow(true)
                .build()

        val data: Data =
            Data.Builder()
                .apply {
                    putString(KEY_FOLDER_URI, folder.toString())
                    putString(KEY_DOWNLOAD_SOURCE, content.source)
                    putString(KEY_DOWNLOAD_NAME, content.fileName)
                }
                .build()

        val downloadFileRequest =
            OneTimeWorkRequestBuilder<DownloadWorker>()
                .addTag(content.source)
                .setInputData(data)
                .setConstraints(constraints)
                .build()

        // use KEEP or REPLACE
        workManager.enqueueUniqueWork(content.source, ExistingWorkPolicy.KEEP, downloadFileRequest)
    }

    fun startMultipleDownloadWorkers(folder: Uri, downloads: List<DownloadItem>) {

        val unmeteredConnectionOnly =
            preferences.getBoolean(PreferenceKeys.DownloadManager.UNMETERED_ONLY_KEY, false)

        val constraints =
            Constraints.Builder()
                .apply {
                    if (unmeteredConnectionOnly) setRequiredNetworkType(NetworkType.UNMETERED)
                    else setRequiredNetworkType(NetworkType.CONNECTED)
                }
                .setRequiresStorageNotLow(true)
                .build()

        val work: List<OneTimeWorkRequest> =
            downloads.map {
                val data =
                    Data.Builder()
                        .apply {
                            putString(KEY_FOLDER_URI, folder.toString())
                            putString(KEY_DOWNLOAD_SOURCE, it.download)
                            putString(KEY_DOWNLOAD_NAME, it.filename)
                        }
                        .build()

                OneTimeWorkRequestBuilder<DownloadWorker>()
                    .setInputData(data)
                    .setConstraints(constraints)
                    .addTag(it.download)
                    .build()
            }

        // use KEEP or REPLACE
        workManager.enqueue(work)
    }

    fun enqueueDownload(sourceUrl: String, fileName: String) {
        // todo: folder should be nullable for the system download manager
        messageLiveData.postValue(Event(MainActivityMessage.DownloadEnqueued(sourceUrl, fileName)))
    }

    fun enqueueDownloads(downloads: List<DownloadItem>) {
        // todo: folder should be nullable for the system download manager
        messageLiveData.postValue(Event(MainActivityMessage.MultipleDownloadsEnqueued(downloads)))
    }

    fun getDownloadOnUnmeteredOnlyPreference(): Boolean {
        return preferences.getBoolean(PreferenceKeys.DownloadManager.UNMETERED_ONLY_KEY, false)
    }

    fun downloadPlugin(context: Context, url: String, repositoryURL: String?) {
        viewModelScope.launch {
            when (val result = customDownloadRepository.downloadPlugin(url)) {
                is EitherResult.Failure -> {
                    Timber.e("Error downloading plugin at $url:\n${result.failure}")
                }
                is EitherResult.Success -> {
                    val installResult =
                        pluginRepository.savePlugin(context, result.success, repositoryURL)
                    postPluginInstallResult(installResult)
                }
            }
        }
    }

    private fun postPluginInstallResult(result: InstallResult) {
        when (result) {
            is InstallResult.Error ->
                messageLiveData.postValue(
                    Event(MainActivityMessage.StringID(R.string.plugin_install_not_installed))
                )
            InstallResult.Incompatible ->
                messageLiveData.postValue(
                    Event(MainActivityMessage.StringID(R.string.plugin_install_incompatible))
                )
            InstallResult.Installed ->
                messageLiveData.postValue(
                    Event(MainActivityMessage.StringID(R.string.plugin_install_installed))
                )
        }
    }

    companion object {
        const val KEY_DOWNLOAD_FOLDER = "download_folder_key"
        const val KEY_TORRENT_DOWNLOAD_ID = "torrent_download_id_key"
        const val KEY_LAST_BACK_PRESS = "last_back_press_key"
        const val KEY_LAST_UPDATE_VERSION_CHECKED = "last_update_version_checked_key"

        const val KEY_FOLDER_URI = "download_folder_key"
        const val KEY_DOWNLOAD_SOURCE = "download_source_key"
        const val KEY_DOWNLOAD_NAME = "download_name_key"
    }
}

sealed class MainActivityMessage {
    data class StringID(val id: Int) : MainActivityMessage()

    data class InstalledPlugins(val number: Int) : MainActivityMessage()

    data class UpdateFound(val signature: String) : MainActivityMessage()

    data object RequireDownloadFolder : MainActivityMessage()

    data object RequireDownloadPermissions : MainActivityMessage()

    data object RequireNotificationPermissions : MainActivityMessage()

    data class DownloadEnqueued(val source: String, val fileName: String) : MainActivityMessage()

    data class MultipleDownloadsEnqueued(val downloads: List<DownloadItem>) : MainActivityMessage()
}
