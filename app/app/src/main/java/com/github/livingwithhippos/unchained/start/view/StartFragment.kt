package com.github.livingwithhippos.unchained.start.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint


/**
 * A simple [Fragment] subclass.
 */
@AndroidEntryPoint
class StartFragment : Fragment() {

    // ViewModel shared with main activity and eventually other fragments
    private val viewModel: MainActivityViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check our credentials and decide to navigate to the user fragment or the authentication one.
        checkCredentialsStatus(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    // note: we could just load all the credentials once here and then check them
    // no need to interrogate again the database for the partial ones
    private fun checkCredentialsStatus(viewModel: MainActivityViewModel) {
        viewModel.fetchFirstWorkingCredentials()
        viewModel.workingCredentialsLiveData.observe(this, Observer {
            if (it != null) {
                //todo: load complete user fragment
            } else {
                // check partial records
            }
        })
    }
}