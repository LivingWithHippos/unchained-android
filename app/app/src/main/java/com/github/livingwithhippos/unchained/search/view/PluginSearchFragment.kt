package com.github.livingwithhippos.unchained.search.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import androidx.fragment.app.viewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentSearchPluginsTabBinding
import com.github.livingwithhippos.unchained.folderlist.view.FolderListFragment
import com.github.livingwithhippos.unchained.plugins.ParserResult
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.search.viewmodel.SearchViewModel
import com.github.livingwithhippos.unchained.utilities.extension.hideKeyboard
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.sidesheet.SideSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class PluginSearchFragment : UnchainedFragment() {

    private val viewModel: SearchViewModel by viewModels()

    private val pluginsList: MutableList<Plugin> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSearchPluginsTabBinding.inflate(inflater, container, false)

        setup(binding)

        viewModel.pluginLiveData.observe(viewLifecycleOwner) { parsedPlugins ->
            if (pluginsList.isNotEmpty()) pluginsList.clear()
            pluginsList.addAll(parsedPlugins.first)
            setupAndShowSheet(inflater, parsedPlugins.first)
        }

        return binding.root
    }

    private fun setupAndShowSheet(inflater: LayoutInflater, plugins: List<Plugin>) {
        val sideSheetDialog = SideSheetDialog(requireContext())
        sideSheetDialog.setContentView(R.layout.sidesheet_search_plugins_options)

        sideSheetDialog.findViewById<Button>(R.id.closeButton)?.setOnClickListener {
            sideSheetDialog.dismiss()
        }

        val pluginsChipsGroup: ChipGroup =
            sideSheetDialog.findViewById<ChipGroup>(R.id.pluginsChipGroup) ?: return

        for (plugin in plugins) {
            val pluginChip: Chip = (inflater.inflate(R.layout.custom_chip_layout, pluginsChipsGroup, false) as Chip)
                .apply {
                    text = plugin.name
                    isCheckable = true
                    // todo: load this from preferences
                    isChecked = false
                }
            pluginChip.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setPluginEnabled(
                    plugin.name,
                    isChecked
                )
            }
            pluginsChipsGroup.addView(pluginChip)
            // todo: on checked listener to get the enabled list on search click
        }

        sideSheetDialog.findViewById<Chip>(R.id.allPluginsChip)?.setOnCheckedChangeListener {
            _,
            isChecked ->
            for (i in 0 until pluginsChipsGroup.childCount) {
                val chip = pluginsChipsGroup.getChildAt(i) as? Chip
                chip?.let { if (it.id != R.id.allPluginsChip) it.isChecked = isChecked }
            }
        }

        val categoryPicker: AutoCompleteTextView? =
            sideSheetDialog.findViewById(R.id.categoryPickerTextView) as? AutoCompleteTextView
        val orderPicker: AutoCompleteTextView? =
            sideSheetDialog.findViewById(R.id.sortingPickerTextView) as? AutoCompleteTextView

        if (categoryPicker != null) {
            categoryPicker.setOnItemClickListener { parent, _, position, _ ->
                val selectedItem = parent.getItemAtPosition(position).toString()
                viewModel.saveSearchCategory(stringToCategory(selectedItem))
            }

            val currentCategory = categoryToString(viewModel.getSearchCategory())
            categoryPicker.setText(currentCategory, false)
        }

        if (orderPicker != null) {
            orderPicker.setOnItemClickListener { parent, _, position, _ ->
                val selectedItem = parent.getItemAtPosition(position).toString()
                viewModel.setListSortPreference(stringToSortingOrder(selectedItem))
            }

            val currentOrder = sortingOrderToString(viewModel.getListSortPreference())
            orderPicker.setText(currentOrder, false)
        }

        sideSheetDialog.show()
    }

    private fun stringToCategory(pickerText: String): String {
        return when (pickerText) {
            getString(R.string.category_art) -> "art"
            getString(R.string.category_anime) -> "anime"
            getString(R.string.category_doujinshi) -> "doujinshi"
            getString(R.string.category_manga) -> "manga"
            getString(R.string.category_software) -> "software"
            getString(R.string.category_games) -> "games"
            getString(R.string.category_movies) -> "movies"
            getString(R.string.category_pictures) -> "pictures"
            getString(R.string.category_videos) -> "videos"
            getString(R.string.category_music) -> "music"
            getString(R.string.category_tv) -> "tv"
            getString(R.string.category_books) -> "books"
            // searches on "all" will just be redirected to the no_category search
            else -> "all"
        }
    }

    private fun categoryToString(category: String): String {
        return when (category) {
            "art" -> getString(R.string.category_art)
            "anime" -> getString(R.string.category_anime)
            "doujinshi" -> getString(R.string.category_doujinshi)
            "manga" -> getString(R.string.category_manga)
            "software" -> getString(R.string.category_software)
            "games" -> getString(R.string.category_games)
            "movies" -> getString(R.string.category_movies)
            "pictures" -> getString(R.string.category_pictures)
            "videos" -> getString(R.string.category_videos)
            "music" -> getString(R.string.category_music)
            "tv" -> getString(R.string.category_tv)
            "books" ->
                getString(R.string.category_books) // Assuming this is the correct string resource
            // searches on "all" will just be redirected to the no_category search
            else -> getString(R.string.category_all)
        }
    }

    private fun stringToSortingOrder(pickerText: String): String {
        return when (pickerText) {
            getString(R.string.default_string) -> FolderListFragment.TAG_DEFAULT_SORT
            getString(R.string.seeders) -> FolderListFragment.TAG_SORT_SEEDERS
            getString(R.string.sort_by_size_asc) -> FolderListFragment.TAG_SORT_SIZE_ASC
            getString(R.string.sort_by_size_desc) -> FolderListFragment.TAG_SORT_SIZE_DESC
            getString(R.string.sort_by_az) -> FolderListFragment.TAG_SORT_AZ
            getString(R.string.sort_by_za) -> FolderListFragment.TAG_SORT_ZA
            getString(R.string.added_date) -> FolderListFragment.TAG_SORT_ADDED
            else -> FolderListFragment.TAG_DEFAULT_SORT
        }
    }

    private fun sortingOrderToString(sortingTag: String): String {
        return when (sortingTag) {
            FolderListFragment.TAG_DEFAULT_SORT -> getString(R.string.default_string)
            "sort_seeders_tag" -> getString(R.string.seeders)
            "sort_size_asc_tag" -> getString(R.string.sort_by_size_asc)
            "sort_size_desc_tag" -> getString(R.string.sort_by_size_desc)
            "sort_az_tag" -> getString(R.string.sort_by_az)
            "sort_za_tag" -> getString(R.string.sort_by_za)
            "sort_added_tag" -> getString(R.string.added_date)
            else -> getString(R.string.default_string)
        }
    }

    private fun setup(binding: FragmentSearchPluginsTabBinding) {
        binding.tfSearch.hideKeyboard()
        binding.bPluginSettings.setOnClickListener { viewModel.fetchPlugins(requireContext()) }
        binding.bStartSearch.setOnClickListener {
            val query: String = binding.tiSearch.text?.toString()?.trim() ?: ""
            if (query.isBlank()) {
                // todo: add string
                context?.showToast(R.string.file_not_allowed)
            } else {
                // todo: category etc
                viewModel
                    .pluginSearchWithSettings(
                        query = query
                    )
                    .observe(viewLifecycleOwner) { result ->
                        when (result) {
                            is ParserResult.SingleResult -> {
                                Timber.d("Single result: ${result.value}")
                            }
                            is ParserResult.Results -> {
                                Timber.d("Results: ${result.values}")
                            }
                            is ParserResult.SearchStarted -> {
                                Timber.d("Search started")
                            }
                            is ParserResult.SearchFinished -> {
                                Timber.d("Search finished")
                            }
                            is ParserResult.EmptyInnerLinks -> {
                                Timber.d("Empty inner links")
                            }
                            else -> {
                                Timber.d("Unknown result: $result")
                            }
                        }
                    }

            }
        }
    }
}
