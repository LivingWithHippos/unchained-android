package com.github.livingwithhippos.unchained.search.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentSearchBinding
import com.github.livingwithhippos.unchained.plugins.ParserResult
import com.github.livingwithhippos.unchained.plugins.ScrapedItem
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.search.model.SearchItemAdapter
import com.github.livingwithhippos.unchained.search.model.SearchItemListener
import com.github.livingwithhippos.unchained.search.viewmodel.SearchViewModel
import com.github.livingwithhippos.unchained.utilities.extension.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SearchFragment : UnchainedFragment(), SearchItemListener {

    private var _binding: FragmentSearchBinding? = null
    val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)

        setup()

        return binding.root
    }

    private fun setup() {
        // setup the plugin dropdown
        val pluginAdapter = ArrayAdapter(requireContext(), R.layout.plugin_list_item, arrayListOf<String>())
        (binding.pluginPicker.editText as? AutoCompleteTextView)?.setAdapter(pluginAdapter)

        viewModel.pluginLiveData.observe(viewLifecycleOwner) { plugins ->
            pluginAdapter.clear()
            pluginAdapter.addAll(plugins.map { it.name })

            if (binding.pluginPicker.editText?.text.toString().isBlank()
                && plugins.isNotEmpty()
            ) {
                // load the latest selected plugin or the first one available
                val lastPlugin: String = viewModel.getLastSelectedPlugin()
                val selectedPlugin: Plugin = plugins.firstOrNull{ it.name == lastPlugin} ?: plugins.first()

                //todo: record the item used in the preferences and reselect it at setup time
                (binding.pluginPicker.editText as? AutoCompleteTextView)?.setText(
                    selectedPlugin.name,
                    false
                )
                setupCategory(plugins.first())
            }

            // update the categories dropdown when the selected plugins change
            (binding.pluginPicker.editText as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
                val selection: String? = pluginAdapter.getItem(position)
                if (selection != null) {
                    setupCategory(plugins.first { it.name == selection })
                    viewModel.setLastSelectedPlugin(plugins.first { it.name == selection }.name)
                }
            }
        }

        viewModel.fetchPlugins()

        val adapter = SearchItemAdapter(this)
        binding.rvSearchList.adapter = adapter

        // load the latest results if coming back from another fragment
        val lastResults = viewModel.getSearchResults()
        if (lastResults.isNotEmpty())
            adapter.submitList(lastResults)

        // search button listener
        binding.tfSearch.setEndIconOnClickListener {
            it.hideKeyboard()
            viewModel.completeSearch(
                query = binding.tiSearch.text.toString(),
                pluginName = getSelectedPluginName(),
                category = getSelectedCategory()
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
                    else -> {
                        Timber.d(result.toString())
                        binding.loadingCircle.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }


    fun setupCategory(plugin: Plugin) {
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
        (binding.categoryPicker.editText as? AutoCompleteTextView)?.setAdapter(adapter)

        (binding.categoryPicker.editText as? AutoCompleteTextView)?.setText(
            choices.first(),
            false
        )
    }

    private fun getSelectedCategory(): String? {
        return when (binding.categoryPicker.editText?.text.toString()) {
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

    private fun getSelectedPluginName(): String {
        return binding.pluginPicker.editText?.text.toString()
    }

    private fun getSelectedPlugin(): Plugin? {
        return viewModel.getPlugins()
            .firstOrNull { it.name == getSelectedPluginName() }
    }

    override fun onClick(item: ScrapedItem) {
        viewModel.stopSearch()
        val action = SearchFragmentDirections.actionSearchDestToSearchItemFragment(item)
        findNavController().navigate(action)
    }
}