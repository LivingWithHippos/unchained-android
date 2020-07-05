package com.github.livingwithhippos.unchained.authentication.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.authentication.viewmodel.AuthenticationViewModel
import com.github.livingwithhippos.unchained.databinding.FragmentAuthenticationBinding


/**
 * A simple [Fragment] subclass.
 * Use the [AuthenticationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AuthenticationFragment : Fragment(), ButtonListener {

    private val viewModel: AuthenticationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

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
            if (it != null) {
                binding.auth = it
                observeSecrets(it.deviceCode)
            }
        })
    }

    private fun observeSecrets(deviceCode: String){

        // start checking for user confirmation
        viewModel.fetchSecrets(deviceCode)
        viewModel.secretLiveData.observe(viewLifecycleOwner, Observer {
            if (it?.clientId != null) {
                observeToken(deviceCode, it.clientSecret)
            }
        })
    }

    private fun observeToken(deviceCode: String, clientSecret: String){

        // start checking for user confirmation
        viewModel.fetchToken(deviceCode, clientSecret)
        viewModel.tokenLiveData.observe(viewLifecycleOwner, Observer {
            if (it?.accessToken != null) {
                Log.d("VALUE FOUND", "GOT TOKEN")
            }
        })
    }

    override fun onCopyClick(value: String) {
        val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("real-debrid authorization code", value)
        // Set the clipboard's primary clip.
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(),getString(R.string.code_copied),Toast.LENGTH_SHORT).show()
    }
}