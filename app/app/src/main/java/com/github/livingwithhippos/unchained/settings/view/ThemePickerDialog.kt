package com.github.livingwithhippos.unchained.settings.view

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.ItemThemeListBinding
import com.github.livingwithhippos.unchained.settings.viewmodel.SettingsViewModel
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
                requireContext().getThemeList().find { it.themeID == viewModel.getCurrentTheme() }
            if (currentTheme != null) {
                label.text = currentTheme.name
            }

            viewModel.themeLiveData.observe(this) {
                it.getContentIfNotHandled()?.let { theme ->
                    label.text = theme.name
                    viewModel.applyTheme()
                    context?.showToast(R.string.restart_to_apply)
                    dialog?.cancel()
                }
            }

            builder
                .setView(view)
                .setNeutralButton(getString(R.string.close)) { dialog, _ -> dialog.cancel() }
                .setTitle(getString(R.string.themes))
            return builder.create()
        } else throw IllegalStateException("Activity for theme picker cannot be null")
    }

    override fun onThemeClick(item: ThemeItem) {
        viewModel.selectTheme(item)
    }
}

/**
 * Data class for describing an available theme
 *
 * @param name: The name of the theme, shown tp the user
 * @param key: The key of the theme, used to save the default one in the preferences
 * @param nightMode: support for night mode. "auto" (both), "night", "day"
 * @param themeID: The themeID of the theme, used for resources
 * @param primaryColorID: The primary color
 * @param surfaceColorID: The surface color
 * @param primaryContainerColorID: The primary container color
 */
data class ThemeItem(
    val name: String,
    val key: String,
    val nightMode: String,
    @StyleRes val themeID: Int,
    @ColorInt val primaryColorID: Int,
    @ColorInt val surfaceColorID: Int,
    @ColorInt val primaryContainerColorID: Int,
)


class ThemePickerAdapter(private val listener: ThemePickListener) :
    ListAdapter<ThemeItem, ThemeViewHolder>(DiffCallback()) {


    class DiffCallback : DiffUtil.ItemCallback<ThemeItem>() {
        override fun areItemsTheSame(oldItem: ThemeItem, newItem: ThemeItem): Boolean =
            oldItem.key == newItem.key

        // content does not change on update
        override fun areContentsTheSame(
            oldItem: ThemeItem,
            newItem: ThemeItem
        ): Boolean = true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val binding =
            ItemThemeListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ThemeViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val item = getItem(position)
        holder.bindCell(item)
    }

    override fun getItemViewType(position: Int) = R.layout.item_theme_list
}

class ThemeViewHolder(
    private val binding: ItemThemeListBinding,
    private val listener: ThemePickListener
) : RecyclerView.ViewHolder(binding.root) {

    fun bindCell(item: ThemeItem) {
        binding.themeName.text = item.name
        binding.themeColor.topColor = item.primaryColorID
        binding.themeColor.bottomLeftColor = item.surfaceColorID
        binding.themeColor.bottomRightColor = item.primaryContainerColorID

        binding.clItemTheme.setOnClickListener { listener.onThemeClick(item) }
    }
}

interface ThemePickListener {
    fun onThemeClick(item: ThemeItem)
}
