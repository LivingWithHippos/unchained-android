package com.github.livingwithhippos.unchained.lists.view

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.databinding.DialogDownloadItemBinding
import com.github.livingwithhippos.unchained.lists.viewmodel.DownloadDialogViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DownloadContextualDialogFragment: DialogFragment {

    private var item: DownloadItem? = null

    val viewModel: DownloadDialogViewModel by viewModels()

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
            // don't show the delete confirmation at start
            binding.deleteConfirmation = false

            var title = ""
            item?.let { item ->
                title = item.filename
                viewModel.setItem(item)
            }

            if (item==null) {
                item = viewModel.getItem()
                title = item?.filename ?: ""
            }

            binding.bDelete.setOnClickListener {
                binding.deleteConfirmation = true
            }

            binding.bConfirmDelete.setOnClickListener {
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

            binding.bShare.setOnClickListener {
                item?.let { item ->
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    val shareLink = item.download
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareLink)
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with)))
                    dismiss()
                }
            }

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
