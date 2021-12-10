package com.github.livingwithhippos.unchained.statemachine.authentication

import com.github.livingwithhippos.unchained.data.model.UserAction

sealed class FSMAuthenticationState {
    object Start : FSMAuthenticationState()
    object CheckCredentials : FSMAuthenticationState()
    data class WaitingUserAction(val action: UserAction?) : FSMAuthenticationState()
    object StartNewLogin : FSMAuthenticationState()
    object WaitingUserConfirmation : FSMAuthenticationState()
    object WaitingToken : FSMAuthenticationState()
    object AuthenticatedOpenToken : FSMAuthenticationState()
    object AuthenticatedPrivateToken : FSMAuthenticationState()
    object RefreshingOpenToken : FSMAuthenticationState()
}

sealed class FSMAuthenticationEvent {
    object OnAvailableCredentials : FSMAuthenticationEvent()
    object OnMissingCredentials : FSMAuthenticationEvent()
    object OnPrivateToken : FSMAuthenticationEvent()
    object OnNotWorking : FSMAuthenticationEvent()
    object OnAuthLoaded : FSMAuthenticationEvent()
    data class OnUserActionNeeded(val action: UserAction) : FSMAuthenticationEvent()
    object OnUserActionRetry : FSMAuthenticationEvent()
    object OnUserActionReset : FSMAuthenticationEvent()
    object OnUserConfirmationLoaded : FSMAuthenticationEvent()
    object OnUserConfirmationMissing : FSMAuthenticationEvent()
    object OnUserConfirmationExpired : FSMAuthenticationEvent()
    object OnOpenTokenLoaded : FSMAuthenticationEvent()
    object OnWorkingPrivateToken : FSMAuthenticationEvent()
    object OnWorkingOpenToken : FSMAuthenticationEvent()
    object OnExpiredOpenToken : FSMAuthenticationEvent()
    object OnRefreshed : FSMAuthenticationEvent()
    object OnLogout : FSMAuthenticationEvent()
    object OnAuthenticationError : FSMAuthenticationEvent()
}

sealed class FSMAuthenticationSideEffect {
    object CheckingCredentials : FSMAuthenticationSideEffect()
    object PostActionNeeded : FSMAuthenticationSideEffect()
    object PostNewLogin : FSMAuthenticationSideEffect()
    object ResetAuthentication : FSMAuthenticationSideEffect()
    object PostWaitUserConfirmation : FSMAuthenticationSideEffect()
    object PostWaitToken : FSMAuthenticationSideEffect()
    object PostAuthenticatedOpen : FSMAuthenticationSideEffect()
    object PostAuthenticatedPrivate : FSMAuthenticationSideEffect()
    object PostRefreshingToken : FSMAuthenticationSideEffect()
}

// support class
sealed class CurrentFSMAuthentication {
    // auth is ok
    object Authenticated : CurrentFSMAuthentication()

    // auth may become ok
    object Waiting : CurrentFSMAuthentication()

    // auth is not ok
    object Unauthenticated : CurrentFSMAuthentication()
}
