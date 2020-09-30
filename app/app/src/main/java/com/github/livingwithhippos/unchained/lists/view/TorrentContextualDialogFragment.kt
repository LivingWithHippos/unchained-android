package com.github.livingwithhippos.unchained.lists.view

import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.TorrentItem
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

            val view = inflater.inflate(R.layout.dialog_torrent_item, null)

            val title = item?.filename ?: ""

            builder.setView(view)
                .setTitle(title)
                .setNeutralButton(resources.getString(R.string.close)) { dialog, _ ->
                    dialog.cancel()
                }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
