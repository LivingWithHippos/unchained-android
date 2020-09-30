package com.github.livingwithhippos.unchained.lists.view

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.databinding.DialogDownloadItemBinding
import com.github.livingwithhippos.unchained.lists.viewmodel.TorrentDialogViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DownloadContextualDialogFragment: DialogFragment {

    private var item: DownloadItem? = null

    val viewModel: TorrentDialogViewModel by viewModels()

    constructor(item: DownloadItem) : super() {
        this.item = item
    }

    constructor() : super()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {


            // Use the Builder class for convenient dialog construction
            val builder = MaterialAlertDialogBuilder(it)

            // Get the layout inflater
            val inflater = it.layoutInflater

            val binding = DialogDownloadItemBinding.inflate(inflater)

            binding.bDelete.setOnClickListener {
                item?.let { download ->
                    setFragmentResult("downloadActionKey", bundleOf("deletedDownloadKey" to download.id))
                    dismiss()
                }
            }

            binding.bOpen.setOnClickListener {
                item?.let { download ->
                    setFragmentResult("downloadActionKey", bundleOf("openedDownloadItem" to download))
                    dismiss()
                }
            }

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
