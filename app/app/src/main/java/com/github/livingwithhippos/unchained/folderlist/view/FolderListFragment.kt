package com.github.livingwithhippos.unchained.folderlist.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.FragmentFolderListBinding
import com.github.livingwithhippos.unchained.databinding.FragmentTabListsBinding
import com.github.livingwithhippos.unchained.folderlist.viewmodel.FolderListViewModel
import com.github.livingwithhippos.unchained.lists.viewmodel.DownloadListViewModel


class FolderListFragment : Fragment() {

    private val viewModel: FolderListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val folderBinding = FragmentFolderListBinding.inflate(inflater, container, false)

        return folderBinding.root
    }
}