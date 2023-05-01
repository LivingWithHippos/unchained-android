package com.github.livingwithhippos.unchained.settings.view

import android.app.Dialog
import android.os.Bundle
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.settings.viewmodel.SettingsViewModel
import com.github.livingwithhippos.unchained.utilities.DataBindingAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

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
            list.adapter = adapter
            adapter.submitList(getThemeList())


            builder
                .setView(view)
                .setNeutralButton(getString(R.string.close)) { dialog, _ -> dialog.cancel() }
                .setPositiveButton(getString(R.string.select)) { dialog, _ ->
                    dialog.cancel()
                    // todo
                }
                .setTitle(getString(R.string.themes))
            return builder.create()
        } else throw IllegalStateException("Activity for theme picker cannot be null")
    }

    override fun onThemeClick(item: ThemeItem) {
        TODO("Not yet implemented")
    }

    private fun getThemeList(): List<ThemeItem> {
        Timber.d("Seed one is ${R.color.one_seed}")
        return listOf(
            ThemeItem(R.style.Theme_Unchained_Material3_One, "Pink 01", ResourcesCompat.getColor(resources, R.color.one_seed, null)),
            ThemeItem(R.style.Theme_Unchained_Material3_Two, "White 01", ResourcesCompat.getColor(resources,R.color.two_seed, null)),
        )
    }
}

data class ThemeItem(
    val id: Int,
    val name: String,
    val seedColor: Int
)


class ThemePickerAdapter(listener: ThemePickListener) :
DataBindingAdapter<ThemeItem, ThemePickListener>(DiffCallback(), listener) {
    class DiffCallback : DiffUtil.ItemCallback<ThemeItem>() {
        override fun areItemsTheSame(oldItem: ThemeItem, newItem: ThemeItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ThemeItem, newItem: ThemeItem): Boolean =
            true
    }

    override fun getItemViewType(position: Int) = R.layout.item_theme_list
}

interface ThemePickListener {
    fun onThemeClick(item: ThemeItem)
}
