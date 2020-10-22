package com.github.livingwithhippos.unchained.base

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class UnchainedActivity : AppCompatActivity() {

    @Inject
    lateinit var preferences: SharedPreferences

}