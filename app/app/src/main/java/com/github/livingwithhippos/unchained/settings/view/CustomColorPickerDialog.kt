package com.github.livingwithhippos.unchained.settings.view

import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.customview.ColorWheelView
import com.github.livingwithhippos.unchained.utilities.PreferenceKeys
import com.github.livingwithhippos.unchained.utilities.extension.getThemeList
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint

/**
 * Lets the user pick any seed color from a color wheel, generating a full Material 3 palette
 * from it the same way Material You does from the wallpaper (via DynamicColorsOptions'
 * content-based source), instead of only choosing between the app's fixed color themes. There is
 * only one custom slot: applying a new color overwrites whatever was picked before.
 */
@AndroidEntryPoint
class CustomColorPickerDialog : BottomSheetDialogFragment() {

    private lateinit var preferences: SharedPreferences

    override fun onStart() {
        super.onStart()
        // wrap_content sizing on the sheet's own content frame is ambiguous with this dialog's
        // content and collapses far too short; force a full-height container instead, see #315
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
    ): View = inflater.inflate(R.layout.dialog_custom_color_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val colorWheel = view.findViewById<ColorWheelView>(R.id.colorWheel)
        val brightnessSlider = view.findViewById<Slider>(R.id.brightnessSlider)
        val colorPreview = view.findViewById<View>(R.id.colorPreview)
        val quickSelectContainer = view.findViewById<LinearLayout>(R.id.quickSelectContainer)

        val previewDrawable =
            GradientDrawable().apply { shape = GradientDrawable.OVAL }.also {
                colorPreview.background = it
            }
        fun updatePreview() {
            previewDrawable.setColor(colorWheel.selectedColor)
        }

        val currentTheme = requireContext().getThemeList().find { it.key == CUSTOM_THEME_KEY }
        val initialColor =
            currentTheme?.primaryColorID
                ?: preferences.getInt(
                    PreferenceKeys.Ui.CUSTOM_THEME_SEED_COLOR_KEY,
                    0xFF376B00.toInt(),
                )
        colorWheel.setColor(initialColor)
        brightnessSlider.value = colorWheel.value
        updatePreview()

        colorWheel.onColorChanged = { updatePreview() }
        brightnessSlider.addOnChangeListener { _, sliderValue, _ ->
            colorWheel.value = sliderValue
            updatePreview()
        }

        requireContext()
            .getThemeList()
            .filter { !it.isDynamic }
            .forEach { theme ->
                quickSelectContainer.addView(
                    createQuickSelectSwatch(theme.primaryColorID) {
                        colorWheel.setColor(theme.primaryColorID)
                        brightnessSlider.value = colorWheel.value
                        updatePreview()
                    }
                )
            }

        view.findViewById<MaterialButton>(R.id.cancelButton).setOnClickListener { dismiss() }
        view.findViewById<MaterialButton>(R.id.applyButton).setOnClickListener {
            // written directly and synchronously instead of going through
            // viewModel.selectTheme()+applyTheme(), which only works correctly when applyTheme()
            // runs from inside a themeLiveData observer (as ThemePickerDialog does): applyTheme()
            // reads themeLiveData.value, but selectTheme() posts to it asynchronously, so calling
            // both back to back here would read a stale value and silently fail to persist
            preferences.edit {
                putInt(PreferenceKeys.Ui.CUSTOM_THEME_SEED_COLOR_KEY, colorWheel.selectedColor)
                putInt(SettingsFragment.KEY_THEME_NEW, R.style.Theme_Unchained_Material3_DynamicCustom)
            }
            dismiss()
            activity?.recreate()
        }
    }

    private fun createQuickSelectSwatch(color: Int, onClick: () -> Unit): View {
        val sizePx = resources.getDimensionPixelSize(R.dimen.custom_color_swatch_size)
        val marginPx = resources.getDimensionPixelSize(R.dimen.custom_color_swatch_margin)
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply { marginEnd = marginPx }
            val swatchDrawable =
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
            background = swatchDrawable
            // plain Views are not reachable with a d-pad by default, so make the swatches
            // focusable and show a ring around the focused one for TV and keyboard users
            isFocusable = true
            setOnFocusChangeListener { v, hasFocus ->
                val strokeWidth =
                    if (hasFocus) v.resources.getDimensionPixelSize(R.dimen.focus_stroke_width)
                    else 0
                swatchDrawable.setStroke(
                    strokeWidth,
                    MaterialColors.getColor(v, com.google.android.material.R.attr.colorOnSurface),
                )
            }
            setOnClickListener { onClick() }
        }
    }
}
