package com.github.livingwithhippos.unchained.search.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentSearchPluginsTabBinding
import com.github.livingwithhippos.unchained.folderlist.view.FolderListFragment
import com.github.livingwithhippos.unchained.plugins.ParserResult
import com.github.livingwithhippos.unchained.plugins.model.ScrapedItem
import com.github.livingwithhippos.unchained.search.model.SearchItemAdapter
import com.github.livingwithhippos.unchained.search.model.SearchItemListener
import com.github.livingwithhippos.unchained.search.view.SearchFragment.Companion.digitRegex
import com.github.livingwithhippos.unchained.search.viewmodel.PluginsAndServices
import com.github.livingwithhippos.unchained.search.viewmodel.SearchViewModel
import com.github.livingwithhippos.unchained.utilities.extension.getThemeColor
import com.github.livingwithhippos.unchained.utilities.extension.hideKeyboard
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.sidesheet.SideSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlin.time.Instant
import timber.log.Timber

@AndroidEntryPoint
class PluginSearchFragment : UnchainedFragment(), SearchItemListener {

    private val viewModel: SearchViewModel by viewModels()
    private val searchResultsList: MutableList<ScrapedItem> = mutableListOf()

    private var _binding: FragmentSearchPluginsTabBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSearchPluginsTabBinding.inflate(inflater, container, false)

        setup(binding)

        viewModel.pluginLiveData.observe(viewLifecycleOwner) { parsedPlugins ->
            setupAndShowSheet(inflater, parsedPlugins)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupAndShowSheet(
        inflater: LayoutInflater,
        pluginsAndServices: PluginsAndServices,
    ) {
        val sideSheetDialog = SideSheetDialog(requireContext())
        sideSheetDialog.setContentView(R.layout.sidesheet_search_plugins_options)

        sideSheetDialog.findViewById<Button>(R.id.btnOpenRepositories)?.setOnClickListener {
            val action = PluginSearchFragmentDirections.actionPluginSearchToPluginsRepository()
            findNavController().navigate(action)
            sideSheetDialog.dismiss()
        }

        sideSheetDialog.findViewById<Button>(R.id.closeButton)?.setOnClickListener {
            sideSheetDialog.dismiss()
        }

        val pluginsChipsGroup: ChipGroup =
            sideSheetDialog.findViewById<ChipGroup>(R.id.pluginsChipGroup) ?: return

        for (plugin in pluginsAndServices.plugins) {
            val pluginChip: Chip =
                (inflater.inflate(R.layout.custom_chip_layout, pluginsChipsGroup, false) as Chip)
                    .apply {
                        text = plugin.name
                        isCheckable = true
                        isChecked = plugin.selected == true
                    }
            pluginChip.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setPluginEnabled(plugin.name, isChecked)
            }
            pluginsChipsGroup.addView(pluginChip)
        }

        if (pluginsAndServices.services.isNotEmpty()) {
            val primaryColor = getThemeColor(requireContext(), android.R.attr.colorPrimary)
            // add a divider between plugins and services
            val divider =
                View(requireContext()).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(primaryColor)
                }
            pluginsChipsGroup.addView(divider)

            for (service in pluginsAndServices.services) {
                val pluginChip: Chip =
                    (inflater.inflate(R.layout.custom_chip_layout, pluginsChipsGroup, false)
                            as Chip)
                        .apply {
                            text = service.name
                            isCheckable = true
                            isChecked = service.enabled == true
                        }
                pluginChip.setOnCheckedChangeListener { _, isChecked ->
                    viewModel.setServiceEnabled(service, isChecked)
                }
                pluginsChipsGroup.addView(pluginChip)
            }
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
        binding.bPluginSettings.setOnClickListener {
            viewModel.fetchPluginsAndServices(requireContext())
        }
        val adapter = SearchItemAdapter(this)
        binding.rvSearchList.adapter = adapter

        binding.bStartSearch.setOnClickListener {
            val query: String = binding.tiSearch.text?.toString()?.trim() ?: ""
            if (query.isBlank()) {
                // todo: add string
                context?.showToast(R.string.missing_parameter)
                return@setOnClickListener
            }
            binding.tiSearch.hideKeyboard()
            val searchLiveData = viewModel.pluginSearchWithSettings(query = query)
            // Keep only one active observer for search events to avoid duplicate/lost UI updates.
            searchLiveData.removeObservers(viewLifecycleOwner)
            searchLiveData.observe(viewLifecycleOwner) { result ->
                when (result) {
                    is ParserResult.SingleResult -> {
                        searchResultsList.add(result.value)
                        submitSortedList(adapter, searchResultsList)
                    }
                    is ParserResult.Results -> {
                        searchResultsList.addAll(result.values)
                        submitSortedList(adapter, searchResultsList)
                    }
                    is ParserResult.SearchStarted -> {
                        Timber.d("Search started")
                        searchResultsList.clear()
                        submitSortedList(adapter, searchResultsList)
                        binding.searchingProgress.visibility = View.VISIBLE
                    }
                    is ParserResult.SearchFinished -> {
                        Timber.d("Search finished")
                        binding.searchingProgress.visibility = View.INVISIBLE
                    }
                    is ParserResult.EmptyInnerLinks -> {}
                    is ParserResult.NoEnabledPlugins -> {
                        context?.showToast(R.string.please_select_plugins)
                    }
                    else -> {
                        Timber.d("Unknown result: $result")
                    }
                }
            }
        }
    }

    private fun submitSortedList(adapter: SearchItemAdapter, items: List<ScrapedItem>) {
        when (viewModel.getListSortPreference()) {
            FolderListFragment.TAG_DEFAULT_SORT -> {
                adapter.submitList(items)
            }
            FolderListFragment.TAG_SORT_AZ -> {
                adapter.submitList(items.sortedBy { item -> item.name })
            }
            FolderListFragment.TAG_SORT_ZA -> {
                adapter.submitList(items.sortedByDescending { item -> item.name })
            }
            FolderListFragment.TAG_SORT_SIZE_DESC -> {
                adapter.submitList(items.sortedByDescending { item -> item.parsedSize })
            }
            FolderListFragment.TAG_SORT_SIZE_ASC -> {
                adapter.submitList(items.sortedBy { item -> item.parsedSize })
            }
            FolderListFragment.TAG_SORT_SEEDERS -> {
                adapter.submitList(
                    items.sortedByDescending { item ->
                        if (item.seeders != null) {
                            digitRegex.find(item.seeders)?.value?.toInt()
                        } else null
                    }
                )
            }
            FolderListFragment.TAG_SORT_ADDED -> {
                adapter.submitList(
                    items.sortedByDescending { item ->
                        if (item.addedDate != null) {
                            try {
                                Instant.parse(item.addedDate).toEpochMilliseconds()
                            } catch (e: Exception) {
                                null
                            }
                        } else null
                    }
                )
            }
            else -> {
                adapter.submitList(items)
            }
        }
        adapter.notifyDataSetChanged()
    }

    override fun onClick(item: ScrapedItem) {
        viewModel.stopSearch()
        val action = PluginSearchFragmentDirections.actionPluginSearchToSearchItem(item)
        findNavController().navigate(action)
    }
}
