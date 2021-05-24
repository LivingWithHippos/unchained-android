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
import com.github.livingwithhippos.unchained.plugins.LinkData
import com.github.livingwithhippos.unchained.plugins.ParserResult
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

    fun setupCategory(plugin: Plugin) {
        val choices = mutableListOf<String>()
        choices.add(getString(R.string.category_all))
        if (plugin.supportedCategories.anime != null)
            choices.add(getString(R.string.category_anime))
        if (plugin.supportedCategories.anime != null)
            choices.add(getString(R.string.category_software))
        if (plugin.supportedCategories.anime != null)
            choices.add(getString(R.string.category_games))
        if (plugin.supportedCategories.anime != null)
            choices.add(getString(R.string.category_movies))
        if (plugin.supportedCategories.anime != null)
            choices.add(getString(R.string.category_music))
        if (plugin.supportedCategories.anime != null)
            choices.add(getString(R.string.category_tv))
        if (plugin.supportedCategories.anime != null)
            choices.add(getString(R.string.category_books))

        val adapter = ArrayAdapter(requireContext(), R.layout.plugin_list_item, choices)
        (binding.categoryPicker.editText as? AutoCompleteTextView)?.setAdapter(adapter)

        (binding.categoryPicker.editText as? AutoCompleteTextView)?.setText(
            choices.first(),
            false
        )
    }

    private fun setup() {
        // setup the plugin dropdown
        viewModel.pluginLiveData.observe(viewLifecycleOwner) { plugins ->
            val adapter =
                ArrayAdapter(requireContext(), R.layout.plugin_list_item, plugins.map { plugin ->
                    plugin.name
                })
            (binding.pluginPicker.editText as? AutoCompleteTextView)?.setAdapter(adapter)

            if (binding.pluginPicker.editText?.text.toString().isBlank()
                && plugins.isNotEmpty()
            ) {
                // select the first item of the list
                //todo: record the item used in the preferences and reselect it at setup time
                (binding.pluginPicker.editText as? AutoCompleteTextView)?.setText(
                    plugins.first().name,
                    false
                )
                setupCategory(plugins.first())
            }


            (binding.pluginPicker.editText as? AutoCompleteTextView)?.setOnItemClickListener { parent, view, position, id ->
                val selection: String? = adapter.getItem(position)
                if (selection != null)
                    setupCategory(plugins.first { it.name == selection })
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
                pluginName = getSelectedPlugin()
            ).observe(viewLifecycleOwner) { result ->
                when (result) {
                    is ParserResult.SingleResult -> {
                        adapter.submitList(listOf(result.value))
                        adapter.notifyDataSetChanged()
                    }
                    is ParserResult.Result -> {
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

    private fun getSelectedPlugin(): String {
        return binding.pluginPicker.editText?.text.toString()
    }

    override fun onClick(linkData: LinkData) {
        viewModel.stopSearch()
        val action = SearchFragmentDirections.actionSearchDestToSearchItemFragment(linkData)
        findNavController().navigate(action)
    }
}