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
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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
import com.github.livingwithhippos.unchained.utilities.PLUGINS_URL
import com.github.livingwithhippos.unchained.utilities.extension.delayedScrolling
import com.github.livingwithhippos.unchained.utilities.extension.hideKeyboard
import com.github.livingwithhippos.unchained.utilities.extension.openExternalWebPage
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SearchFragment : UnchainedFragment(), SearchItemListener {

    private val viewModel: SearchViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_bar, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.plugins_link -> {
                openExternalWebPage(PLUGINS_URL)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSearchBinding.inflate(inflater, container, false)

        setup(binding)

        return binding.root
    }

    private fun setup(binding: FragmentSearchBinding) {
        showDialogIfNeeded()
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
                    setupCategory(categoryPickerView, plugins.first { it.name == selection })
                    viewModel.setLastSelectedPlugin(plugins.first { it.name == selection }.name)
                }
            }
        }

        viewModel.fetchPlugins(requireContext())

        val adapter = SearchItemAdapter(this)
        binding.rvSearchList.adapter = adapter

        val latestTag = viewModel.getListSortPreference()
        val drawableID = getSortingDrawable(latestTag)
        binding.sortingButton.tag = latestTag
        binding.sortingButton.background = ResourcesCompat.getDrawable(
            resources,
            drawableID,
            requireContext().theme
        )
        // load the latest results if coming back from another fragment
        val lastResults = viewModel.getSearchResults()
        if (lastResults.isNotEmpty())
            submitSortedList(latestTag, adapter, lastResults)

        binding.sortingButton.setOnClickListener {
            // every click changes to the next state
            val newTag = getNextSortingTag(it.tag as String)
            val currentDrawableID = getSortingDrawable(newTag)
            binding.sortingButton.tag = newTag
            binding.sortingButton.background = ResourcesCompat.getDrawable(
                resources,
                currentDrawableID,
                requireContext().theme
            )
            submitSortedList(newTag, adapter, viewModel.getSearchResults())
            viewModel.setListSortPreference(newTag)
            lifecycleScope.launch {
                binding.rvSearchList.delayedScrolling(requireContext())
            }
        }

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
    }

    private fun performSearch(binding: FragmentSearchBinding, adapter: SearchItemAdapter) {
        binding.tfSearch.hideKeyboard()
        viewModel.completeSearch(
            query = binding.tiSearch.text.toString(),
            pluginName = binding.pluginPicker.editText?.text.toString(),
            category = getSelectedCategory(binding.categoryPicker.editText?.text.toString())
        ).observe(viewLifecycleOwner) { result ->
            when (result) {
                is ParserResult.SingleResult -> {
                    submitSortedList(
                        binding.sortingButton.tag.toString(),
                        adapter,
                        listOf(result.value)
                    )
                    adapter.notifyDataSetChanged()
                }
                is ParserResult.Results -> {
                    submitSortedList(binding.sortingButton.tag.toString(), adapter, result.values)
                    adapter.notifyDataSetChanged()
                }
                is ParserResult.SearchStarted -> {
                    binding.sortingButton.visibility = View.INVISIBLE
                    binding.loadingCircle.visibility = View.VISIBLE
                }
                is ParserResult.SearchFinished -> {
                    binding.loadingCircle.visibility = View.INVISIBLE
                    binding.sortingButton.visibility = View.VISIBLE
                }
                is ParserResult.EmptyInnerLinks -> {
                    context?.showToast(R.string.no_links)
                    binding.loadingCircle.visibility = View.INVISIBLE
                    binding.sortingButton.visibility = View.VISIBLE
                }
                else -> {
                    Timber.d(result.toString())
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
            else -> R.drawable.icon_sort_default
        }
    }

    private fun getNextSortingTag(currentTag: String): String {
        return when (currentTag) {
            FolderListFragment.TAG_DEFAULT_SORT -> FolderListFragment.TAG_SORT_AZ
            FolderListFragment.TAG_SORT_AZ -> FolderListFragment.TAG_SORT_ZA
            FolderListFragment.TAG_SORT_ZA -> FolderListFragment.TAG_SORT_SIZE_DESC
            FolderListFragment.TAG_SORT_SIZE_DESC -> FolderListFragment.TAG_SORT_SIZE_ASC
            FolderListFragment.TAG_SORT_SIZE_ASC -> FolderListFragment.TAG_DEFAULT_SORT
            else -> FolderListFragment.TAG_DEFAULT_SORT
        }
    }

    private fun submitSortedList(
        tag: String,
        adapter: SearchItemAdapter,
        items: List<ScrapedItem>
    ) {
        when (tag) {
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
            else -> {
                adapter.submitList(items)
            }
        }
    }

    private fun showDialogIfNeeded() {
        if (viewModel.isDialogNeeded()) {
            val alertDialog: AlertDialog? = activity?.let {
                val builder = AlertDialog.Builder(it)
                builder.apply {
                    setTitle(R.string.search_plugins)
                    setMessage(R.string.plugin_description_message)
                    setPositiveButton(R.string.open_github) { _, _ ->
                        viewModel.setDialogNeeded(false)
                        // User clicked OK button
                        openExternalWebPage(PLUGINS_URL)
                    }
                    setNegativeButton(R.string.close) { _, _ ->
                        viewModel.setDialogNeeded(false)
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
}
