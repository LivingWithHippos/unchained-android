package com.github.livingwithhippos.unchained.lists.view

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.databinding.DialogTorrentItemBinding
import com.github.livingwithhippos.unchained.lists.viewmodel.TorrentDialogViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TorrentContextualDialogFragment : DialogFragment {

    private var item: TorrentItem? = null
    private var listener: TorrentDialogListener? = null

    val viewModel: TorrentDialogViewModel by viewModels()

    constructor(item: TorrentItem, listener: TorrentDialogListener) : super() {
        this.item = item
        this.listener = listener
    }

    constructor() : super()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {


            // Use the Builder class for convenient dialog construction
            val builder = MaterialAlertDialogBuilder(it)

            // Get the layout inflater
            val inflater = it.layoutInflater

            val binding = DialogTorrentItemBinding.inflate(inflater)

            binding.bDelete.setOnClickListener {
                item?.let { torrent ->
                    listener?.let {mListener ->
                        mListener.onDeleteTorrentClick(torrent.id)
                        dismiss()
                    }
                }
            }

            binding.bOpen.setOnClickListener {
                item?.let { torrent ->
                    listener?.let {mListener ->
                        mListener.onOpenTorrentClick(torrent.id)
                        dismiss()
                    }
                }
            }

            binding.bDownload.setOnClickListener {
                item?.let { torrent ->
                    listener?.let {mListener ->
                        mListener.onDownloadTorrentClick(torrent.links)
                        dismiss()
                    }
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

interface TorrentDialogListener {
    fun onDeleteTorrentClick(id: String)
    fun onOpenTorrentClick(id: String)
    fun onDownloadTorrentClick(id: List<String>)
}
