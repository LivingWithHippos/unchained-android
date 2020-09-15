package com.github.livingwithhippos.unchained.start.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.ui.setupWithNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.ActivityMainBinding
import com.github.livingwithhippos.unchained.settings.SettingsActivity
import com.github.livingwithhippos.unchained.settings.SettingsFragment
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel.AuthenticationState.*
import com.github.livingwithhippos.unchained.utilities.BottomNavManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var bottomNavManager: BottomNavManager? = null

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            setupNavigationManager()
        }

        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.settings -> {
                    openSettings()
                    true
                }
                else -> false
            }
        }

        val viewModel: MainActivityViewModel by viewModels()
        viewModel.authenticationState.observe(this, Observer { state ->
            when (state.getContentIfNotHandled()) {
                // go to login fragment
                UNAUTHENTICATED -> {
                    openAuthentication()
                }
                // go to login fragment and show an error message
                BAD_TOKEN-> {
                    openAuthentication()
                }
                // go to login fragment and show another error message
                ACCOUNT_LOCKED -> {
                    openAuthentication()
                }
                // do nothing
                AUTHENTICATED -> {}
                else -> throw IllegalStateException("Unknown credentials state: $state")
            }
        })
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun openAuthentication() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        if (bottomNav.selectedItemId != R.id.navigation_home) {
            bottomNav.selectedItemId = R.id.navigation_home
        }
    }

    private fun setupNavigationManager() {
        bottomNavManager?.setupNavController() ?: kotlin.run {
            bottomNavManager = BottomNavManager(
                fragmentManager = supportFragmentManager,
                containerId = R.id.nav_host_fragment,
                bottomNavigationView = findViewById(R.id.bottom_nav_view)
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        bottomNavManager?.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        bottomNavManager?.onRestoreInstanceState(savedInstanceState)
        setupNavigationManager()
    }

    private fun setupBottomNavMenu(navController: NavController) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        bottomNav?.setupWithNavController(navController)
    }

    override fun onBackPressed() {
        if (bottomNavManager?.onBackPressed() == false) super.onBackPressed()
    }

}