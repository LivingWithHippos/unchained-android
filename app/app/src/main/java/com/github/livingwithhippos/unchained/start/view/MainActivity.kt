package com.github.livingwithhippos.unchained.start.view

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import com.github.livingwithhippos.unchained.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewModel: MainActivityViewModel by viewModels()

        // check our credentials and decide to navigate to the user fragment or the authentication one.
        checkCredentialsStatus(viewModel)
    }

    private fun checkCredentialsStatus(viewModel: MainActivityViewModel) {
        viewModel.fetchFirstWorkingCredentials()
        viewModel.workingCredentialsLiveData.observe(this, Observer {
            if (it != null) {
                //todo: load complete user fragment
            } else {
                // check partial records
                checkPartialCredentials(viewModel)
            }
        })
    }

    private fun checkPartialCredentials(viewModel: MainActivityViewModel) {
        viewModel.fetchPartialCredentials()
        viewModel.partialCredentialsLiveData.observe(this, Observer {
            if (it.isNullOrEmpty()){
                // todo: load authentication fragment
            } else {
                // todo: load authentication fragment with partial data
            }
        })
    }
}