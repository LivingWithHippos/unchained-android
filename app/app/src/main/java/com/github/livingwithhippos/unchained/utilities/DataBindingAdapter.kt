package com.github.livingwithhippos.unchained.utilities

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.BR

// todo: test implementing class with Nothing as generic value to avoid passing listeners
/**
 * A [ListAdapter] subclass.
 * Allows for a generic list of items with data binding and an optional listener.
 */
abstract class DataBindingAdapter<T, U>(
    diffCallback: DiffUtil.ItemCallback<T>,
    val listener: U? = null
) :
    ListAdapter<T, DataBindingViewHolder<T, U>>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataBindingViewHolder<T, U> {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding =
            DataBindingUtil.inflate<ViewDataBinding>(layoutInflater, viewType, parent, false)
        return DataBindingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DataBindingViewHolder<T, U>, position: Int) =
        holder.bind(getItem(position), listener)
}

/**
 * A [PagingDataAdapter] subclass.
 * Allows for a generic list of items with data binding and Paging support and an optional listener.
 */
abstract class DataBindingPagingAdapter<T : Any, U>(
    diffCallback: DiffUtil.ItemCallback<T>,
    val listener: U? = null
) :
    PagingDataAdapter<T, DataBindingViewHolder<T, U>>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataBindingViewHolder<T, U> {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding =
            DataBindingUtil.inflate<ViewDataBinding>(layoutInflater, viewType, parent, false)
        return DataBindingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DataBindingViewHolder<T, U>, position: Int) {
        val repoItem = getItem(position)
        if (repoItem != null) {
            holder.bind(repoItem, listener)
        }
    }
}

class DataBindingViewHolder<T, U>(private val binding: ViewDataBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: T, listener: U?) {
        binding.setVariable(BR.item, item)
        if (listener != null)
            binding.setVariable(BR.listener, listener)
        binding.executePendingBindings()
    }
}

/**
 * A [PagingDataAdapter] subclass.
 * Allows for a generic list of items with data binding and Paging support and an optional listener.
 */
abstract class DataBindingPagingTrackedAdapter<T : Any, U>(
    diffCallback: DiffUtil.ItemCallback<T>,
    val listener: U? = null
) :
    PagingDataAdapter<T, DataBindingTrackedViewHolder<T, U>>(diffCallback) {

    var tracker: SelectionTracker<T>? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DataBindingTrackedViewHolder<T, U> {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding =
            DataBindingUtil.inflate<ViewDataBinding>(layoutInflater, viewType, parent, false)
        return DataBindingTrackedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DataBindingTrackedViewHolder<T, U>, position: Int) {
        val repoItem = getItem(position)
        if (repoItem != null) {
            holder.bind(repoItem, tracker?.isSelected(repoItem) ?: false, listener)
        }
    }
}

class DataBindingTrackedViewHolder<T, U>(private val binding: ViewDataBinding) :
    RecyclerView.ViewHolder(binding.root) {

    var mItem: T? = null

    fun bind(item: T, isSelected: Boolean, listener: U?) {
        binding.setVariable(BR.item, item)
        binding.setVariable(BR.isSelected, isSelected)
        mItem = item
        if (listener != null)
            binding.setVariable(BR.listener, listener)
        binding.executePendingBindings()
    }

    fun getItemDetails(): ItemDetailsLookup.ItemDetails<T> =
        object : ItemDetailsLookup.ItemDetails<T>() {
            override fun getPosition(): Int = layoutPosition
            override fun getSelectionKey(): T? = mItem
        }
}

class DataBindingDetailsLookup<T>(private val recyclerView: RecyclerView) :
    ItemDetailsLookup<T>() {
    override fun getItemDetails(event: MotionEvent): ItemDetails<T>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        if (view != null) {
            return (recyclerView.getChildViewHolder(view) as DataBindingTrackedViewHolder<T, *>)
                .getItemDetails()
        }
        return null
    }
}
