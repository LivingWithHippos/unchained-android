package com.github.livingwithhippos.unchained.base

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.github.livingwithhippos.unchained.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeleteDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {


            // Use the Builder class for convenient dialog construction
            val builder = MaterialAlertDialogBuilder(it)

            // Get the layout inflater
            val inflater = it.layoutInflater

            val view = inflater.inflate(R.layout.dialog_delete_confirmation, null)

            view.findViewById<Button>(R.id.bConfirmDelete).setOnClickListener {
                setFragmentResult("deleteActionKey", bundleOf("deleteConfirmation" to true))
                dismiss()
            }

            builder.setView(view)
                .setTitle(R.string.confirm_removal)
                .setNeutralButton(getString(R.string.close)) { dialog, _ ->
                    dialog.cancel()
                }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}