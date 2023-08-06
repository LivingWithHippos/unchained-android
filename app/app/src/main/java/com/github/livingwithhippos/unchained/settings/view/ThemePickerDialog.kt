package com.github.livingwithhippos.unchained.settings.view

import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.settings.viewmodel.SettingsViewModel
import com.github.livingwithhippos.unchained.utilities.DataBindingAdapter
import com.github.livingwithhippos.unchained.utilities.extension.getThemeList
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ThemePickerDialog : DialogFragment(), ThemePickListener {

    private val viewModel: SettingsViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity != null) {
            // Use the Builder class for convenient dialog construction
            val builder = MaterialAlertDialogBuilder(requireActivity())

            // Get the layout inflater
            val inflater = requireActivity().layoutInflater

            val view = inflater.inflate(R.layout.dialog_theme_picker, null)
            val adapter = ThemePickerAdapter(this)
            val list = view.findViewById<RecyclerView>(R.id.themeList)
            val label = view.findViewById<TextView>(R.id.selectedTheme)
            list.adapter = adapter
            adapter.submitList(requireContext().getThemeList())

            val currentTheme =
                requireContext().getThemeList().find { it.id == viewModel.getCurrentTheme() }
            if (currentTheme != null) {
                label.text = currentTheme.name
            }

            viewModel.themeLiveData.observe(this) { label.text = it.name }

            builder
                .setView(view)
                .setNeutralButton(getString(R.string.close)) { dialog, _ -> dialog.cancel() }
                .setPositiveButton(getString(R.string.apply)) { dialog, _ ->
                    viewModel.applyTheme()
                    context?.showToast(R.string.restart_to_apply)
                    dialog.cancel()
                }
                .setTitle(getString(R.string.themes))
            return builder.create()
        } else throw IllegalStateException("Activity for theme picker cannot be null")
    }

    override fun onThemeClick(item: ThemeItem) {
        viewModel.selectTheme(item)
    }
}

data class ThemeItem(val id: Int, val name: String, val seedColor: Int)

class ThemePickerAdapter(listener: ThemePickListener) :
    DataBindingAdapter<ThemeItem, ThemePickListener>(DiffCallback(), listener) {
    class DiffCallback : DiffUtil.ItemCallback<ThemeItem>() {
        override fun areItemsTheSame(oldItem: ThemeItem, newItem: ThemeItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ThemeItem, newItem: ThemeItem): Boolean = true
    }

    override fun getItemViewType(position: Int) = R.layout.item_theme_list
}

interface ThemePickListener {
    fun onThemeClick(item: ThemeItem)
}
