package com.github.livingwithhippos.unchained.authentication.view

import android.content.SharedPreferences
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.authentication.viewmodel.AuthenticationViewModel
import com.github.livingwithhippos.unchained.authentication.viewmodel.SecretResult
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentAuthenticationBinding
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationEvent
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationState
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.DebridProvider
import com.github.livingwithhippos.unchained.utilities.PRIVATE_TOKEN
import com.github.livingwithhippos.unchained.utilities.getDebridProvider
import com.github.livingwithhippos.unchained.utilities.setDebridProvider
import com.github.livingwithhippos.unchained.utilities.extension.copyToClipboard
import com.github.livingwithhippos.unchained.utilities.extension.getClipboardText
import com.github.livingwithhippos.unchained.utilities.extension.getThemeColor
import com.github.livingwithhippos.unchained.utilities.extension.hideKeyboard
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * A simple [UnchainedFragment] subclass. It is capable of authenticating a user via either the
 * private API key or the OAUTH system
 */
@AndroidEntryPoint
class AuthenticationFragment : UnchainedFragment() {

    @Inject lateinit var preferences: SharedPreferences

    private val viewModel: AuthenticationViewModel by viewModels()
    private var _binding: FragmentAuthenticationBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        _binding = FragmentAuthenticationBinding.inflate(inflater, container, false)
        renderProviderUi(preferences.getDebridProvider())

        binding.bServiceRealDebrid.setOnClickListener {
            onProviderSelected(DebridProvider.RealDebrid)
        }
        binding.bServiceAllDebrid.setOnClickListener {
            onProviderSelected(DebridProvider.AllDebrid)
        }

        binding.bPastePrivateCode.setOnClickListener {
            val pasteText = getClipboardText()
            binding.tiPrivateCode.setText(pasteText, TextView.BufferType.EDITABLE)
            binding.tiPrivateCode.hideKeyboard()
        }

        binding.bCopyLink.setOnClickListener {
            copyToClipboard(
                getString(R.string.code_copied),
                binding.tvUserCodeValue.text.toString(),
            )
        }

        // todo: check if needed
        // binding.loginMessageDirect = getLoginMessage(LOGIN_TYPE_DIRECT)
        // binding.loginMessageIndirect = getLoginMessage(LOGIN_TYPE_INDIRECT)

        binding.tiPrivateCode.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                v.hideKeyboard()
            }
        }

        binding.bInsertPrivate.setOnClickListener { onSaveCodeClick(binding.tiPrivateCode) }

        activityViewModel.fsmAuthenticationState.observe(viewLifecycleOwner) {
            if (it != null) {
                when (it.peekContent()) {
                    FSMAuthenticationState.AuthenticatedOpenToken -> {
                        val action = AuthenticationFragmentDirections.actionAuthenticationToUser()
                        findNavController().navigate(action)
                    }

                    FSMAuthenticationState.AuthenticatedPrivateToken -> {
                        val action = AuthenticationFragmentDirections.actionAuthenticationToUser()
                        findNavController().navigate(action)
                    }

                    FSMAuthenticationState.StartNewLogin -> {
                        // reset the current data
                        // token == null
                        binding.cbToken.isChecked = false
                        binding.cbToken.text = getString(R.string.waiting_token)
                        // auth == null
                        binding.tvAuthenticationLink.text = ""
                        binding.tvAuthenticationLink.visibility = View.GONE
                        binding.cbLink.isChecked = false
                        binding.cbLink.text = getString(R.string.waiting_link)
                        // secrets == null
                        binding.tvLoginMessage.visibility = View.VISIBLE
                        binding.cbSecret.isChecked = false
                        binding.cbSecret.text = getString(R.string.waiting_user_auth)
                        binding.tvUserCodeValue.text = getString(R.string.copy_code)
                        binding.bCopyLink.isEnabled = false

                        // get the authentication link to start the process
                        viewModel.fetchAuthenticationInfo()
                    }

                    FSMAuthenticationState.WaitingUserConfirmation -> {
                        // start the next auth step
                        viewModel.fetchSecrets()
                    }

                    FSMAuthenticationState.WaitingToken -> {
                        viewModel.fetchToken()
                    }

                    FSMAuthenticationState.CheckCredentials,
                    FSMAuthenticationState.RefreshingOpenToken -> {
                        // managed by activity
                    }

                    is FSMAuthenticationState.WaitingUserAction -> {
                        // todo: depending on the action required show an error or restart the
                        // process
                    }

                    FSMAuthenticationState.Start -> {
                        // this shouldn't happen
                    }
                }
            }
        }

        // 1. start checking for the auth link
        viewModel.authLiveData.observe(viewLifecycleOwner) { event ->
            event?.peekContent()?.let { auth ->
                binding.tvAuthenticationLink.text = auth.verificationUrl
                binding.tvAuthenticationLink.visibility = View.VISIBLE
                binding.cbLink.isChecked = true
                binding.cbLink.text = getString(R.string.link_loaded)
                // let the user copy the user code to enter in the website
                binding.tvUserCodeValue.text = auth.userCode
                binding.bCopyLink.isEnabled = true
                // update the currently saved credentials
                activityViewModel.updateCredentialsDeviceCode(auth.deviceCode)
                viewModel.setupSecretLoop(auth.expiresIn)
                // transition state machine
                if (
                    activityViewModel.getAuthenticationMachineState()
                        is FSMAuthenticationState.StartNewLogin
                ) {
                    activityViewModel.transitionAuthenticationMachine(
                        FSMAuthenticationEvent.OnAuthLoaded
                    )
                }
            }
        }

        // 2. start checking for user confirmation
        viewModel.secretLiveData.observe(
            viewLifecycleOwner,
            EventObserver { secrets ->
                when (secrets) {
                    SecretResult.Empty -> {
                        // will launch another call, re-entering WaitingUserConfirmation
                        if (
                            activityViewModel.getAuthenticationMachineState()
                                is FSMAuthenticationState.WaitingUserConfirmation
                        )
                            activityViewModel.transitionAuthenticationMachine(
                                FSMAuthenticationEvent.OnUserConfirmationMissing
                            )
                    }

                    SecretResult.Expired -> {
                        // will restart the authentication process
                        activityViewModel.transitionAuthenticationMachine(
                            FSMAuthenticationEvent.OnUserConfirmationExpired
                        )
                    }

                    is SecretResult.Retrieved -> {
                        if (
                            activityViewModel.getAuthenticationMachineState()
                                is FSMAuthenticationState.WaitingUserConfirmation
                        ) {
                            binding.cbSecret.isChecked = true
                            binding.cbSecret.text = getString(R.string.obtained_user_auth)

                            lifecycleScope.launch {
                                // update the currently saved credentials
                                activityViewModel.updateCredentials(
                                    clientId = secrets.value.clientId,
                                    clientSecret = secrets.value.clientSecret,
                                )
                                // start the next auth step
                                activityViewModel.transitionAuthenticationMachine(
                                    FSMAuthenticationEvent.OnUserConfirmationLoaded
                                )
                            }
                        }
                    }

                    is SecretResult.ApiKeyRetrieved -> {
                        binding.cbSecret.isChecked = true
                        binding.cbSecret.text = getString(R.string.obtained_user_auth)
                        binding.cbToken.isChecked = true
                        binding.cbToken.text = getString(R.string.obtained_token)
                        activityViewModel.updateCredentials(
                            accessToken = secrets.value,
                            clientId = PRIVATE_TOKEN,
                            clientSecret = PRIVATE_TOKEN,
                            deviceCode = PRIVATE_TOKEN,
                            refreshToken = PRIVATE_TOKEN,
                        )
                        activityViewModel.transitionAuthenticationMachine(
                            FSMAuthenticationEvent.OnPrivateToken
                        )
                    }
                }
            },
        )

        // 3. start checking for the authentication token
        viewModel.tokenLiveData.observe(
            viewLifecycleOwner,
            EventObserver { token ->
                // pass the value to be checked and eventually saved
                if (token != null) {
                    binding.cbToken.isChecked = true
                    binding.cbToken.text = getString(R.string.obtained_token)
                    // update the current credentials
                    activityViewModel.updateCredentialsAccessToken(token.accessToken)
                    activityViewModel.updateCredentialsRefreshToken(token.refreshToken)
                    activityViewModel.transitionAuthenticationMachine(
                        FSMAuthenticationEvent.OnOpenTokenLoaded
                    )
                }
            },
        )

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getLoginMessage(type: Int): SpannableStringBuilder {
        val sb = SpannableStringBuilder()

        sb.append(getString(R.string.please_visit))

        val link = SpannableString(getString(R.string.this_link))
        link.setSpan(UnderlineSpan(), 0, link.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val colorSecondary =
            requireContext().getThemeColor(com.google.android.material.R.attr.colorSecondary)
        link.setSpan(
            ForegroundColorSpan(colorSecondary),
            0,
            link.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        sb.append(link)

        sb.append(getString(R.string.to_authenticate))

        if (type == LOGIN_TYPE_INDIRECT) sb.append(getString(R.string.using_code))

        return sb
    }

    fun onSaveCodeClick(codeInputField: TextInputEditText) {
        val token: String = codeInputField.text.toString().trim()
        val minimumLength =
            when (preferences.getDebridProvider()) {
                DebridProvider.RealDebrid -> 40
                DebridProvider.AllDebrid -> 16
            }
        if (token.length < minimumLength) context?.showToast(R.string.invalid_token)
        else {
            // pass the value to be checked and eventually saved
            activityViewModel.updateCredentials(
                accessToken = token,
                clientId = PRIVATE_TOKEN,
                clientSecret = PRIVATE_TOKEN,
                deviceCode = PRIVATE_TOKEN,
                refreshToken = PRIVATE_TOKEN,
            )
            activityViewModel.transitionAuthenticationMachine(FSMAuthenticationEvent.OnPrivateToken)
        }
    }

    private fun onProviderSelected(provider: DebridProvider) {
        if (preferences.getDebridProvider() == provider) return
        preferences.setDebridProvider(provider)
        renderProviderUi(provider)
        resetAuthenticationUi()
        viewModel.fetchAuthenticationInfo()
    }

    private fun renderProviderUi(provider: DebridProvider) {
        binding.serviceToggleGroup.check(
            when (provider) {
                DebridProvider.RealDebrid -> R.id.bServiceRealDebrid
                DebridProvider.AllDebrid -> R.id.bServiceAllDebrid
            }
        )
        binding.tvLoginMessage.text =
            when (provider) {
                DebridProvider.RealDebrid -> getLoginMessage(LOGIN_TYPE_INDIRECT)
                DebridProvider.AllDebrid -> getString(R.string.alldebrid_pin_message)
            }
        binding.tvUsePrivateToken.text =
            when (provider) {
                DebridProvider.RealDebrid -> getString(R.string.add_private_token_message)
                DebridProvider.AllDebrid -> getString(R.string.add_private_token_message_alldebrid)
            }
        binding.tfPrivateCode.hint =
            when (provider) {
                DebridProvider.RealDebrid -> getString(R.string.private_token)
                DebridProvider.AllDebrid -> getString(R.string.api_key)
            }
    }

    private fun resetAuthenticationUi() {
        binding.cbToken.isChecked = false
        binding.cbToken.text = getString(R.string.waiting_token)
        binding.tvAuthenticationLink.text = ""
        binding.tvAuthenticationLink.visibility = View.GONE
        binding.cbLink.isChecked = false
        binding.cbLink.text = getString(R.string.waiting_link)
        binding.cbSecret.isChecked = false
        binding.cbSecret.text = getString(R.string.waiting_user_auth)
        binding.tvUserCodeValue.text = getString(R.string.copy_code)
        binding.bCopyLink.isEnabled = false
        binding.tiPrivateCode.setText("")
    }

    companion object {
        const val LOGIN_TYPE_DIRECT = 0
        const val LOGIN_TYPE_INDIRECT = 1
    }
}
