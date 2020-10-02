package com.github.livingwithhippos.unchained.base


import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
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
import com.github.livingwithhippos.unchained.utilities.extension.isMagnet
import com.github.livingwithhippos.unchained.utilities.extension.isTorrent
import com.github.livingwithhippos.unchained.utilities.extension.observeOnce
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.bottomnavigation.BottomNavigationView


/**
 * A [UnchainedActivity] subclass.
 * Shared between all the fragments except for the preferences.
 */
class MainActivity : UnchainedActivity() {


    private lateinit var binding: ActivityMainBinding

    val viewModel: MainActivityViewModel by viewModels()

    private val downloadReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                viewModel.checkDownload(it.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        val host: NavHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment? ?: return
        
        val navController = host.navController

        setupBottomNavMenu(navController)

        // manage the authentication state
        viewModel.authenticationState.observe(this, { state ->
            when (state.peekContent()) {
                // go to login fragment
                AuthenticationState.UNAUTHENTICATED -> {
                    openAuthentication()
                    // bottomNavManager?.disableMenuItems(listOf(R.id.navigation_home))
                }
                // refresh the token.
                // todo: if it keeps on being bad (hehe) delete the credentials and start the authentication from zero
                AuthenticationState.BAD_TOKEN -> {
                    viewModel.refreshToken()
                }
                // go to login fragment and show another error message
                AuthenticationState.ACCOUNT_LOCKED -> {
                    openAuthentication()
                    // bottomNavManager?.disableMenuItems(listOf(R.id.navigation_home))
                }
                // do nothing
                AuthenticationState.AUTHENTICATED, AuthenticationState.AUTHENTICATED_NO_PREMIUM -> {
                    // bottomNavManager?.enableMenuItems()
                }
            }
        })

        // check if the app has been opened by clicking on torrents/magnet on sharing links
        getIntentData()

        // observe for torrents downloaded
        registerReceiver(
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.top_app_bar, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                openSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getIntentData() {

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain")
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                        when {
                            text.isMagnet() -> {
                                // check auth state before loading it
                                viewModel.authenticationState.observeOnce(this, { auth ->
                                    when (auth.peekContent()) {
                                        AuthenticationState.AUTHENTICATED -> processLinkIntent(text)
                                        AuthenticationState.AUTHENTICATED_NO_PREMIUM -> baseContext.showToast(
                                            R.string.premium_needed_torrent
                                        )
                                        else -> showToast(R.string.please_login)
                                    }
                                })
                            }
                            text.isTorrent() -> {
                                viewModel.authenticationState.observeOnce(this, { auth ->
                                    when (auth.peekContent()) {
                                        AuthenticationState.AUTHENTICATED -> processLinkIntent(text)
                                        AuthenticationState.AUTHENTICATED_NO_PREMIUM -> baseContext.showToast(
                                            R.string.premium_needed_torrent
                                        )
                                        else -> showToast(R.string.please_login)
                                    }
                                })
                            }
                            else -> {
                                // we do not have other cases
                            }
                        }
                    }

            }
            Intent.ACTION_VIEW -> {
                /* Implicit intent with path to torrent file or magnet link */

                val data = intent.data
                // check uri content
                if (data != null) {

                    when (data.scheme) {
                        //clicked on a torrent file or a magnet link
                        SCHEME_MAGNET, SCHEME_CONTENT, SCHEME_FILE -> {
                            // check auth state before loading it
                            viewModel.authenticationState.observeOnce(this, { auth ->
                                when (auth.peekContent()) {
                                    AuthenticationState.AUTHENTICATED -> processLinkIntent(data)
                                    AuthenticationState.AUTHENTICATED_NO_PREMIUM -> baseContext.showToast(
                                        R.string.premium_needed_torrent
                                    )
                                    else -> showToast(R.string.please_login)
                                }
                            })
                        }
                        SCHEME_HTTP, SCHEME_HTTPS -> {
                            showToast("You activated the http/s scheme somehow")
                        }
                    }
                }
            }
            null -> { // app opened directly by the user. Do nothing.
            }
            else -> {

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadReceiver)
    }

    private fun processLinkIntent(uri: Uri) {
        // simulate click on new download tab
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        if (bottomNav.selectedItemId != R.id.navigation_download) {
            bottomNav.selectedItemId = R.id.navigation_download
        }
        viewModel.addLink(uri)
    }

    private fun processLinkIntent(text: String) = processLinkIntent(Uri.parse(text))

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

    private fun setupBottomNavMenu(navController: NavController) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        bottomNav?.setupWithNavController(navController)
    }

    companion object {
        private const val EXIT_WAIT_TIME = 2000L
    }

}