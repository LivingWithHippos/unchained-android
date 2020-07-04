package com.github.livingwithhippos.unchained.authentication.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.transition.Scene
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.authentication.viewmodel.AuthenticationViewModel
import com.github.livingwithhippos.unchained.databinding.SceneAuthenticationLinkBinding


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
        val view = inflater.inflate(R.layout.fragment_authentication, container, false)
        val sceneRoot: ViewGroup = view.findViewById(R.id.scene_root)
        val loadingScene: Scene = Scene.getSceneForLayout(
            sceneRoot,
            R.layout.scene_authentication_loading,
            requireContext()
        )
        val fetchedAuthLinkScene: Scene =
            Scene.getSceneForLayout(sceneRoot, R.layout.scene_authentication_link, requireContext())

        //todo: add loading gif
        viewModel.fetchAuthenticationInfo()

        viewModel.authLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {

            val authBinding = SceneAuthenticationLinkBinding.inflate(inflater, container, false)
            //fixme: data not showing even when different from null
            authBinding.auth = it
            val fadeTransition: Transition =
                TransitionInflater.from(requireContext())
                    .inflateTransition(R.transition.fade_transition)

            TransitionManager.go(fetchedAuthLinkScene, fadeTransition)
            }
        })

        return view
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