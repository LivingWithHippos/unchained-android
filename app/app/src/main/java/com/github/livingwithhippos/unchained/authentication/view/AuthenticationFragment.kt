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
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.authentication.viewmodel.AuthenticationViewModel
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.AuthenticationStatus
import com.github.livingwithhippos.unchained.databinding.FragmentAuthenticationBinding
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.PRIVATE_TOKEN
import com.github.livingwithhippos.unchained.utilities.extension.copyToClipboard
import com.github.livingwithhippos.unchained.utilities.extension.getClipboardText
import com.github.livingwithhippos.unchained.utilities.extension.getThemeColor
import com.github.livingwithhippos.unchained.utilities.extension.observeOnce
import com.github.livingwithhippos.unchained.utilities.extension.openExternalWebPage
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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

        authBinding.listener = this

        authBinding.loginMessageDirect = getLoginMessage(LOGIN_TYPE_DIRECT)
        authBinding.loginMessageIndirect = getLoginMessage(LOGIN_TYPE_INDIRECT)

        // start checking for the auth link
        viewModel.authLiveData.observe(
            viewLifecycleOwner,
            EventObserver { auth ->
                if (auth != null) {
                    authBinding.auth = auth
                    lifecycleScope.launch {
                        // update the currently saved credentials
                        activityViewModel.updateCredentials(
                            deviceCode = auth.deviceCode
                        )
                        // start the next auth step
                        viewModel.fetchSecrets(auth.deviceCode, auth.expiresIn)
                    }
                }
            }
        )

        // start checking for user confirmation
        viewModel.secretLiveData.observe(
            viewLifecycleOwner,
            EventObserver { secrets ->
                authBinding.secrets = secrets
                lifecycleScope.launch {
                    // update the currently saved credentials
                    activityViewModel.updateCredentials(
                        clientId = secrets.clientId,
                        clientSecret = secrets.clientSecret,
                    )
                    // start the next auth step
                    activityViewModel.getCurrentCredentials().asLiveData()
                        .observeOnce(
                            viewLifecycleOwner,
                            {
                                viewModel.fetchToken(
                                    secrets.clientId,
                                    it.deviceCode,
                                    secrets.clientSecret
                                )
                            }
                        )
                }
            }
        )

        // start checking for the authentication token
        viewModel.tokenLiveData.observe(
            viewLifecycleOwner,
            EventObserver { token ->
                authBinding.token = token
                // pass the value to be checked and eventually saved
                if (token != null) {
                    lifecycleScope.launch {
                        // update the current credentials
                        activityViewModel.updateCredentials(
                            accessToken = token.accessToken,
                            refreshToken = token.refreshToken
                        )
                    }
                    activityViewModel.getCurrentCredentials().asLiveData()
                        .observeOnce(
                            viewLifecycleOwner,
                            {
                                lifecycleScope.launch {
                                    // check the current credentials
                                    activityViewModel.setupAuthenticationStatus(it)
                                }
                            }
                        )
                }
            }
        )

        activityViewModel.newAuthenticationState.observe(
            viewLifecycleOwner,
            {

                if (it.peekContent() is AuthenticationStatus.Authenticated || it.peekContent() is AuthenticationStatus.AuthenticatedNoPremium) {
                    viewModel.setAuthState(true)
                    val action =
                        AuthenticationFragmentDirections.actionAuthenticationToUser()
                    findNavController().navigate(action)
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

    override fun onSaveCodeClick(codeInputField: TextInputEditText) {
        val token: String = codeInputField.text.toString().trim()
        // mine is 52 characters
        if (token.length < 40)
            context?.showToast(R.string.invalid_token)
        else
        // pass the value to be checked and eventually saved
            lifecycleScope.launch {
                activityViewModel.updateCredentials(
                    accessToken = token,
                    clientId = PRIVATE_TOKEN,
                    clientSecret = PRIVATE_TOKEN,
                    deviceCode = PRIVATE_TOKEN,
                    refreshToken = PRIVATE_TOKEN
                )
                activityViewModel.setupAuthenticationStatus()
            }
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
    fun onSaveCodeClick(codeInputField: TextInputEditText)
    fun onPasteCodeClick(codeInputField: TextInputEditText)
    fun onOpenLinkClick(url: String)
}
