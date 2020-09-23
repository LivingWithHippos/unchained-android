package com.github.livingwithhippos.unchained.settings

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.github.livingwithhippos.unchained.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsDialogFragment(private val title: Int, private val message: Int) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = MaterialAlertDialogBuilder(it)
            builder.setTitle(title)
                .setMessage(message)
                .setNeutralButton(resources.getString(R.string.close)) { dialog, _ ->
                    dialog.cancel()
                }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
