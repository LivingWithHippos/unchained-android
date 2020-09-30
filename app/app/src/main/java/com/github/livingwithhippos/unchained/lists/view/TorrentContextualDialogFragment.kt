package com.github.livingwithhippos.unchained.lists.view

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.databinding.DialogTorrentItemBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TorrentContextualDialogFragment : DialogFragment {

    private var item: TorrentItem? = null

    constructor(item: TorrentItem) : super() {
        this.item = item
    }

    constructor() : super() {
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {


            // Use the Builder class for convenient dialog construction
            val builder = MaterialAlertDialogBuilder(it)

            // Get the layout inflater
            val inflater = it.layoutInflater

            val binding = DialogTorrentItemBinding.inflate(inflater)

            val title = item?.filename ?: ""

            builder.setView(binding.root)
                .setTitle(title)
                .setNeutralButton(resources.getString(R.string.close)) { dialog, _ ->
                    dialog.cancel()
                }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
