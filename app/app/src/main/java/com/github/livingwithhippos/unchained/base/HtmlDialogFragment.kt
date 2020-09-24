package com.github.livingwithhippos.unchained.base

import android.app.Dialog
import android.os.Bundle
import androidx.core.text.HtmlCompat
import android.widget.TextView
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.fragment.app.DialogFragment
import com.github.livingwithhippos.unchained.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * A [DialogFragment] subclass.
 * Parse the given text from html and displays it accordingly.
 */
class HtmlDialogFragment(private val title: Int, private val message: Int) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = MaterialAlertDialogBuilder(it)

            // Get the layout inflater
            val inflater = it.layoutInflater

            val view = inflater.inflate(R.layout.dialog_settings_plain, null)
            view.findViewById<TextView>(R.id.tvHeader).text = getString(title)
            view.findViewById<TextView>(R.id.tvMessage).text = HtmlCompat.fromHtml(getString(message), FROM_HTML_MODE_COMPACT)

            builder.setView(view)
                .setNeutralButton(resources.getString(R.string.close)) { dialog, _ ->
                    dialog.cancel()
                }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
