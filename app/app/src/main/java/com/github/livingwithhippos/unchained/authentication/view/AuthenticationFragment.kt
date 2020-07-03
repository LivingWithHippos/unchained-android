package com.github.livingwithhippos.unchained.authentication.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.authentication.viewmodel.AuthenticationViewModel
import com.github.livingwithhippos.unchained.databinding.FragmentAuthenticationBinding
import com.github.livingwithhippos.unchained.databinding.FragmentUserProfileBinding
import com.github.livingwithhippos.unchained.user.viewmodel.UserProfileViewModel


/**
 * A simple [Fragment] subclass.
 * Use the [AuthenticationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AuthenticationFragment : Fragment() {

    private val viewModel: AuthenticationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val authBinding = FragmentAuthenticationBinding.inflate(inflater,container,false)

        viewModel.fetchAuthenticationInfo()

        viewModel.authLiveData.observe(viewLifecycleOwner, Observer {
            authBinding.auth = it
        })

        return authBinding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment.
         *
         * @return A new instance of fragment AuthenticationFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
            AuthenticationFragment().apply {
            }
    }
}