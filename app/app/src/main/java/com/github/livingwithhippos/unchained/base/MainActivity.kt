package com.github.livingwithhippos.unchained.base


import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import com.github.livingwithhippos.unchained.utilities.extension.observeOnce

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.ui.setupWithNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.AuthenticationState
import com.github.livingwithhippos.unchained.databinding.ActivityMainBinding
import com.github.livingwithhippos.unchained.settings.SettingsActivity
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import com.github.livingwithhippos.unchained.utilities.BottomNavManager
import com.github.livingwithhippos.unchained.utilities.SCHEME_HTTP
import com.github.livingwithhippos.unchained.utilities.SCHEME_HTTPS
import com.github.livingwithhippos.unchained.utilities.SCHEME_MAGNET
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

/**
 * A [AppCompatActivity] subclass.
 * Shared between all the fragments except for the preferences.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var bottomNavManager: BottomNavManager? = null

    private lateinit var binding: ActivityMainBinding

    val viewModel: MainActivityViewModel by viewModels()

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

        viewModel.authenticationState.observe(this, Observer { state ->
            when (state.peekContent()) {
                // go to login fragment
                AuthenticationState.UNAUTHENTICATED -> {
                    openAuthentication()
                    bottomNavManager?.disableMenuItems(listOf(R.id.navigation_home))
                }
                // refresh the token.
                // todo: if it keeps on being bad (hehe) delete the credentials and start the authentication from zero
                AuthenticationState.BAD_TOKEN -> {
                    viewModel.refreshToken()
                }
                // go to login fragment and show another error message
                AuthenticationState.ACCOUNT_LOCKED -> {
                    openAuthentication()
                    bottomNavManager?.disableMenuItems(listOf(R.id.navigation_home))
                }
                // do nothing
                AuthenticationState.AUTHENTICATED, AuthenticationState.AUTHENTICATED_NO_PREMIUM -> {
                    bottomNavManager?.enableMenuItems()
                }
            }
        })

        getIntentData()
    }

    private fun getIntentData() {

        intent?.let{
            /* Implicit intent with path to torrent file, http or magnet link */

            // clicks on magnets arrive here
            val data = it.data
            // check uri content
            if (data!=null) {

                when (data.scheme) {
                    //clicked on a torrent file or a magnet link
                    SCHEME_MAGNET, SCHEME_CONTENT, SCHEME_FILE -> {
                        // check auth state before loading it
                        viewModel.authenticationState.observeOnce(this, { auth ->
                            when (auth.peekContent()) {
                                AuthenticationState.AUTHENTICATED -> processLinkIntent(data)
                                AuthenticationState.AUTHENTICATED_NO_PREMIUM -> baseContext.showToast(R.string.premium_needed_torrent)
                                else -> showToast(R.string.please_login)
                            }
                        })
                    }
                    SCHEME_HTTP, SCHEME_HTTPS -> {}
                }
            }
        }
    }

    private fun processLinkIntent(uri: Uri) {
        // simulate click on new download tab
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        if (bottomNav.selectedItemId != R.id.navigation_download) {
            bottomNav.selectedItemId = R.id.navigation_download
        }
        viewModel.addLink(uri)
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun openAuthentication() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        // note: the [BottomNavManager] also has a selectItem() method but this should work for every bottom menu
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