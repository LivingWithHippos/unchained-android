package com.github.livingwithhippos.unchained.newdownload.model

import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.cache.CachedFile
import com.github.livingwithhippos.unchained.generated.callback.OnClickListener
import com.github.livingwithhippos.unchained.search.model.LinkItem
import com.github.livingwithhippos.unchained.search.model.LinkItemListener
import com.github.livingwithhippos.unchained.utilities.DataBindingAdapter
import com.github.livingwithhippos.unchained.utilities.DataBindingStaticAdapter

class CacheFileAdapter :
    DataBindingStaticAdapter<CachedFile>(
        DiffCallback()
    ) {
    class DiffCallback : DiffUtil.ItemCallback<CachedFile>() {
        override fun areItemsTheSame(oldItem: CachedFile, newItem: CachedFile): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: CachedFile, newItem: CachedFile): Boolean {
            return oldItem.fileName == newItem.fileName && oldItem.fileSize == newItem.fileSize
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_cache_file
}