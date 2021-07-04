package com.github.livingwithhippos.unchained.lists.view

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.DialogDownloadItemBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DownloadContextualDialogFragment : DialogFragment() {

    private val args: DownloadContextualDialogFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {

            // Use the Builder class for convenient dialog construction
            val builder = MaterialAlertDialogBuilder(it)

            // Get the layout inflater
            val inflater = it.layoutInflater

            val binding = DialogDownloadItemBinding.inflate(inflater)
            // don't show the delete confirmation at start
            binding.deleteConfirmation = false
            binding.download = args.download

            binding.bDelete.setOnClickListener {
                binding.deleteConfirmation = true
            }

            binding.bConfirmDelete.setOnClickListener {
                // partial workaround for navigation related crash
                dismiss()
                setFragmentResult(
                    "downloadActionKey",
                    bundleOf("deletedDownloadKey" to args.download.id)
                )
            }

            binding.bOpen.setOnClickListener {
                dismiss()
                setFragmentResult(
                    "downloadActionKey",
                    bundleOf("openedDownloadItem" to args.download)
                )
            }

            binding.bShare.setOnClickListener {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                val shareLink = args.download.download
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareLink)
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with)))
                dismiss()
            }

            builder.setView(binding.root)
                .setTitle(args.download.filename)
                .setNeutralButton(getString(R.string.close)) { dialog, _ ->
                    dialog.cancel()
                }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
