package com.github.livingwithhippos.unchained.authentication.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.authentication.viewmodel.AuthenticationViewModel
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.AuthenticationState
import com.github.livingwithhippos.unchained.databinding.FragmentAuthenticationBinding
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import com.github.livingwithhippos.unchained.utilities.extension.copyToClipboard
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint


/**
 * A simple [UnchainedFragment] subclass.
 * It is capable of authenticating a user via either the private API key or the OAUTH system
 */
@AndroidEntryPoint
class AuthenticationFragment : UnchainedFragment(), ButtonListener {

    private val viewModel: AuthenticationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val authBinding = FragmentAuthenticationBinding.inflate(inflater, container, false)
        //todo: add loading gif

        authBinding.listener = this

        //open source client id observers:

        // start checking for the auth link
        viewModel.authLiveData.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { auth ->
                authBinding.auth = auth
                viewModel.fetchSecrets(auth.deviceCode, auth.expiresIn)
            }
        })

        // start checking for user confirmation
        viewModel.secretLiveData.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { secrets ->
                authBinding.secrets = secrets
                viewModel.authLiveData.value?.peekContent()?.deviceCode?.let { device ->
                    viewModel.fetchToken(secrets.clientId, device, secrets.clientSecret)
                }

            }
        })

        // start checking for the authentication token
        viewModel.tokenLiveData.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { token ->
                authBinding.token = token
                // pass the value to be checked and eventually saved
                viewModel.checkAndSaveToken(token = token)
            }
        })

        // a user is retrieved when a working token is used
        viewModel.userLiveData.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let {user->
                if (user.premium > 0)
                    activityViewModel.setAuthenticated()
                else
                    activityViewModel.setAuthenticatedNoPremium()
            }
        })

        // monitor the shared authentication state, if authenticated switch to the user fragment
        activityViewModel.authenticationState.observe(viewLifecycleOwner, {
            when (it.peekContent()) {
                AuthenticationState.AUTHENTICATED -> {
                    val action =
                        AuthenticationFragmentDirections.actionAuthenticationToUser()
                    findNavController().navigate(action)
                    // these will stop the api calls to the secret endpoint in the viewModel
                    viewModel.setAuthState(AuthenticationState.AUTHENTICATED)
                }
                AuthenticationState.UNAUTHENTICATED -> viewModel.setAuthState(AuthenticationState.UNAUTHENTICATED)
                AuthenticationState.BAD_TOKEN -> viewModel.setAuthState(AuthenticationState.BAD_TOKEN)
                AuthenticationState.ACCOUNT_LOCKED -> viewModel.setAuthState(AuthenticationState.ACCOUNT_LOCKED)
                AuthenticationState.AUTHENTICATED_NO_PREMIUM -> {
                    val action =
                        AuthenticationFragmentDirections.actionAuthenticationToUser()
                    findNavController().navigate(action)
                    // these will stop the api calls to the secret endpoint in the viewModel
                    viewModel.setAuthState(AuthenticationState.AUTHENTICATED_NO_PREMIUM)
                }
            }
        })

        // get the authentication link to start the process
        viewModel.fetchAuthenticationInfo()

        return authBinding.root
    }

    override fun onCopyClick(text: String) {
        copyToClipboard("real-debrid authorization code", text)
        context?.showToast(R.string.code_copied)
    }

    override fun onInsertTokenClick(etToken: EditText) {
        //todo: rename all these references to privateKey or something like that to avoid confusion with token from open source client id
        val token = etToken.text.toString().trim()
        // mine is 52 characters
        if (token.length < 40)
            context?.showToast(R.string.invalid_token)
        else
        // pass the value to be checked and eventually saved
            viewModel.checkAndSaveToken(privateKey = token)
    }

}

interface ButtonListener {
    fun onCopyClick(text: String)
    fun onInsertTokenClick(etToken: EditText)
}