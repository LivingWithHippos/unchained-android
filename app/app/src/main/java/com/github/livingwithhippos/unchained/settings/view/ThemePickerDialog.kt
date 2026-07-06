package com.github.livingwithhippos.unchained.settings.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.ItemThemeListBinding
import com.github.livingwithhippos.unchained.settings.viewmodel.SettingsViewModel
import com.github.livingwithhippos.unchained.utilities.extension.getThemeList
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * A bottom sheet instead of a plain alert dialog: it's the modern Material 3 pattern for picking
 * one of many options with a color preview (matching Android's own system theme picker), and it
 * correctly handles a long, scrollable list at any font scale, unlike a MaterialAlertDialogBuilder
 * custom view, whose height doesn't resolve reliably for scrollable content, see #315.
 */
@AndroidEntryPoint
class ThemePickerDialog : BottomSheetDialogFragment(), ThemePickListener {

    private val viewModel: SettingsViewModel by activityViewModels()
    private lateinit var adapter: ThemePickerAdapter

    override fun onStart() {
        super.onStart()
        // wrap_content sizing on the sheet's own content frame is ambiguous with a scrollable
        // RecyclerView inside, and collapses to just the title and close button; force a fixed,
        // generous height directly on its container instead of relying on it to size itself from
        // content. Has to happen here, not onCreateDialog(), since the sheet's internal container
        // isn't attached yet
        val bottomSheetContainer =
            dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return
        bottomSheetContainer.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        bottomSheetContainer.requestLayout()
        val behavior = (dialog as? BottomSheetDialog)?.behavior ?: return
        behavior.isFitToContents = false
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.dialog_theme_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val themes = requireContext().getThemeList()
        val currentTheme = themes.find { it.themeID == viewModel.getCurrentTheme() }

        val recyclerView = view.findViewById<RecyclerView>(R.id.themeList)
        adapter = ThemePickerAdapter(this, currentTheme?.key)
        recyclerView.adapter = adapter
        adapter.submitList(themes)

        view.findViewById<View>(R.id.closeButton).setOnClickListener { dismiss() }

        viewModel.themeLiveData.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { theme ->
                adapter.setSelectedKey(theme.key)
                viewModel.applyTheme()
                dismiss()
                activity?.recreate()
            }
        }
    }

    override fun onThemeClick(item: ThemeItem) {
        if (item.key == CUSTOM_THEME_KEY) {
            dismiss()
            CustomColorPickerDialog().show(parentFragmentManager, "CustomColorPickerDialogFragment")
        } else {
            viewModel.selectTheme(item)
        }
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
 * @param isDynamic: true if this theme's colors come from Android's dynamic color system instead
 *   of the fixed colors baked into themeID, either from the wallpaper or a user-picked seed color
 */
data class ThemeItem(
    val name: String,
    val key: String,
    val nightMode: String,
    @param:StyleRes val themeID: Int,
    @param:ColorInt val primaryColorID: Int,
    @param:ColorInt val surfaceColorID: Int,
    @param:ColorInt val primaryContainerColorID: Int,
    val isDynamic: Boolean = false,
)

/** [ThemeItem.key] of the user-customizable, seed-color-based dynamic theme. */
const val CUSTOM_THEME_KEY = "custom_theme"

class ThemePickerAdapter(private val listener: ThemePickListener, initialSelectedKey: String?) :
    ListAdapter<ThemeItem, ThemeViewHolder>(DiffCallback()) {

    private var selectedKey: String? = initialSelectedKey

    fun setSelectedKey(key: String) {
        selectedKey = key
        notifyDataSetChanged()
    }

    class DiffCallback : DiffUtil.ItemCallback<ThemeItem>() {
        override fun areItemsTheSame(oldItem: ThemeItem, newItem: ThemeItem): Boolean =
            oldItem.key == newItem.key

        // content does not change on update
        override fun areContentsTheSame(oldItem: ThemeItem, newItem: ThemeItem): Boolean = true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val binding =
            ItemThemeListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ThemeViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val item = getItem(position)
        holder.bindCell(item, item.key == selectedKey)
    }

    override fun getItemViewType(position: Int) = R.layout.item_theme_list
}

class ThemeViewHolder(
    private val binding: ItemThemeListBinding,
    private val listener: ThemePickListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bindCell(item: ThemeItem, isSelected: Boolean) {
        binding.themeName.text = item.name
        binding.themeColor.topColor = item.primaryColorID
        binding.themeColor.bottomLeftColor = item.surfaceColorID
        binding.themeColor.bottomRightColor = item.primaryContainerColorID
        binding.themeSelectedCheck.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

        binding.clItemTheme.setOnClickListener { listener.onThemeClick(item) }
    }
}

interface ThemePickListener {
    fun onThemeClick(item: ThemeItem)
}
