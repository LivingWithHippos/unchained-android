package com.github.livingwithhippos.unchained.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/** Base [Fragment] class, giving simple access to the activity ViewModel to its subclasses */
abstract class UnchainedFragment : Fragment() {

    // activity viewModel. To be used for alerting of expired token or missing network
    val activityViewModel: MainActivityViewModel by activityViewModel()
}
