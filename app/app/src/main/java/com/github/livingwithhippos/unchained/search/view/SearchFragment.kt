package com.github.livingwithhippos.unchained.search.view

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.repository.DownloadResult
import com.github.livingwithhippos.unchained.databinding.FragmentSearchBinding
import com.github.livingwithhippos.unchained.folderlist.view.FolderListFragment
import com.github.livingwithhippos.unchained.plugins.ParserResult
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.plugins.model.ScrapedItem
import com.github.livingwithhippos.unchained.search.model.SearchItemAdapter
import com.github.livingwithhippos.unchained.search.model.SearchItemListener
import com.github.livingwithhippos.unchained.search.viewmodel.SearchViewModel
import com.github.livingwithhippos.unchained.utilities.MAGNET_PATTERN
import com.github.livingwithhippos.unchained.utilities.PLUGINS_PACK_FOLDER
import com.github.livingwithhippos.unchained.utilities.PLUGINS_PACK_LINK
import com.github.livingwithhippos.unchained.utilities.PLUGINS_PACK_NAME
import com.github.livingwithhippos.unchained.utilities.PLUGINS_URL
import com.github.livingwithhippos.unchained.utilities.extension.delayedScrolling
import com.github.livingwithhippos.unchained.utilities.extension.getThemedDrawable
import com.github.livingwithhippos.unchained.utilities.extension.hideKeyboard
import com.github.livingwithhippos.unchained.utilities.extension.openExternalWebPage
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

@AndroidEntryPoint
class SearchFragment : UnchainedFragment(), SearchItemListener {

    private val viewModel: SearchViewModel by viewModels()

    private val magnetPattern = Regex(MAGNET_PATTERN, RegexOption.IGNORE_CASE)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSearchBinding.inflate(inflater, container, false)

        setup(binding)

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.search_bar, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.plugins_pack -> {
                            context?.showToast(R.string.downloading)
                            lifecycleScope.launch {
                                val cacheDir = context?.cacheDir

                                if (cacheDir != null) {
                                    // clean up old files
                                    // todo: also clear other files, at least ending with zip
                                    File(cacheDir, PLUGINS_PACK_FOLDER).deleteRecursively()

                                    activityViewModel.downloadFileToCache(
                                        PLUGINS_PACK_LINK,
                                        PLUGINS_PACK_NAME,
                                        cacheDir,
                                        ".zip"
                                    ).observe(
                                        viewLifecycleOwner
                                    ) {
                                        when (it) {
                                            is DownloadResult.End -> {
                                                activityViewModel.processPluginsPack(cacheDir, requireContext().filesDir, it.fileName)
                                            }
                                            DownloadResult.Failure -> {
                                                context?.showToast(R.string.error_loading_file)
                                            }
                                            is DownloadResult.Progress -> {
                                                Timber.d("Plugins pack progress: ${it.percent}")
                                            }
                                            DownloadResult.WrongURL -> {
                                                context?.showToast(R.string.error_loading_file)
                                            }
                                        }
                                    }
                                }
                            }
                            true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner, Lifecycle.State.RESUMED
        )

        return binding.root
    }

    private fun setup(binding: FragmentSearchBinding) {
        showDialogsIfNeeded()
        // setup the plugin dropdown
        val pluginAdapter =
            ArrayAdapter(requireContext(), R.layout.plugin_list_item, arrayListOf<String>())
        (binding.pluginPicker.editText as? AutoCompleteTextView)?.setAdapter(pluginAdapter)

        viewModel.pluginLiveData.observe(viewLifecycleOwner) { parsedPlugins ->

            val plugins = parsedPlugins.first
            if (parsedPlugins.second > 0)
                requireContext().showToast(
                    resources.getQuantityString(
                        R.plurals.plugins_version_old_format,
                        parsedPlugins.second,
                        parsedPlugins.second
                    )
                )

            pluginAdapter.clear()
            pluginAdapter.addAll(plugins.map { it.name })

            val pluginPickerView = binding.pluginPicker.editText as? AutoCompleteTextView
            val categoryPickerView = binding.categoryPicker.editText as? AutoCompleteTextView
            if (pluginPickerView?.text.toString().isBlank() &&
                plugins.isNotEmpty()
            ) {
                // load the latest selected plugin or the first one available
                val lastPlugin: String = viewModel.getLastSelectedPlugin()
                val selectedPlugin: Plugin =
                    plugins.firstOrNull { it.name == lastPlugin } ?: plugins.first()

                // todo: record the item used in the preferences and reselect it at setup time
                (binding.pluginPicker.editText as? AutoCompleteTextView)?.setText(
                    selectedPlugin.name,
                    false
                )
                setupCategory(categoryPickerView, plugins.first())
            }

            // update the categories dropdown when the selected plugins change
            pluginPickerView?.setOnItemClickListener { _, _, position, _ ->
                val selection: String? = pluginAdapter.getItem(position)
                if (selection != null) {
                    binding.pluginPicker.hideKeyboard()
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
        binding.sortingButton.background = requireContext().getThemedDrawable(sortDrawableID)

        binding.sortingButton.setOnClickListener {
            showSortingPopup(it, R.menu.sorting_popup, adapter, binding.rvSearchList)
        }

        // load the latest results if coming back from another fragment
        val lastResults = viewModel.getSearchResults()
        if (lastResults.isNotEmpty())
            submitSortedList(adapter, lastResults)

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

        // search button listener
        binding.tfSearch.setEndIconOnClickListener {
            performSearch(binding, adapter)
        }

        viewModel.cacheLiveData.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                submitCachedList(it, adapter)
            }
        }
    }

    private fun showSortingPopup(
        v: View,
        @MenuRes menuRes: Int,
        searchAdapter: SearchItemAdapter,
        searchList: RecyclerView
    ) {

        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // todo: check if the theme is needed, in case use getSortDrawable and remove from the menu xml the icons
            v.background = menuItem.icon
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
            }
            // update the list and scroll it to the top
            submitSortedList(searchAdapter, viewModel.getSearchResults())
            lifecycleScope.launch {
                searchList.delayedScrolling(requireContext())
            }
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
        viewModel.completeSearch(
            query = binding.tiSearch.text.toString(),
            pluginName = binding.pluginPicker.editText?.text.toString(),
            category = getSelectedCategory(binding.categoryPicker.editText?.text.toString())
        ).observe(viewLifecycleOwner) { result ->
            when (result) {
                is ParserResult.SingleResult -> {
                    submitSortedList(
                        searchAdapter,
                        listOf(result.value)
                    )
                    searchAdapter.notifyDataSetChanged()
                }
                is ParserResult.Results -> {
                    submitSortedList(searchAdapter, result.values)
                    searchAdapter.notifyDataSetChanged()
                }
                is ParserResult.SearchStarted -> {
                    searchAdapter.submitList(emptyList())
                    binding.sortingButton.visibility = View.INVISIBLE
                    binding.loadingCircle.visibility = View.VISIBLE
                }
                is ParserResult.SearchFinished -> {
                    binding.loadingCircle.visibility = View.INVISIBLE
                    binding.sortingButton.visibility = View.VISIBLE
                    // update the data with cached results
                }
                is ParserResult.EmptyInnerLinks -> {
                    context?.showToast(R.string.no_links)
                    searchAdapter.submitList(emptyList())
                    binding.loadingCircle.visibility = View.INVISIBLE
                    binding.sortingButton.visibility = View.VISIBLE
                }
                else -> {
                    Timber.d("Unexpected result: $result")
                    searchAdapter.submitList(emptyList())
                    binding.loadingCircle.visibility = View.INVISIBLE
                    binding.sortingButton.visibility = View.VISIBLE
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
            else -> R.drawable.icon_sort_default
        }
    }

    private fun submitCachedList(cache: Set<String>, adapter: SearchItemAdapter) {
        // alternatively get results from the viewModel
        val items = adapter.currentList.map {
            it.apply {
                val btih = magnetPattern.find(it.magnets.first())?.groupValues?.get(1)?.uppercase()
                if (cache.contains(btih))
                    isCached = true
            }
        }
        submitSortedList(adapter, items)
        adapter.notifyDataSetChanged()
    }

    private fun submitSortedList(
        adapter: SearchItemAdapter,
        items: List<ScrapedItem>
    ) {
        when (viewModel.getListSortPreference()) {
            FolderListFragment.TAG_DEFAULT_SORT -> {
                adapter.submitList(items)
            }
            FolderListFragment.TAG_SORT_AZ -> {
                adapter.submitList(
                    items.sortedBy { item ->
                        item.name
                    }
                )
            }
            FolderListFragment.TAG_SORT_ZA -> {
                adapter.submitList(
                    items.sortedByDescending { item ->
                        item.name
                    }
                )
            }
            FolderListFragment.TAG_SORT_SIZE_DESC -> {
                adapter.submitList(
                    items.sortedByDescending { item ->
                        item.parsedSize
                    }
                )
            }
            FolderListFragment.TAG_SORT_SIZE_ASC -> {
                adapter.submitList(
                    items.sortedBy { item ->
                        item.parsedSize
                    }
                )
            }
            FolderListFragment.TAG_SORT_SEEDERS -> {
                adapter.submitList(
                    items.sortedByDescending { item ->
                        if (item.seeders != null) {
                            digitRegex.find(item.seeders)?.value?.toInt()
                        } else
                            null
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
            val alertDialog: AlertDialog? = activity?.let {
                val builder = AlertDialog.Builder(it)
                builder.apply {
                    setTitle(R.string.search_plugins)
                    setMessage(R.string.plugin_description_message)
                    setPositiveButton(R.string.open_github) { _, _ ->
                        viewModel.setPluginDialogNeeded(false)
                        // User clicked OK button
                        context.openExternalWebPage(PLUGINS_URL)
                    }
                    setNegativeButton(R.string.close) { _, _ ->
                        viewModel.setPluginDialogNeeded(false)
                    }
                }
                builder.create()
            }
            alertDialog?.show()
        }

        if (viewModel.isDOHDialogNeeded()) {
            val alertDialog: AlertDialog? = activity?.let {
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
        if (plugin.supportedCategories.anime != null)
            choices.add(getString(R.string.category_anime))
        if (plugin.supportedCategories.software != null)
            choices.add(getString(R.string.category_software))
        if (plugin.supportedCategories.games != null)
            choices.add(getString(R.string.category_games))
        if (plugin.supportedCategories.movies != null)
            choices.add(getString(R.string.category_movies))
        if (plugin.supportedCategories.music != null)
            choices.add(getString(R.string.category_music))
        if (plugin.supportedCategories.tv != null)
            choices.add(getString(R.string.category_tv))
        if (plugin.supportedCategories.books != null)
            choices.add(getString(R.string.category_books))

        val adapter = ArrayAdapter(requireContext(), R.layout.plugin_list_item, choices)
        autoCompleteView?.setAdapter(adapter)

        autoCompleteView?.setText(
            choices.first(),
            false
        )
    }

    private fun getSelectedCategory(pickerText: String): String? {
        return when (pickerText) {
            getString(R.string.category_all) -> {
                // "all"
                // searches on "all" will just be redirected to the no_category search
                null
            }
            getString(R.string.category_anime) -> "anime"
            getString(R.string.category_software) -> "software"
            getString(R.string.category_games) -> "games"
            getString(R.string.category_movies) -> "movies"
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
