package com.github.livingwithhippos.unchained.search.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.viewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentSearchBinding
import com.github.livingwithhippos.unchained.plugins.LinkData
import com.github.livingwithhippos.unchained.search.model.SearchItemAdapter
import com.github.livingwithhippos.unchained.search.model.SearchItemListener
import com.github.livingwithhippos.unchained.search.viewmodel.SearchViewModel
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint

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
        viewModel.pluginLiveData.observe(viewLifecycleOwner) {
            val adapter = ArrayAdapter(requireContext(), R.layout.plugin_list_item, it.map {  plugin ->
                plugin.name
            })
            (binding.pluginPicker.editText as? AutoCompleteTextView)?.setAdapter(adapter)

            if (binding.pluginPicker.editText.toString().isNullOrBlank()
                && it.isNotEmpty()
            ) {
                // select the first item of the list
                //todo: record the item used in the preferences and reselect it at setup time
                // todo: fix
                (binding.pluginPicker.editText as? AutoCompleteTextView)?.setText(it.first().name, false);
            }
        }
        viewModel.fetchPlugins()

        // search button listener
        binding.tfSearch.setEndIconOnClickListener {
            viewModel.search(
                query = binding.tiSearch.text.toString(),
                pluginName = getSelectedPlugin()
            )
        }

        val adapter = SearchItemAdapter(this)
        binding.rvSearchList.adapter = adapter
        viewModel.resultLiveData.observe(viewLifecycleOwner) {
            adapter.submitList( it )
            adapter.notifyDataSetChanged()
        }
    }

    private fun getSelectedPlugin(): String {
        return binding.pluginPicker.editText?.text.toString()
    }

    override fun onClick(linkData: LinkData) {
        context?.showToast("Clicked ${linkData.name}")
    }
}