package com.github.livingwithhippos.unchained.authentication.view

import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.authentication.viewmodel.AuthenticationViewModel
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.AuthenticationState
import com.github.livingwithhippos.unchained.databinding.FragmentAuthenticationBinding
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.extension.copyToClipboard
import com.github.livingwithhippos.unchained.utilities.extension.getClipboardText
import com.github.livingwithhippos.unchained.utilities.extension.getThemeColor
import com.github.livingwithhippos.unchained.utilities.extension.openExternalWebPage
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint

/**
 * A simple [UnchainedFragment] subclass.
 * It is capable of authenticating a user via either the private API key or the OAUTH system
 */
@AndroidEntryPoint
class AuthenticationFragment : UnchainedFragment(), ButtonListener {

    private val viewModel: AuthenticationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val authBinding = FragmentAuthenticationBinding.inflate(inflater, container, false)
        // todo: add loading gif

        authBinding.listener = this

        authBinding.loginMessageDirect = getLoginMessage(LOGIN_TYPE_DIRECT)
        authBinding.loginMessageIndirect = getLoginMessage(LOGIN_TYPE_INDIRECT)

        // open source client id observers:

        // start checking for the auth link
        viewModel.authLiveData.observe(
            viewLifecycleOwner,
            EventObserver { auth ->
                if (auth != null) {
                    authBinding.auth = auth
                    viewModel.fetchSecrets(auth.deviceCode, auth.expiresIn)
                }
            }
        )

        // start checking for user confirmation
        viewModel.secretLiveData.observe(
            viewLifecycleOwner,
            EventObserver { secrets ->
                authBinding.secrets = secrets
                viewModel.authLiveData.value?.peekContent()?.deviceCode?.let { device ->
                    viewModel.fetchToken(secrets.clientId, device, secrets.clientSecret)
                }
            }
        )

        // start checking for the authentication token
        viewModel.tokenLiveData.observe(
            viewLifecycleOwner,
            EventObserver { token ->
                authBinding.token = token
                // pass the value to be checked and eventually saved
                viewModel.checkAndSaveToken(token = token)
            }
        )

        // a user is retrieved when a working token is used
        viewModel.userLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                it?.let { user ->
                    if (user.premium > 0)
                        activityViewModel.setAuthenticated()
                    else
                        activityViewModel.setAuthenticatedNoPremium()
                }
            }
        )

        // monitor the shared authentication state, if authenticated switch to the user fragment
        activityViewModel.authenticationState.observe(
            viewLifecycleOwner,
            {
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
            }
        )

        // get the authentication link to start the process
        viewModel.fetchAuthenticationInfo()

        return authBinding.root
    }

    private fun getLoginMessage(type: Int): SpannableStringBuilder {
        val sb = SpannableStringBuilder()

        sb.append(getString(R.string.please_visit))

        val link = SpannableString(getString(R.string.this_link))
        link.setSpan(UnderlineSpan(), 0, link.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val colorSecondary = requireContext().getThemeColor(R.attr.colorSecondary)
        link.setSpan(
            ForegroundColorSpan(colorSecondary),
            0,
            link.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        sb.append(link)

        sb.append(getString(R.string.to_authenticate))

        if (type == LOGIN_TYPE_INDIRECT)
            sb.append(getString(R.string.using_code))

        return sb
    }

    override fun onCopyClick(text: String) {
        copyToClipboard("real-debrid authorization code", text)
        context?.showToast(R.string.code_copied)
    }

    override fun onSaveCodeClick(codeInputField: TextInputEditText) {
        val token: String = codeInputField.text.toString().trim()
        // mine is 52 characters
        if (token.length < 40)
            context?.showToast(R.string.invalid_token)
        else
        // pass the value to be checked and eventually saved
            viewModel.checkAndSaveToken(privateKey = token)
    }

    override fun onPasteCodeClick(codeInputField: TextInputEditText) {
        val pasteText = getClipboardText()
        codeInputField.setText(pasteText, TextView.BufferType.EDITABLE)
    }

    override fun onOpenLinkClick(url: String) {
        openExternalWebPage(url)
    }

    companion object {
        const val LOGIN_TYPE_DIRECT = 0
        const val LOGIN_TYPE_INDIRECT = 1
    }
}

interface ButtonListener {
    fun onCopyClick(text: String)
    fun onSaveCodeClick(codeInputField: TextInputEditText)
    fun onPasteCodeClick(codeInputField: TextInputEditText)
    fun onOpenLinkClick(url: String)
}
