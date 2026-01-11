package com.github.livingwithhippos.unchained.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import timber.log.Timber

/** Base [Fragment] class, giving simple access to the activity ViewModel to its subclasses */
abstract class UnchainedFragment : Fragment() {

    // activity viewModel. To be used for alerting of expired token or missing network
    val activityViewModel: MainActivityViewModel by activityViewModels()

    fun safeNavigate(action: NavDirections): Boolean {
        val nav = findNavController()
        val current = nav.currentDestination
        if (current != null && current.getAction(action.actionId) != null) {
            try {
                nav.navigate(action)
                return true
            } catch (e: IllegalArgumentException) {
                Timber.w(e, "Safe navigate failed for actionId=${action.actionId}")
                return false
            }
        } else {
            Timber.w(
                "Navigation action not found from destination ${current?.id} for actionId=${action.actionId}"
            )
            return false
        }
    }
}
