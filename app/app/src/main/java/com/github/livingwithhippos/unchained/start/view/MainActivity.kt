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