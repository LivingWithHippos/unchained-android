package com.github.livingwithhippos.unchained.authentication.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.authentication.viewmodel.AuthenticationViewModel
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentAuthenticationBinding
import com.github.livingwithhippos.unchained.utilities.copyToClipboard
import com.github.livingwithhippos.unchained.utilities.showToast
import dagger.hilt.android.AndroidEntryPoint


/**
 * A simple [Fragment] subclass.
 * Use the [AuthenticationFragment.newInstance] factory method to
 * create an instance of this fragment.
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

        viewModel.fetchAuthenticationInfo()
        observeAuthentication(authBinding)

        return authBinding.root
    }

    private fun observeAuthentication(binding: FragmentAuthenticationBinding) {
        viewModel.authLiveData.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { auth ->
                binding.auth = auth
                observeSecrets(binding, auth.deviceCode)
            }
        })
    }

    private fun observeSecrets(binding: FragmentAuthenticationBinding, deviceCode: String) {
        // start checking for user confirmation
        viewModel.fetchSecrets(deviceCode)
        viewModel.secretLiveData.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { secrets ->
                binding.secrets = secrets
                observeToken(binding, secrets.clientId, deviceCode, secrets.clientSecret)
            }
        })
    }

    private fun observeToken(
        binding: FragmentAuthenticationBinding,
        clientId: String,
        deviceCode: String,
        clientSecret: String
    ) {

        // start checking for user confirmation
        viewModel.fetchToken(clientId, deviceCode, clientSecret)
        viewModel.tokenLiveData.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { token ->
                activityViewModel.setAuthenticated()
                binding.token = token
                val action =
                    AuthenticationFragmentDirections.actionAuthenticationToUser(token.accessToken)
                findNavController().navigate(action)
            }
        })
    }

    override fun onCopyClick(text: String) {
        copyToClipboard("real-debrid authorization code", text)
        showToast(R.string.code_copied)
    }

    override fun onInsertTokenClick(etToken: EditText) {
        val token = etToken.text.toString().trim()
        // mine is 52 characters
        if (token.length < 50)
            showToast(R.string.invalid_token)
        else {
            // pass the value to be checked and eventually saved
            viewModel.checkAndSaveToken(token)
            // check if we were able to load the user with the custom token
            viewModel.userLiveData.observe(viewLifecycleOwner, Observer {
                // wrong token or connectivity issues
                if (it == null)
                    showToast(R.string.invalid_token)
                else {
                    activityViewModel.setAuthenticated()
                    // go to user screen
                    // todo: the user call will be repeated, avoid if possible (shared viewmodel?)
                    val action =
                        AuthenticationFragmentDirections.actionAuthenticationToUser(token)
                    findNavController().navigate(action)
                }
            })
        }
    }
}

interface ButtonListener {
    fun onCopyClick(text: String)
    fun onInsertTokenClick(etToken: EditText)
}