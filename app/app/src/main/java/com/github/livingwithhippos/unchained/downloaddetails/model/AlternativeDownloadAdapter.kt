package com.github.livingwithhippos.unchained.downloaddetails.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.Alternative
import com.github.livingwithhippos.unchained.databinding.ItemAlternativeDownloadBinding


class AlternativeDownloadAdapter(private val listener: DownloadDetailsListener) : ListAdapter<Alternative, AlternativeDownloadViewHolder>(DiffCallback()) {


    class DiffCallback : DiffUtil.ItemCallback<Alternative>() {
        override fun areItemsTheSame(oldItem: Alternative, newItem: Alternative): Boolean =
            oldItem.id == newItem.id

        // content does not change on update
        override fun areContentsTheSame(oldItem: Alternative, newItem: Alternative): Boolean = true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlternativeDownloadViewHolder {
        val binding = ItemAlternativeDownloadBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return AlternativeDownloadViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlternativeDownloadViewHolder, position: Int) {
        val alternative = getItem(position)
        holder.bindCell(alternative, listener)
    }
}

class AlternativeDownloadViewHolder(
    private val binding: ItemAlternativeDownloadBinding
) : RecyclerView.ViewHolder(binding.root){

    fun bindCell(item: Alternative, listener: DownloadDetailsListener){
        binding.streamingOverline1.text = item.quality ?: itemView.context.getString(R.string.alternative_link)
        binding.tvTitle.text = item.mimeType
        binding.bOpen.contentDescription = itemView.context.getString(R.string.open_link_format, item.mimeType)
        binding.bCopy.contentDescription = itemView.context.getString(R.string.copy_link_format, item.mimeType)
        binding.bStream.contentDescription = itemView.context.getString(R.string.open_with_format, item.mimeType)

        // todo: maybe move the listener to the constructor
        binding.bShare.setOnClickListener { listener.onShareClick(item.download) }
        binding.bOpen.setOnClickListener { listener.onOpenClick(item.download) }
        binding.bCopy.setOnClickListener { listener.onCopyClick(item.download) }
        binding.bStream.setOnClickListener { listener.onOpenTranscodedStream(it, item.download) }
    }

}

interface DownloadDetailsListener {
    fun onCopyClick(text: String)
    fun onOpenClick(url: String)
    fun onOpenTranscodedStream(view: View, url: String)
    fun onLoadStreamsClick(id: String)
    fun onBrowserStreamsClick(id: String)
    fun onDownloadClick(link: String, fileName: String)
    fun onShareClick(url: String)
    fun onSendToPlayer(url: String)
}