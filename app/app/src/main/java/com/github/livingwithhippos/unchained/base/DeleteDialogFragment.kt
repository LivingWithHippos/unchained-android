package com.github.livingwithhippos.unchained.base

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.github.livingwithhippos.unchained.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeleteDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)

            val title = arguments?.getString("title") ?: getString(R.string.delete)
            builder.setMessage(R.string.confirm_item_removal_description)
                .setTitle(title)
                .setPositiveButton(R.string.delete) { _, _ ->
                    setFragmentResult("deleteActionKey", bundleOf("deleteConfirmation" to true))
                }
                .setNegativeButton(R.string.close) { dialog, _ ->
                    dialog.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
