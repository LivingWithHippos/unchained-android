package com.github.livingwithhippos.unchained.statemachine.authentication

import com.github.livingwithhippos.unchained.data.local.Credentials

sealed class FSMAuthenticationState {
    object GetCredentials : FSMAuthenticationState()
    object CheckCredentials : FSMAuthenticationState()
    object NewLogin : FSMAuthenticationState()
    object WaitUser : FSMAuthenticationState()
    object WaitToken : FSMAuthenticationState()
    object Ready : FSMAuthenticationState()
    object Refreshing : FSMAuthenticationState()
}

sealed class FSMAuthenticationEvent {
    data class OnCredentials(val credentials: Credentials?) : FSMAuthenticationEvent()
    object onPrivateToken : FSMAuthenticationEvent()
    object onNotWorking : FSMAuthenticationEvent()
    object onAuthLoaded : FSMAuthenticationEvent()
    object onUserConfirmationLoaded : FSMAuthenticationEvent()
    object onUserConfirmationMissing : FSMAuthenticationEvent()
    object onOpenTokenLoaded : FSMAuthenticationEvent()
    object onWorkingPrivateToken : FSMAuthenticationEvent()
    object onWorkingOpenToken : FSMAuthenticationEvent()
    object onExpired : FSMAuthenticationEvent()
    object onRefreshed : FSMAuthenticationEvent()
}

sealed class FSMAuthenticationSideEffect {
    object CheckingCredentials : FSMAuthenticationSideEffect()
}