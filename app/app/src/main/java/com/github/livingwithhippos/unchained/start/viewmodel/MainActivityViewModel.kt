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
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.local.Credentials
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.ApiConversionError
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.KodiDevice
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.model.User
import com.github.livingwithhippos.unchained.data.model.UserAction
import com.github.livingwithhippos.unchained.data.repository.AuthenticationRepository
import com.github.livingwithhippos.unchained.data.repository.CustomDownloadRepository
import com.github.livingwithhippos.unchained.data.repository.HostsRepository
import com.github.livingwithhippos.unchained.data.repository.KodiDeviceRepository
import com.github.livingwithhippos.unchained.data.repository.PluginRepository
import com.github.livingwithhippos.unchained.data.repository.PluginRepository.Companion.TYPE_UNCHAINED
import com.github.livingwithhippos.unchained.data.repository.UserRepository
import com.github.livingwithhippos.unchained.data.repository.VariousApiRepository
import com.github.livingwithhippos.unchained.lists.view.ListState
import com.github.livingwithhippos.unchained.lists.view.ListsTabFragment
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.statemachine.authentication.CurrentFSMAuthentication
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationEvent
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationSideEffect
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationState
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.PLUGINS_PACK_FOLDER
import com.github.livingwithhippos.unchained.utilities.PRIVATE_TOKEN
import com.github.livingwithhippos.unchained.utilities.UnzipUtils
import com.github.livingwithhippos.unchained.utilities.extension.getDownloadedFileUri
import com.github.livingwithhippos.unchained.utilities.extension.isMagnet
import com.github.livingwithhippos.unchained.utilities.extension.isTorrent
import com.github.livingwithhippos.unchained.utilities.postEvent
import com.tinder.StateMachine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
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
    private val preferences: SharedPreferences,
    private val authRepository: AuthenticationRepository,
    private val userRepository: UserRepository,
    private val variousApiRepository: VariousApiRepository,
    private val hostsRepository: HostsRepository,
    private val pluginRepository: PluginRepository,
    private val protoStore: ProtoStore,
    private val kodiDeviceRepository: KodiDeviceRepository,
    private val customDownloadRepository: CustomDownloadRepository
) : ViewModel() {

    val fsmAuthenticationState = MutableLiveData<Event<FSMAuthenticationState>?>()
    private val credentialsFlow = protoStore.credentialsFlow

    val externalLinkLiveData = MutableLiveData<Event<Uri>>()

    val downloadedFileLiveData = MutableLiveData<Event<Long>>()

    val notificationTorrentLiveData = MutableLiveData<Event<String>>()

    val listStateLiveData = MutableLiveData<Event<ListState>>()

    val connectivityLiveData = MutableLiveData<Boolean?>()
    // val currentNetworkLiveData = MutableLiveData<Network?>()

    val jumpTabLiveData = MutableLiveData<Event<String>>()

    // todo: use a better name to reflect the difference between this and externalLinkLiveData
    val linkLiveData = MutableLiveData<Event<String>?>()

    val messageLiveData = MutableLiveData<Event<MainActivityMessage>?>()

    private var refreshJob: Job? = null

    private val authStateMachine: StateMachine<FSMAuthenticationState, FSMAuthenticationEvent, FSMAuthenticationSideEffect> =
        StateMachine.create {

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
                        FSMAuthenticationSideEffect.PostAuthenticatedOpen
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
                        FSMAuthenticationSideEffect.PostAuthenticatedPrivate
                    )
                }
                on<FSMAuthenticationEvent.OnNotWorking> {
                    transitionTo(
                        FSMAuthenticationState.StartNewLogin,
                        FSMAuthenticationSideEffect.ResetAuthentication
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
                on<FSMAuthenticationEvent.OnUserConfirmationExpired> {
                    transitionTo(
                        FSMAuthenticationState.StartNewLogin,
                        FSMAuthenticationSideEffect.PostNewLogin
                    )
                }
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
                on<FSMAuthenticationEvent.OnAuthenticationError> {
                    transitionTo(
                        FSMAuthenticationState.WaitingUserAction(null),
                        FSMAuthenticationSideEffect.PostActionNeeded
                    )
                }
                on<FSMAuthenticationEvent.OnLogout> {
                    transitionTo(
                        FSMAuthenticationState.StartNewLogin,
                        FSMAuthenticationSideEffect.PostNewLogin
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
                on<FSMAuthenticationEvent.OnLogout> {
                    transitionTo(
                        FSMAuthenticationState.StartNewLogin,
                        FSMAuthenticationSideEffect.PostNewLogin
                    )
                }
                on<FSMAuthenticationEvent.OnAuthenticationError> {
                    transitionTo(
                        FSMAuthenticationState.WaitingUserAction(null),
                        FSMAuthenticationSideEffect.PostActionNeeded
                    )
                }
                on<FSMAuthenticationEvent.OnNotWorking> {
                    transitionTo(
                        FSMAuthenticationState.StartNewLogin,
                        FSMAuthenticationSideEffect.ResetAuthentication
                    )
                }
            }

            state<FSMAuthenticationState.AuthenticatedPrivateToken> {
                on<FSMAuthenticationEvent.OnLogout> {
                    transitionTo(
                        FSMAuthenticationState.StartNewLogin,
                        FSMAuthenticationSideEffect.PostNewLogin
                    )
                }
                on<FSMAuthenticationEvent.OnAuthenticationError> {
                    transitionTo(
                        FSMAuthenticationState.WaitingUserAction(null),
                        FSMAuthenticationSideEffect.PostActionNeeded
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
                            Event(
                                FSMAuthenticationState.CheckCredentials
                            )
                        )
                    }
                    FSMAuthenticationSideEffect.PostNewLogin -> {
                        fsmAuthenticationState.postValue(
                            Event(
                                FSMAuthenticationState.StartNewLogin
                            )
                        )

                    }
                    FSMAuthenticationSideEffect.PostAuthenticatedOpen -> {
                        fsmAuthenticationState.postValue(
                            Event(
                                FSMAuthenticationState.AuthenticatedOpenToken
                            )
                        )
                    }
                    FSMAuthenticationSideEffect.PostRefreshingToken -> {
                        fsmAuthenticationState.postValue(
                            Event(
                                FSMAuthenticationState.RefreshingOpenToken
                            )
                        )
                    }
                    FSMAuthenticationSideEffect.PostAuthenticatedPrivate -> {
                        fsmAuthenticationState.postValue(
                            Event(
                                FSMAuthenticationState.AuthenticatedPrivateToken
                            )
                        )
                    }
                    FSMAuthenticationSideEffect.PostWaitToken -> {
                        fsmAuthenticationState.postValue(
                            Event(
                                FSMAuthenticationState.WaitingToken
                            )
                        )
                    }
                    FSMAuthenticationSideEffect.PostWaitUserConfirmation -> {
                        fsmAuthenticationState.postValue(
                            Event(
                                FSMAuthenticationState.WaitingUserConfirmation
                            )
                        )
                    }
                    FSMAuthenticationSideEffect.PostActionNeeded -> {
                        when (it.event) {
                            is FSMAuthenticationEvent.OnUserActionNeeded -> {
                                val action =
                                    (it.event as FSMAuthenticationEvent.OnUserActionNeeded).action

                                fsmAuthenticationState.postValue(
                                    Event(
                                        FSMAuthenticationState.WaitingUserAction(
                                            action
                                        )
                                    )
                                )
                            }
                            is FSMAuthenticationEvent.OnAuthenticationError -> {
                                fsmAuthenticationState.postValue(
                                    Event(
                                        FSMAuthenticationState.WaitingUserAction(null)
                                    )
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
                                Event(
                                    FSMAuthenticationState.StartNewLogin
                                )
                            )
                        }
                    }
                }
            }
        }

    fun checkCredentials() {
        viewModelScope.launch {
            // todo: how to do this
            val credentials = protoStore.credentialsFlow.first { it.accessToken.isNotBlank() }
            val userResult = userRepository.getUserOrError(credentials.accessToken)
            parseUserResult(userResult, credentials.refreshToken == PRIVATE_TOKEN)
        }
    }

    private fun parseUserResult(
        user: EitherResult<UnchainedNetworkException, User>,
        isPrivateToken: Boolean
    ) {
        when (user) {
            is EitherResult.Success -> {

                if (isPrivateToken) {
                    transitionAuthenticationMachine(FSMAuthenticationEvent.OnWorkingPrivateToken)
                } else {
                    // todo: check if always posting Expired token makes sense. The idea is that this way I can manage the expiration time better
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

    fun logout() {
        viewModelScope.launch {
            protoStore.deleteCredentials()
            recheckAuthenticationStatus()
        }
    }

    fun recheckAuthenticationStatus() {
        when (getAuthenticationMachineState()) {
            FSMAuthenticationState.AuthenticatedOpenToken, FSMAuthenticationState.AuthenticatedPrivateToken, FSMAuthenticationState.RefreshingOpenToken -> {
                transitionAuthenticationMachine(FSMAuthenticationEvent.OnLogout)
            }
            else -> {
                Timber.e("Asked for logout while in a wrong state: ${getAuthenticationMachineState()}")
            }
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
            val credentials = protoStore.getCredentials()
            if (!credentials.refreshToken.isNullOrBlank() && credentials.refreshToken != PRIVATE_TOKEN) {
                // todo: add EitherResult to check for errors and retry eventually
                when (val newToken = authRepository.refreshTokenWithError(credentials)) {
                    is EitherResult.Success -> {
                        protoStore.setCredentials(
                            deviceCode = credentials.deviceCode,
                            clientId = credentials.clientId,
                            clientSecret = credentials.clientSecret,
                            accessToken = newToken.success.accessToken,
                            refreshToken = newToken.success.refreshToken
                        )

                        // program the refresh of the token
                        programTokenRefresh(newToken.success.expiresIn)

                        if (getAuthenticationMachineState() is FSMAuthenticationState.RefreshingOpenToken)
                            transitionAuthenticationMachine(FSMAuthenticationEvent.OnRefreshed)
                        // else I'm just refreshing before it expires
                        // todo: just set it to refreshing at the start of this function
                    }
                    is EitherResult.Failure -> {
                        onParseCallFailure(
                            newToken.failure,
                            credentials.deviceCode == PRIVATE_TOKEN
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
                                transitionAuthenticationMachine(FSMAuthenticationEvent.OnExpiredOpenToken)
                            }
                            FSMAuthenticationState.CheckCredentials -> {
                                if (isPrivateToken)
                                    transitionAuthenticationMachine(FSMAuthenticationEvent.OnNotWorking)
                                else
                                    transitionAuthenticationMachine(FSMAuthenticationEvent.OnExpiredOpenToken)
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
                        // 9 is permission denied which should mean the token is not valid at all and should be discarded
                        transitionAuthenticationMachine(FSMAuthenticationEvent.OnNotWorking)
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
                        messageLiveData.postValue(Event(MainActivityMessage.StringID(R.string.plugin_install_installed)))
                    else
                        messageLiveData.postValue(Event(MainActivityMessage.StringID(R.string.plugin_install_not_installed)))
                }
            }
        }
    }

    // todo: move this stuff to a shared navigationViewModel
    fun setListState(state: ListState) {
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
                link.isMagnet() -> linkLiveData.postValue(Event(link))
                link.isTorrent() -> linkLiveData.postValue(Event(link))
                link.endsWith(TYPE_UNCHAINED, ignoreCase = true) -> {
                    // only accept github links for now
                    val newLink = convertGithubToRaw(link)
                    if (newLink != null)
                        linkLiveData.postValue(Event(newLink))
                    else
                        messageLiveData.postValue(Event(MainActivityMessage.StringID(R.string.invalid_url)))
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
                        messageLiveData.postValue(Event(MainActivityMessage.StringID(R.string.host_match_not_found)))
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
                            messageLiveData.postValue(Event(MainActivityMessage.StringID(R.string.plugin_install_installed)))
                        else
                            messageLiveData.postValue(Event(MainActivityMessage.StringID(R.string.plugin_install_not_installed)))
                    } else {
                        // is it the same plugin?
                        if (existingPlugin.name == tempPlugin.name) {
                            // is the version newer?
                            if (existingPlugin.version < tempPlugin.version) {
                                file.delete()
                                val installed = pluginRepository.addExternalPlugin(context, data)
                                if (installed)
                                    messageLiveData.postValue(Event(MainActivityMessage.StringID(R.string.plugin_install_installed)))
                                else
                                    messageLiveData.postValue(Event(MainActivityMessage.StringID(R.string.plugin_install_not_installed)))
                            } else {
                                // installed plugin is newer
                                messageLiveData.postValue(Event(MainActivityMessage.StringID(R.string.plugin_install_error_newer)))
                            }
                        } else {
                            // same file name for different plugins
                            val installed = pluginRepository.addExternalPlugin(
                                context, data,
                                "_$filename"
                            )
                            if (installed)
                                messageLiveData.postValue(Event(MainActivityMessage.StringID(R.string.plugin_install_installed)))
                            else
                                messageLiveData.postValue(Event(MainActivityMessage.StringID(R.string.plugin_install_not_installed)))
                        }
                    }
                } else {
                    val installed = pluginRepository.addExternalPlugin(context, data)
                    if (installed)
                        messageLiveData.postValue(Event(MainActivityMessage.StringID(R.string.plugin_install_installed)))
                    else
                        messageLiveData.postValue(Event(MainActivityMessage.StringID(R.string.plugin_install_not_installed)))
                }
            } else {
                messageLiveData.postValue(Event(MainActivityMessage.StringID(R.string.plugin_install_not_installed)))
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

    fun updateCredentials(
        deviceCode: String? = null,
        clientId: String? = null,
        clientSecret: String? = null,
        accessToken: String? = null,
        refreshToken: String? = null
    ) {
        viewModelScope.launch {
            protoStore.updateCredentials(
                deviceCode,
                clientId,
                clientSecret,
                accessToken,
                refreshToken
            )
        }
    }

    fun updateCredentialsDeviceCode(deviceCode: String) {
        viewModelScope.launch {
            protoStore.updateDeviceCode(deviceCode)
        }
    }

    fun updateCredentialsClientId(clientId: String) {
        viewModelScope.launch {
            protoStore.updateClientId(clientId)
        }
    }

    fun updateCredentialsClientSecret(clientSecret: String) {
        viewModelScope.launch {
            protoStore.updateClientSecret(clientSecret)
        }
    }

    fun updateCredentialsAccessToken(accessToken: String) {
        viewModelScope.launch {
            protoStore.updateAccessToken(accessToken)
        }
    }

    fun updateCredentialsRefreshToken(refreshToken: String) {
        viewModelScope.launch {
            protoStore.updateRefreshToken(refreshToken)
        }
    }

    suspend fun getCredentials(): Credentials.CurrentCredential {
        // todo: find a better way to get the first valid value
        val credentials = credentialsFlow.firstOrNull {
            it.clientId.isNotBlank() || it.refreshToken.isNotBlank() || it.deviceCode.isNotBlank() || it.accessToken.isNotBlank() || it.clientSecret.isNotBlank()
        }
        if (credentials == null)
            return protoStore.getCredentials()
        else
            return credentials
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
            FSMAuthenticationState.AuthenticatedPrivateToken, FSMAuthenticationState.AuthenticatedOpenToken -> CurrentFSMAuthentication.Authenticated
            FSMAuthenticationState.CheckCredentials, FSMAuthenticationState.RefreshingOpenToken -> CurrentFSMAuthentication.Waiting
            else -> CurrentFSMAuthentication.Unauthenticated
        }

    fun transitionAuthenticationMachine(event: FSMAuthenticationEvent) {
        authStateMachine.transition(event)
    }

    /**
     * Jump to the main screen set in the settings
     */
    fun goToStartUpScreen() {
        jumpTabLiveData.postEvent(preferences.getString("main_screen", "user") ?: "user")
    }

    /**
     * Loads the old saved kodi preference into the new db one and then deletes it
     */
    fun updateOldKodiPreferences() {

        val address: String? = preferences.getString("kodi_ip_address", null)
        val port: Int? = preferences.getString("kodi_port", null)?.toIntOrNull()

        if (!address.isNullOrBlank() && port != null) {
            val user: String? = preferences.getString("kodi_username", null)
            val password: String? = preferences.getString("kodi_password", null)

            viewModelScope.launch {
                kodiDeviceRepository.add(
                    KodiDevice(
                        "Imported Kodi Device",
                        address,
                        port,
                        user,
                        password,
                        true
                    )
                )

                with(preferences.edit()) {
                    remove("kodi_ip_address")
                    remove("kodi_port")
                    remove("kodi_username")
                    remove("kodi_password")
                    apply()
                }
            }
        }
    }

    suspend fun downloadFileToCache(
        link: String,
        fileName: String,
        cacheDir: File,
        suffix: String? = null
    ) = customDownloadRepository.downloadToCache(link, fileName, cacheDir, suffix).asLiveData()

    private suspend fun installPluginsPack(cacheDir: File, pluginsDir: File) {
        val pluginsFolder = File(cacheDir, PLUGINS_PACK_FOLDER)
        var installedPlugins = 0
        if (pluginsFolder.exists() && pluginsFolder.isDirectory) {
            pluginsFolder.walk().forEach { pluginFile ->
                if (pluginFile.path.endsWith(".unchained")) {
                    val plugin = pluginRepository.readPluginFile(pluginFile)
                    if (plugin != null) {
                        val installed = pluginRepository.addExternalPlugin(pluginsDir, pluginFile)
                        if (installed) {
                            Timber.d("Installed plugin ${pluginFile.name}")
                            installedPlugins++
                        } else
                            Timber.d("Error installing plugin ${pluginFile.name}")
                    } else {
                        Timber.d("Error parsing plugin ${pluginFile.name}")
                    }
                } else {
                    // this also gets triggered by the plugins own folder which is traversed by walk
                    Timber.d("Skipping unrecognized file into the plugin folder: ${pluginFile.name}")
                }
            }
        }
        messageLiveData.postValue(Event(MainActivityMessage.InstalledPlugins(installedPlugins)))
    }

    fun processPluginsPack(cacheDir: File, pluginsDir: File, fileName: String) {
        try {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    val cacheFile = File(cacheDir, fileName)
                    UnzipUtils.unzip(cacheFile, File(cacheDir, PLUGINS_PACK_FOLDER))
                    Timber.d("Zip pack extracted")
                    installPluginsPack(cacheDir, pluginsDir)
                }
            }
        } catch (exception: Exception) {
            when (exception) {
                is IOException -> {
                    Timber.e("Plugins pack IOException error with the file: ${exception.message}")
                }
                is java.io.FileNotFoundException -> {
                    Timber.e("Plugins pack: file not found: ${exception.message}")
                }
                else -> {
                    Timber.e("Plugins pack: Other error getting the file: ${exception.message}")
                }
            }
        }

    }

    fun clearCache(cacheDir: File) {
        cacheDir.listFiles()?.forEach {
            if (it.name != "image_cache")
                it.deleteRecursively()
        }
    }

    companion object {
        const val KEY_TORRENT_DOWNLOAD_ID = "torrent_download_id_key"
        const val KEY_PLUGIN_DOWNLOAD_ID = "plugin_download_id_key"
        const val KEY_LAST_BACK_PRESS = "last_back_press_key"
        const val KEY_REFRESHING_TOKEN = "refreshing_token_key"
        const val KEY_FSM_AUTH_STATE = "fsm_auth_state_key"
    }
}

sealed class MainActivityMessage {
    data class StringID(val id: Int): MainActivityMessage()
    data class InstalledPlugins(val number: Int): MainActivityMessage()
}