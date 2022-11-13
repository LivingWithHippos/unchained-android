package com.github.livingwithhippos.unchained.torrentfilepicker.view

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.livingwithhippos.unchained.data.model.cache.CachedAlternative
import com.github.livingwithhippos.unchained.databinding.FragmentCacheBinding
import com.github.livingwithhippos.unchained.torrentfilepicker.model.CacheFileAdapter
import com.github.livingwithhippos.unchained.utilities.extension.setFileSize

class TorrentCacheListFragment() : Fragment() {

    private var cache: CachedAlternative? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            // it.classLoader = ?
            cache = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                it.getParcelable(CACHE_KEY, CachedAlternative::class.java)
            else
                it.getParcelable(CACHE_KEY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentCacheBinding.inflate(inflater, container, false)

        val listsAdapter = CacheFileAdapter()
        binding.rvCacheList.adapter = listsAdapter

        cache?.let { currCache ->
            listsAdapter.submitList(currCache.cachedFiles.sortedByDescending { it.fileSize })

            val totalSize: Long = currCache.cachedFiles.sumOf { it.fileSize }
            val totalFiles = currCache.cachedFiles.size.toString()

            binding.tvFilesNumber.text = totalFiles
            binding.tvTotalSize.setFileSize(totalSize)
        }

        return binding.root
    }

    companion object {

        private const val CACHE_KEY = "key_cache"

        @JvmStatic
        fun newInstance(cache: CachedAlternative) =
            TorrentCacheListFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(CACHE_KEY, cache)
                }
            }
    }
}
