package com.github.livingwithhippos.unchained.search.view

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentSearchBinding
import com.github.livingwithhippos.unchained.folderlist.view.FolderListFragment
import com.github.livingwithhippos.unchained.plugins.ParserResult
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.plugins.model.ScrapedItem
import com.github.livingwithhippos.unchained.search.model.SearchItemAdapter
import com.github.livingwithhippos.unchained.search.model.SearchItemListener
import com.github.livingwithhippos.unchained.search.viewmodel.SearchViewModel
import com.github.livingwithhippos.unchained.utilities.MAGNET_PATTERN
import com.github.livingwithhippos.unchained.utilities.extension.delayedScrolling
import com.github.livingwithhippos.unchained.utilities.extension.getThemedDrawable
import com.github.livingwithhippos.unchained.utilities.extension.hideKeyboard
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.datetime.toInstant
import timber.log.Timber

@AndroidEntryPoint
class SearchFragment : UnchainedFragment(), SearchItemListener {

    private val viewModel: SearchViewModel by viewModels()

    private val magnetPattern = Regex(MAGNET_PATTERN, RegexOption.IGNORE_CASE)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentSearchBinding.inflate(inflater, container, false)

        setup(binding)

        return binding.root
    }

    private fun setup(binding: FragmentSearchBinding) {
        showDialogsIfNeeded()
        // setup the plugin dropdown
        val pluginAdapter =
            ArrayAdapter(requireContext(), R.layout.basic_dropdown_list_item, arrayListOf<String>())
        (binding.pluginPicker.editText as? AutoCompleteTextView)?.setAdapter(pluginAdapter)

        binding.bManagePlugins.setOnClickListener {
            val action = SearchFragmentDirections.actionSearchDestToRepositoryFragment()
            findNavController().navigate(action)
        }

        viewModel.pluginLiveData.observe(viewLifecycleOwner) { parsedPlugins ->
            val plugins = parsedPlugins.first
            if (parsedPlugins.second > 0)
                requireContext()
                    .showToast(
                        resources.getQuantityString(
                            R.plurals.plugins_version_old_format,
                            parsedPlugins.second,
                            parsedPlugins.second,
                        )
                    )

            pluginAdapter.clear()
            pluginAdapter.addAll(plugins.map { it.name })

            val pluginPickerView = binding.pluginPicker.editText as? AutoCompleteTextView
            val categoryPickerView = binding.categoryPicker.editText as? AutoCompleteTextView
            if (pluginPickerView?.text.toString().isBlank() && plugins.isNotEmpty()) {
                // load the latest selected plugin or the first one available
                val lastPlugin: String = viewModel.getLastSelectedPlugin()
                val selectedPlugin: Plugin =
                    plugins.firstOrNull { it.name == lastPlugin } ?: plugins.first()

                // todo: record the item used in the preferences and reselect it at setup time
                (binding.pluginPicker.editText as? AutoCompleteTextView)?.setText(
                    selectedPlugin.name,
                    false,
                )
                setupCategory(categoryPickerView, plugins.first())
            }

            // update the categories dropdown when the selected plugins change
            pluginPickerView?.setOnItemClickListener { _, _, position, _ ->
                val selection: String? = pluginAdapter.getItem(position)
                if (selection != null) {
                    setupCategory(categoryPickerView, plugins.first { it.name == selection })
                    viewModel.setLastSelectedPlugin(plugins.first { it.name == selection }.name)
                }
            }
        }

        viewModel.fetchPlugins(requireContext())

        val adapter = SearchItemAdapter(this)
        binding.rvSearchList.adapter = adapter

        // load the sorting preference if set
        val sortTag = viewModel.getListSortPreference()
        val sortDrawableID = getSortingDrawable(sortTag)
        (binding.bSorting as MaterialButton).apply {
            icon = requireContext().getThemedDrawable(sortDrawableID)
            text = getSortingTitle(sortTag)
        }
        binding.bSorting.setOnClickListener {
            showSortingPopup(it, R.menu.sorting_popup, adapter, binding.rvSearchList)
        }

        // load the latest results if coming back from another fragment
        val lastResults = viewModel.getSearchResults()
        if (lastResults.isNotEmpty()) submitSortedList(adapter, lastResults)

        // search option
        binding.tiSearch.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    performSearch(binding, adapter)
                    true
                }
                else -> false
            }
        }

        binding.tiSearch.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                v.hideKeyboard()
            }
        }

        // search button listener
        binding.tfSearch.setEndIconOnClickListener { performSearch(binding, adapter) }
    }

    private fun showSortingPopup(
        v: View,
        @MenuRes menuRes: Int,
        searchAdapter: SearchItemAdapter,
        searchList: RecyclerView,
    ) {

        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            if (v is MaterialButton) {
                v.icon = menuItem.icon
                v.text = menuItem.title
            }
            // save the new sorting preference
            when (menuItem.itemId) {
                R.id.sortByDefault -> {
                    viewModel.setListSortPreference(FolderListFragment.TAG_DEFAULT_SORT)
                }
                R.id.sortByAZ -> {
                    viewModel.setListSortPreference(FolderListFragment.TAG_SORT_AZ)
                }
                R.id.sortByZA -> {
                    viewModel.setListSortPreference(FolderListFragment.TAG_SORT_ZA)
                }
                R.id.sortBySizeAsc -> {
                    viewModel.setListSortPreference(FolderListFragment.TAG_SORT_SIZE_ASC)
                }
                R.id.sortBySizeDesc -> {
                    viewModel.setListSortPreference(FolderListFragment.TAG_SORT_SIZE_DESC)
                }
                R.id.sortBySeeders -> {
                    viewModel.setListSortPreference(FolderListFragment.TAG_SORT_SEEDERS)
                }
                R.id.sortByAdded -> {
                    viewModel.setListSortPreference(FolderListFragment.TAG_SORT_ADDED)
                }
            }
            // update the list and scroll it to the top
            submitSortedList(searchAdapter, viewModel.getSearchResults())
            lifecycleScope.launch { searchList.delayedScrolling(requireContext()) }
            true
        }
        popup.setOnDismissListener {
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }

    private fun performSearch(binding: FragmentSearchBinding, searchAdapter: SearchItemAdapter) {
        binding.tfSearch.hideKeyboard()
        viewModel
            .completeSearch(
                query = binding.tiSearch.text.toString(),
                // fixme: this will break with same-name plugins
                pluginName = binding.pluginPicker.editText?.text.toString(),
                category = getSelectedCategory(binding.categoryPicker.editText?.text.toString()),
            )
            .observe(viewLifecycleOwner) { result ->
                when (result) {
                    is ParserResult.SingleResult -> {
                        // does this work without an append?
                        submitSortedList(searchAdapter, listOf(result.value))
                        searchAdapter.notifyDataSetChanged()
                    }
                    is ParserResult.Results -> {
                        submitSortedList(searchAdapter, result.values)
                        searchAdapter.notifyDataSetChanged()
                    }
                    is ParserResult.SearchStarted -> {
                        searchAdapter.submitList(emptyList())
                        binding.loadingCircle.isIndeterminate = true
                    }
                    is ParserResult.SearchFinished -> {
                        binding.loadingCircle.isIndeterminate = false
                        binding.loadingCircle.progress = 100
                        // update the data with cached results
                    }
                    is ParserResult.EmptyInnerLinks -> {
                        context?.showToast(R.string.no_links)
                        searchAdapter.submitList(emptyList())
                        binding.loadingCircle.isIndeterminate = false
                        binding.loadingCircle.progress = 100
                    }
                    is ParserResult.ScrapeProtectionError -> {
                        context?.showToast(R.string.connection_error)
                        searchAdapter.submitList(emptyList())
                        binding.loadingCircle.isIndeterminate = false
                        binding.loadingCircle.progress = 100
                    }
                    else -> {
                        Timber.d("Unexpected result: $result")
                        searchAdapter.submitList(emptyList())
                        binding.loadingCircle.isIndeterminate = false
                        binding.loadingCircle.progress = 100
                    }
                }
            }
    }

    private fun getSortingDrawable(tag: String): Int {
        return when (tag) {
            FolderListFragment.TAG_DEFAULT_SORT -> R.drawable.icon_sort_default
            FolderListFragment.TAG_SORT_AZ -> R.drawable.icon_sort_az
            FolderListFragment.TAG_SORT_ZA -> R.drawable.icon_sort_za
            FolderListFragment.TAG_SORT_SIZE_DESC -> R.drawable.icon_sort_size_desc
            FolderListFragment.TAG_SORT_SIZE_ASC -> R.drawable.icon_sort_size_asc
            FolderListFragment.TAG_SORT_SEEDERS -> R.drawable.icon_sort_seeders
            FolderListFragment.TAG_SORT_ADDED -> R.drawable.icon_date
            else -> R.drawable.icon_sort_default
        }
    }

    private fun getSortingTitle(tag: String): String {
        val res =
            when (tag) {
                FolderListFragment.TAG_DEFAULT_SORT -> R.string.default_string
                FolderListFragment.TAG_SORT_AZ -> R.string.sort_by_az
                FolderListFragment.TAG_SORT_ZA -> R.string.sort_by_za
                FolderListFragment.TAG_SORT_SIZE_ASC -> R.string.sort_by_size_asc
                FolderListFragment.TAG_SORT_SIZE_DESC -> R.string.sort_by_size_desc
                FolderListFragment.TAG_SORT_SEEDERS -> R.string.seeders
                FolderListFragment.TAG_SORT_ADDED -> R.string.added_date
                else -> R.string.default_string
            }

        return getString(res)
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
                                item.addedDate.toInstant().toEpochMilliseconds()
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
    }

    private fun showDialogsIfNeeded() {
        if (viewModel.isPluginDialogNeeded()) {
            val alertDialog: AlertDialog? =
                activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.apply {
                        setTitle(R.string.search_plugins)
                        setMessage(R.string.plugin_description_message)
                        setPositiveButton(R.string.close) { _, _ ->
                            viewModel.setPluginDialogNeeded(false)
                        }
                    }
                    builder.create()
                }
            alertDialog?.show()
        }

        if (viewModel.isDOHDialogNeeded()) {
            val alertDialog: AlertDialog? =
                activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.apply {
                        setTitle(R.string.doh)
                        setMessage(R.string.doh_description_message)
                        setPositiveButton(R.string.enable) { _, _ ->
                            viewModel.enableDOH(true)
                            viewModel.setDOHDialogNeeded(false)
                        }
                        setNegativeButton(R.string.disable) { _, _ ->
                            viewModel.enableDOH(false)
                            viewModel.setDOHDialogNeeded(false)
                        }
                    }
                    builder.create()
                }
            alertDialog?.show()
        }
    }

    private fun setupCategory(autoCompleteView: AutoCompleteTextView?, plugin: Plugin) {
        val choices = mutableListOf<String>()
        choices.add(getString(R.string.category_all))
        if (plugin.supportedCategories.art != null) choices.add(getString(R.string.category_art))
        if (plugin.supportedCategories.anime != null)
            choices.add(getString(R.string.category_anime))
        if (plugin.supportedCategories.doujinshi != null)
            choices.add(getString(R.string.category_doujinshi))
        if (plugin.supportedCategories.manga != null)
            choices.add(getString(R.string.category_manga))
        if (plugin.supportedCategories.software != null)
            choices.add(getString(R.string.category_software))
        if (plugin.supportedCategories.games != null)
            choices.add(getString(R.string.category_games))
        if (plugin.supportedCategories.movies != null)
            choices.add(getString(R.string.category_movies))
        if (plugin.supportedCategories.videos != null)
            choices.add(getString(R.string.category_videos))
        if (plugin.supportedCategories.pictures != null)
            choices.add(getString(R.string.category_pictures))
        if (plugin.supportedCategories.music != null)
            choices.add(getString(R.string.category_music))
        if (plugin.supportedCategories.tv != null) choices.add(getString(R.string.category_tv))
        if (plugin.supportedCategories.books != null)
            choices.add(getString(R.string.category_books))

        val adapter = ArrayAdapter(requireContext(), R.layout.basic_dropdown_list_item, choices)
        autoCompleteView?.setAdapter(adapter)

        autoCompleteView?.setText(choices.first(), false)
    }

    private fun getSelectedCategory(pickerText: String): String? {
        return when (pickerText) {
            getString(R.string.category_all) -> {
                // "all"
                // searches on "all" will just be redirected to the no_category search
                null
            }
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
            getString(R.string.category_tv) -> "books"
            else -> null
        }
    }

    override fun onClick(item: ScrapedItem) {
        viewModel.stopSearch()
        val action = SearchFragmentDirections.actionSearchDestToSearchItemFragment(item)
        findNavController().navigate(action)
    }

    companion object {
        val digitRegex = "\\d+".toRegex()
    }
}
