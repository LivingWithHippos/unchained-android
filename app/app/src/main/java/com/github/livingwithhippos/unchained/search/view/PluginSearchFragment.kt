package com.github.livingwithhippos.unchained.search.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.viewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentSearchPluginsTabBinding
import com.github.livingwithhippos.unchained.search.viewmodel.SearchViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.sidesheet.SideSheetDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PluginSearchFragment: UnchainedFragment() {

    private val viewModel: SearchViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSearchPluginsTabBinding.inflate(inflater, container, false)

        setup(binding)

        viewModel.pluginLiveData.observe(viewLifecycleOwner) { parsedPlugins ->
            val sideSheetDialog = SideSheetDialog(requireContext())
            sideSheetDialog.setContentView(R.layout.sidesheet_search_plugins_options)

            sideSheetDialog.findViewById<Button>(R.id.closeButton)?.setOnClickListener {
                sideSheetDialog.dismiss()
            }

            val pluginsChipsGroup: ChipGroup = sideSheetDialog.findViewById<ChipGroup>(R.id.pluginsChipGroup) ?: return@observe

            for (plugin in parsedPlugins.first) {
                pluginsChipsGroup.addView((inflater.inflate(R.layout.custom_chip_layout, pluginsChipsGroup, false) as Chip).apply {
                    text = plugin.name
                    isCheckable = true
                    // todo: load this from preferences
                    isChecked = false
                })
                // todo: on checked listener to get the enabled list on search click
            }

            sideSheetDialog.findViewById<Chip>(R.id.allPluginsChip)?.setOnCheckedChangeListener { _, isChecked ->
                for (i in 0 until pluginsChipsGroup.childCount) {
                    val chip = pluginsChipsGroup.getChildAt(i) as? Chip
                    chip?.let {
                        if (it.id != R.id.allPluginsChip)
                            it.isChecked = isChecked
                    }
                }
            }
            sideSheetDialog.show()
        }

        return binding.root
    }

    private fun setup(binding: FragmentSearchPluginsTabBinding) {
        binding.bOptions.setOnClickListener {
            viewModel.fetchPlugins(requireContext())
        }
    }
}