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
}