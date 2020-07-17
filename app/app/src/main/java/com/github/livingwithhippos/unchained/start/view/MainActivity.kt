package com.github.livingwithhippos.unchained.start.view

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.ui.setupWithNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.ActivityMainBinding
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import com.github.livingwithhippos.unchained.utilities.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    var currentNavController: NavController? = null
        private set

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModel: MainActivityViewModel by viewModels()


        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // BottomNavigationBar with Navigation
        setupBottomNavigationBar()
    }

    private fun setupBottomNavMenu(navController: NavController) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        bottomNav?.setupWithNavController(navController)
    }

    private fun setupBottomNavigationBar() {
        binding.bottomNavView.setupWithNavController(
            listOf(
                R.navigation.home_nav_graph,
                R.navigation.download_nav_graph
            ),
            supportFragmentManager,
            R.id.nav_host_fragment,
            intent
        ).observe(this) { navController ->
            currentNavController = navController

            /*
            manages if we go to a special destination that needs something
            navController.addOnDestinationChangedListener { _, destination, _ ->
            }
             */

        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.navigateUp() ?: super.onSupportNavigateUp()
    }
}