package com.github.livingwithhippos.unchained.lists.view

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.DialogTorrentItemBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TorrentContextualDialogFragment : DialogFragment() {

    private val args: TorrentContextualDialogFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {

            // Use the Builder class for convenient dialog construction
            val builder = MaterialAlertDialogBuilder(it)

            // Get the layout inflater
            val inflater = it.layoutInflater

            val binding = DialogTorrentItemBinding.inflate(inflater)
            // don't show the delete confirmation at start
            binding.deleteConfirmation = false

            binding.torrent = args.torrent

            binding.bDelete.setOnClickListener {
                binding.deleteConfirmation = true
            }

            binding.bConfirmDelete.setOnClickListener {
                // note: if you call dismiss() after the setFragmentResult() the navigation
                // happens too quickly while this fragment is still on the navigation stack
                // and it will crash the app
                dismiss()
                setFragmentResult(
                    "torrentActionKey",
                    bundleOf("deletedTorrentKey" to args.torrent.id)
                )
            }

            binding.bOpen.setOnClickListener {
                dismiss()
                setFragmentResult(
                    "torrentActionKey",
                    bundleOf("openedTorrentItem" to args.torrent.id)
                )
            }

            binding.bDownload.setOnClickListener {
                dismiss()
                setFragmentResult(
                    "torrentActionKey",
                    bundleOf("downloadedTorrentItem" to args.torrent)
                )
            }

            builder.setView(binding.root)
                .setTitle(args.torrent.filename)
                .setNeutralButton(getString(R.string.close)) { dialog, _ ->
                    dialog.cancel()
                }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
