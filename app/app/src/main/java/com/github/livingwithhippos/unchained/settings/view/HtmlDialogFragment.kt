package com.github.livingwithhippos.unchained.settings.view

import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.settings.viewmodel.HtmlDialogViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/** A [DialogFragment] subclass. Parse the given text from html and displays it accordingly. */
@AndroidEntryPoint
class HtmlDialogFragment : DialogFragment {

    private val viewModel: HtmlDialogViewModel by viewModels()

    private var title: Int? = null
    private var message: Int? = null

    constructor(title: Int, message: Int) : super() {
        this.title = title
        this.message = message
    }

    constructor() : super()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            if (title == null) title = viewModel.getTitleResource()
            else viewModel.setTitleResource(title)

            if (message == null) message = viewModel.getMessageResource()
            else viewModel.setMessageResource(message)

            // Use the Builder class for convenient dialog construction
            val builder = MaterialAlertDialogBuilder(it)

            // Get the layout inflater
            val inflater = it.layoutInflater

            val view = inflater.inflate(R.layout.dialog_settings_plain, null)
            view.findViewById<TextView>(R.id.tvHeader).text = getString(title ?: R.string.error)
            view.findViewById<TextView>(R.id.tvMessage).text =
                HtmlCompat.fromHtml(
                    getString(message ?: R.string.error_loading_dialog),
                    FROM_HTML_MODE_COMPACT
                )

            builder.setView(view).setNeutralButton(getString(R.string.close)) { dialog, _ ->
                dialog.cancel()
            }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
