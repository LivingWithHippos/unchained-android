package com.github.livingwithhippos.unchained.search.view

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentSearchBinding
import com.github.livingwithhippos.unchained.plugins.ParserResult
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.plugins.model.ScrapedItem
import com.github.livingwithhippos.unchained.search.model.SearchItemAdapter
import com.github.livingwithhippos.unchained.search.model.SearchItemListener
import com.github.livingwithhippos.unchained.search.viewmodel.SearchViewModel
import com.github.livingwithhippos.unchained.utilities.PLUGINS_URL
import com.github.livingwithhippos.unchained.utilities.extension.hideKeyboard
import com.github.livingwithhippos.unchained.utilities.extension.openExternalWebPage
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SearchFragment : UnchainedFragment(), SearchItemListener {

    private val viewModel: SearchViewModel by viewModels()

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

        viewModel.pluginLiveData.observe(viewLifecycleOwner) { plugins ->
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

        // load the latest results if coming back from another fragment
        val lastResults = viewModel.getSearchResults()
        if (lastResults.isNotEmpty())
            adapter.submitList(lastResults)

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
                    adapter.submitList(listOf(result.value))
                    adapter.notifyDataSetChanged()
                }
                is ParserResult.Results -> {
                    adapter.submitList(result.values)
                    adapter.notifyDataSetChanged()
                }
                is ParserResult.SearchStarted -> {
                    binding.loadingCircle.visibility = View.VISIBLE
                }
                is ParserResult.SearchFinished -> {
                    binding.loadingCircle.visibility = View.INVISIBLE
                }
                is ParserResult.EmptyInnerLinks -> {
                    context?.showToast(R.string.no_links)
                    binding.loadingCircle.visibility = View.INVISIBLE
                }
                else -> {
                    Timber.d(result.toString())
                    binding.loadingCircle.visibility = View.INVISIBLE
                }
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
