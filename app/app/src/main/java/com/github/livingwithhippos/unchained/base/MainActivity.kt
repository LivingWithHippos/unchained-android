package com.github.livingwithhippos.unchained.base

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.forEach
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.UserAction
import com.github.livingwithhippos.unchained.data.repository.PluginRepository.Companion.TYPE_UNCHAINED
import com.github.livingwithhippos.unchained.data.service.ForegroundTorrentService
import com.github.livingwithhippos.unchained.data.service.ForegroundTorrentService.Companion.KEY_TORRENT_ID
import com.github.livingwithhippos.unchained.databinding.ActivityMainBinding
import com.github.livingwithhippos.unchained.settings.view.SettingsActivity
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment.Companion.KEY_TORRENT_NOTIFICATIONS
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityMessage
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import com.github.livingwithhippos.unchained.statemachine.authentication.CurrentFSMAuthentication
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationState
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.SCHEME_HTTP
import com.github.livingwithhippos.unchained.utilities.SCHEME_HTTPS
import com.github.livingwithhippos.unchained.utilities.SCHEME_MAGNET
import com.github.livingwithhippos.unchained.utilities.TelemetryManager
import com.github.livingwithhippos.unchained.utilities.extension.downloadFile
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

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    // countly crash reporter set up. Debug mode only
    override fun onStart() {
        super.onStart()
        TelemetryManager.onStart(this)
    }

    override fun onStop() {
        TelemetryManager.onStop()
        super.onStop()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        TelemetryManager.onConfigurationChanged(newConfig)
    }

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

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.top_app_bar, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.settings -> {
                        openSettings()
                        true
                    }
                    else -> false
                }
            }
        })

        viewModel.fsmAuthenticationState.observe(
            this
        ) {
            // do not inline this variable in the when, because getContentIfNotHandled() will change its value to null if checked again in WaitingUserAction
            val authState: FSMAuthenticationState? = it?.getContentIfNotHandled()
            when (authState) {
                null -> {
                    // do nothing
                }
                is FSMAuthenticationState.CheckCredentials -> {
                    viewModel.checkCredentials()
                }
                FSMAuthenticationState.Start -> {
                    // do nothing. This is our starting point. It should not be reached again
                }
                FSMAuthenticationState.StartNewLogin -> {
                    // this state should be managed by the fragments directly
                }
                FSMAuthenticationState.AuthenticatedOpenToken -> {
                    // unlock the bottom menu
                    enableAllBottomNavItems()
                }
                FSMAuthenticationState.RefreshingOpenToken -> {
                    viewModel.refreshToken()
                }
                FSMAuthenticationState.AuthenticatedPrivateToken -> {
                    // unlock the bottom menu
                    enableAllBottomNavItems()
                }
                FSMAuthenticationState.WaitingToken -> {
                    // this state should be managed by the fragments directly
                }
                FSMAuthenticationState.WaitingUserConfirmation -> {
                    // this state should be managed by the fragments directly
                }
                is FSMAuthenticationState.WaitingUserAction -> {
                    // go back to the user/start fragment and disable the buttons.
                    when (authState.action) {
                        UserAction.PERMISSION_DENIED -> showToast(R.string.permission_denied)
                        UserAction.TFA_NEEDED -> showToast(R.string.tfa_needed)
                        UserAction.TFA_PENDING -> showToast(R.string.tfa_pending)
                        UserAction.IP_NOT_ALLOWED -> showToast(R.string.ip_Address_not_allowed)
                        UserAction.UNKNOWN -> showToast(R.string.generic_login_error)
                        UserAction.NETWORK_ERROR -> showToast(R.string.network_error)
                        UserAction.RETRY_LATER -> showToast(R.string.retry_later)
                        null -> showToast(R.string.retry_later)
                    }
                    // this state should be managed by the fragments directly
                    lifecycleScope.launch {
                        disableBottomNavItems(
                            R.id.navigation_lists,
                            R.id.navigation_search
                        )
                        doubleClickBottomItem(R.id.navigation_home)
                    }
                }
            }
        }

        // disable the bottom menu items before loading the credentials
        disableBottomNavItems(
            R.id.navigation_lists,
            R.id.navigation_search
        )

        // to avoid issues with restoring the app state we check the current state before calling this
        when (viewModel.fsmAuthenticationState.value?.peekContent()) {
            is FSMAuthenticationState.AuthenticatedPrivateToken, FSMAuthenticationState.AuthenticatedOpenToken -> {
                // we probably stopped and restored the app, do the same actions
                // in the viewModel.fsmAuthenticationState.observe for these states

                // unlock the bottom menu
                enableAllBottomNavItems()
            }
            else -> {
                // todo: decide if we need to check other possible values or reset the fsm to checkCredentials in these states and call startAuthenticationMachine
                // start the authentication state machine, the first time it's going to be null
                viewModel.startAuthenticationMachine()
            }
        }

        // check if the app has been opened by clicking on torrents/magnet on sharing links
        getIntentData()

        // observe for torrents downloaded
        registerReceiver(
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )

        viewModel.linkLiveData.observe(this) {
            it?.getContentIfNotHandled()?.let { link ->
                when {
                    link.endsWith(TYPE_UNCHAINED, ignoreCase = true) -> {
                        downloadPlugin(link)
                    }
                    else -> {
                        // check the authentication
                        processExternalRequestOnAuthentication(Uri.parse(link))
                    }
                }
            }
        }

        viewModel.jumpTabLiveData.observe(
            this,
            EventObserver {
                when (it) {
                    "user" -> {
                        // do nothing
                    }
                    "downloads" -> {
                        lifecycleScope.launch {
                            doubleClickBottomItem(R.id.navigation_lists)
                        }
                    }
                    "search" -> {
                        lifecycleScope.launch {
                            doubleClickBottomItem(R.id.navigation_search)
                        }
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

        @SuppressLint("ShowToast")
        val currentToast: Toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)

        viewModel.messageLiveData.observe(
            this
        ) {
            when (val content = it?.getContentIfNotHandled()) {
                is MainActivityMessage.InstalledPlugins -> {
                    currentToast.cancel()
                    currentToast.setText(
                        getString(
                            R.string.n_plugins_installed,
                            content.number
                        )
                    )
                    currentToast.show()
                }
                is MainActivityMessage.StringID -> {
                    currentToast.cancel()
                    currentToast.setText(getString(content.id))
                    currentToast.show()
                }
                null -> {}
            }
        }

        // start the notification system if enabled
        if (preferences.getBoolean(KEY_TORRENT_NOTIFICATIONS, false)) {
            val notificationIntent = Intent(this, ForegroundTorrentService::class.java)
            ContextCompat.startForegroundService(this, notificationIntent)
        }

        // load the old share preferences of kodi devices into the db and then delete them
        viewModel.updateOldKodiPreferences()

        viewModel.connectivityLiveData.observe(
            this
        ) {
            when (it) {
                true -> Timber.d("connection enabled")
                false -> {
                    Timber.e("connection disabled")
                    applicationContext.showToast(R.string.no_network_connection)
                }
                null -> {
                    Timber.e("connection null")
                }
            }
        }

        viewModel.clearCache(applicationContext.cacheDir)
    }

    override fun onResume() {
        super.onResume()
        viewModel.addConnectivityCheck(applicationContext)
    }

    override fun onPause() {
        super.onPause()
        viewModel.removeConnectivityCheck(applicationContext)
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
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
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
                                data.path?.endsWith(
                                    TYPE_UNCHAINED,
                                    ignoreCase = true
                                ) == true -> addSearchPlugin(data)
                                else -> {
                                    // it's a magnet/torrent, check auth state before loading it
                                    processExternalRequestOnAuthentication(data)
                                }
                            }
                        }
                        SCHEME_HTTP, SCHEME_HTTPS -> {
                            processExternalRequestOnAuthentication(data)
                        }
                    }
                }
            }
            null -> {
                // could be because of the tap on a notification
                intent.getStringExtra(KEY_TORRENT_ID)?.let { id ->

                    when (viewModel.getAuthenticationMachineState()) {
                        FSMAuthenticationState.AuthenticatedOpenToken, FSMAuthenticationState.AuthenticatedPrivateToken -> {
                            processTorrentNotificationIntent(id)
                        }
                        FSMAuthenticationState.RefreshingOpenToken -> {
                            // todo: launch it after a delay
                        }
                        else -> {
                            // do nothing
                        }
                    }
                }
            }
            else -> {
            }
        }
    }

    private fun processExternalRequestOnAuthentication(uri: Uri) {
        lifecycleScope.launch {
            delayLoop@ for (loop in 1..5) {
                when (viewModel.getCurrentAuthenticationStatus()) {
                    CurrentFSMAuthentication.Authenticated -> {
                        // auth ok, process link and exit loop
                        processExternalRequest(uri)
                        break@delayLoop
                    }
                    CurrentFSMAuthentication.Unauthenticated -> {
                        // auth not ok, show error and exit loop
                        showToast(R.string.please_login)
                        break@delayLoop
                    }
                    CurrentFSMAuthentication.Waiting -> {
                        // auth may become ok, delay and continue loop
                        delay(AUTH_DELAY)
                    }
                }
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

    private fun processExternalRequest(uri: Uri) {
        lifecycleScope.launch {
            doubleClickBottomItem(R.id.navigation_lists)
            viewModel.addLink(uri)
        }
    }

    /**
     * simulate a double click on a bottom bar option which will bring us to the first destinatin of that tab.
     *
     * @param destinationID the id of the bottom item to click
     */
    private suspend fun doubleClickBottomItem(destinationID: Int) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)

        // if the tab was already selected, a single tap will bring us back to the first fragment of its navigation xml. Otherwise, simulate another click after a delay
        if (bottomNav.selectedItemId != destinationID) {
            bottomNav.selectedItemId = destinationID
        }
        delay(100)
        bottomNav.selectedItemId = destinationID
    }

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

        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.nav_host_fragment
        ) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        bottomNavigationView.setupWithNavController(navController)

        // Setup the ActionBar with navController and 3 top level destinations
        // these won't show a back/up arrow
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.authentication_dest,
                R.id.start_dest,
                R.id.user_dest,
                R.id.list_tabs_dest,
                R.id.search_dest
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
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
        private const val AUTH_DELAY = 500L
    }
}
