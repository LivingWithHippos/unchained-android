package com.github.livingwithhippos.unchained.base

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.AuthenticationState
import com.github.livingwithhippos.unchained.data.repositoy.PluginRepository.Companion.TYPE_UNCHAINED
import com.github.livingwithhippos.unchained.data.service.ForegroundTorrentService
import com.github.livingwithhippos.unchained.data.service.ForegroundTorrentService.Companion.KEY_TORRENT_ID
import com.github.livingwithhippos.unchained.databinding.ActivityMainBinding
import com.github.livingwithhippos.unchained.settings.SettingsActivity
import com.github.livingwithhippos.unchained.settings.SettingsFragment.Companion.KEY_TORRENT_NOTIFICATIONS
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.SCHEME_HTTP
import com.github.livingwithhippos.unchained.utilities.SCHEME_HTTPS
import com.github.livingwithhippos.unchained.utilities.SCHEME_MAGNET
import com.github.livingwithhippos.unchained.utilities.extension.downloadFile
import com.github.livingwithhippos.unchained.utilities.extension.observeOnce
import com.github.livingwithhippos.unchained.utilities.extension.setupWithNavController
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * A [AppCompatActivity] subclass.
 * Shared between all the fragments except for the preferences.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var currentNavController: LiveData<NavController>? = null
    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var binding: ActivityMainBinding

    val viewModel: MainActivityViewModel by viewModels()

    @Inject
    lateinit var preferences: SharedPreferences

    private val downloadReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                viewModel.checkTorrentDownload(
                    it.getLongExtra(
                        DownloadManager.EXTRA_DOWNLOAD_ID,
                        -1
                    )
                )
                viewModel.checkPluginDownload(
                    applicationContext,
                    it.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        } // Else, need to wait for onRestoreInstanceState

        // list of fragments with no back arrow
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.authentication_dest,
                R.id.start_dest,
                R.id.user_dest,
                R.id.list_tabs_dest,
                R.id.search_dest
            ),
            null
        )

        // manage the authentication state
        viewModel.authenticationState.observe(
            this,
            { state ->
                when (state.peekContent()) {
                    // go to login fragment
                    AuthenticationState.UNAUTHENTICATED -> {
                        lifecycleScope.launch {
                            disableBottomNavItems(
                                R.id.navigation_lists,
                                R.id.navigation_search
                            )
                            doubleClickBottomItem(R.id.navigation_home)
                            viewModel.setTokenRefreshing(false)
                        }
                    }
                    // refresh the token.
                    AuthenticationState.BAD_TOKEN -> {
                        if (!viewModel.isTokenRefreshing()) {
                            viewModel.refreshToken()
                            viewModel.setTokenRefreshing(true)
                        } else {
                            viewModel.setUnauthenticated()
                        }
                    }
                    // go to login fragment and show another error message
                    AuthenticationState.ACCOUNT_LOCKED -> {
                        lifecycleScope.launch {
                            disableBottomNavItems(
                                R.id.navigation_lists,
                                R.id.navigation_search
                            )
                            doubleClickBottomItem(R.id.navigation_home)
                            viewModel.setTokenRefreshing(false)
                        }
                    }
                    // do nothing
                    AuthenticationState.AUTHENTICATED, AuthenticationState.AUTHENTICATED_NO_PREMIUM -> {
                        enableAllBottomNavItems()
                        viewModel.setTokenRefreshing(false)
                    }
                }
            }
        )

        disableBottomNavItems(
            R.id.navigation_lists,
            R.id.navigation_search
        )
        viewModel.fetchFirstWorkingCredentials()

        // check if the app has been opened by clicking on torrents/magnet on sharing links
        getIntentData()

        // observe for torrents downloaded
        registerReceiver(
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )

        viewModel.linkLiveData.observe(
            this,
            EventObserver { link ->
                when {
                    link.endsWith(TYPE_UNCHAINED) -> {
                        downloadPlugin(link)
                    }
                    else -> {
                        // check the authentication
                        // todo: replace all these with a simple call and just elaborate the returned error in case
                        viewModel.authenticationState.observeOnce(
                            this,
                            { auth ->
                                when (auth.peekContent()) {
                                    // same as a received magnet
                                    AuthenticationState.AUTHENTICATED -> processLinkIntent(link)
                                    AuthenticationState.AUTHENTICATED_NO_PREMIUM -> baseContext.showToast(
                                        R.string.premium_needed
                                    )
                                    else -> showToast(R.string.please_login)
                                }
                            }
                        )
                    }
                }
            }
        )

        // monitor if the torrent notification service needs to be started. It monitor the preference change itself
        // for the shutting down part
        preferences.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == KEY_TORRENT_NOTIFICATIONS) {
                val enableTorrentNotifications = sharedPreferences.getBoolean(key, false)
                if (enableTorrentNotifications) {
                    val notificationIntent = Intent(this, ForegroundTorrentService::class.java)
                    ContextCompat.startForegroundService(this, notificationIntent)
                }
            }
        }

        viewModel.messageLiveData.observe(
            this,
            EventObserver {
                showToast(it, length = Toast.LENGTH_LONG)
            }
        )

        // start the notification system if enabled
        if (preferences.getBoolean(KEY_TORRENT_NOTIFICATIONS, false)) {
            val notificationIntent = Intent(this, ForegroundTorrentService::class.java)
            ContextCompat.startForegroundService(this, notificationIntent)
        }

        viewModel.connectivityLiveData.observe(
            this,
            {
                if (it) {
                    Timber.d("connection enabled")
                } else {
                    Timber.e("connection disabled")
                    applicationContext.showToast(R.string.no_network_connection)
                }
            }
        )
        viewModel.setupConnectivityCheck(applicationContext)
    }

    private fun downloadPlugin(link: String) {
        val pluginName = link.replace("%2F", "/").split("/").last()
        val manager =
            applicationContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val queuedDownload = manager.downloadFile(
            link = link,
            title = getString(R.string.unchained_plugin_download),
            description = getString(R.string.temporary_plugin_download),
            fileName = pluginName
        )
        when (queuedDownload) {
            is EitherResult.Failure -> {
                applicationContext.showToast(
                    getString(
                        R.string.download_not_started_format,
                        pluginName
                    )
                )
            }
            is EitherResult.Success -> {
                viewModel.setPluginDownload(queuedDownload.success)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.top_app_bar, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp(appBarConfiguration) ?: false
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
            // shared text link
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain")
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                        viewModel.downloadSupportedLink(text)
                    }
            }
            // files clicked
            Intent.ACTION_VIEW -> {
                /* Implicit intent with path to torrent file or magnet link */

                val data = intent.data
                // check uri content
                if (data != null) {

                    when (data.scheme) {
                        // clicked on a torrent file or a magnet link or .unchained file
                        SCHEME_MAGNET, SCHEME_CONTENT, SCHEME_FILE -> {
                            when {
                                // check if it's a search plugin
                                data.path?.endsWith(TYPE_UNCHAINED) == true -> addSearchPlugin(data)
                                else -> {
                                    // it's a magnet/torrent, check auth state before loading it
                                    viewModel.authenticationState.observeOnce(
                                        this,
                                        { auth ->
                                            when (auth.peekContent()) {
                                                AuthenticationState.AUTHENTICATED -> processLinkIntent(
                                                    data
                                                )
                                                AuthenticationState.AUTHENTICATED_NO_PREMIUM -> baseContext.showToast(
                                                    R.string.premium_needed_torrent
                                                )
                                                else -> showToast(R.string.please_login)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        SCHEME_HTTP, SCHEME_HTTPS -> {
                            showToast("You activated the http/s scheme somehow")
                        }
                    }
                }
            }
            null -> {
                // could be because of the tap on a notification
                intent.getStringExtra(KEY_TORRENT_ID)?.let { id ->

                    viewModel.authenticationState.observeOnce(
                        this,
                        { auth ->
                            if (auth.peekContent() == AuthenticationState.AUTHENTICATED || auth.peekContent() == AuthenticationState.AUTHENTICATED_NO_PREMIUM)
                                processTorrentNotificationIntent(id)
                        }
                    )
                }
            }
            else -> {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadReceiver)
    }

    private fun processTorrentNotificationIntent(torrentID: String) {
        lifecycleScope.launch {
            doubleClickBottomItem(R.id.navigation_lists)
            viewModel.addTorrentId(torrentID)
        }
    }

    private fun processLinkIntent(uri: Uri) {
        lifecycleScope.launch {
            doubleClickBottomItem(R.id.navigation_lists)
            viewModel.addLink(uri)
        }
    }

    private suspend fun doubleClickBottomItem(destinationID: Int) {
        // simulate click on a bottom bar option
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)

        // if the tab was already selected, a single tap will bring us back to the first fragment of its navigation xml. Otherwise, simulate another click after a delay
        if (bottomNav.selectedItemId != destinationID) {
            bottomNav.selectedItemId = destinationID
        }
        delay(100)
        bottomNav.selectedItemId = destinationID
    }

    private fun processLinkIntent(link: String) = processLinkIntent(Uri.parse(link))

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun addSearchPlugin(data: Uri) {
        viewModel.addPlugin(applicationContext, data)
    }

    // the standard menu items to disable are those for the download/torrent lists and the new download one
    private fun disableBottomNavItems(vararg itemsIDs: Int) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        bottomNav.menu.forEach {
            if (itemsIDs.contains(it.itemId))
                it.isEnabled = false
        }
    }

    private fun enableAllBottomNavItems() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        bottomNav.menu.forEach {
            it.isEnabled = true
        }
    }

    /**
     * Called on first creation and when restoring state.
     */
    private fun setupBottomNavigationBar() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav_view)

        val navGraphIds = listOf(
            R.navigation.home_nav_graph,
            R.navigation.lists_nav_graph,
            R.navigation.search_nav_graph,
        )

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = bottomNavigationView.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_fragment,
            intent = intent
        )

        // Whenever the selected controller changes, setup the action bar.
        controller.observe(
            this,
            { navController ->
                setupActionBarWithNavController(navController, appBarConfiguration)
            }
        )
        currentNavController = controller
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // BottomNavigationBar with Navigation
        setupBottomNavigationBar()
    }

    override fun onBackPressed() {
        // if the user is pressing back on an "exiting"fragment, show a toast alerting him and wait for him to press back again for confirmation
        val navController = currentNavController?.value

        if (navController != null) {
            val currentDestination = navController.currentDestination
            val previousDestination = navController.previousBackStackEntry

            // check if we're pressing back from the user or authentication fragment
            if (currentDestination?.id == R.id.user_dest || currentDestination?.id == R.id.authentication_dest) {
                // check the destination for the back action
                if (previousDestination == null ||
                    previousDestination.destination.id == R.id.authentication_dest ||
                    previousDestination.destination.id == R.id.start_dest ||
                    previousDestination.destination.id == R.id.user_dest ||
                    previousDestination.destination.id == R.id.search_dest
                ) {
                    // check if it has been 2 seconds since the last time we pressed back
                    val pressedTime = System.currentTimeMillis()
                    val lastPressedTime = viewModel.getLastBackPress()
                    // exit if pressed back twice in EXIT_WAIT_TIME
                    if (pressedTime - lastPressedTime <= EXIT_WAIT_TIME)
                        finish()
                    // else update the last time the user pressed back
                    else {
                        viewModel.setLastBackPress(pressedTime)
                        this.showToast(R.string.press_again_exit)
                    }
                } else {
                    super.onBackPressed()
                }
            } else {
                super.onBackPressed()
            }
        } else
            super.onBackPressed()
    }

    companion object {
        private const val EXIT_WAIT_TIME = 2000L
    }
}
