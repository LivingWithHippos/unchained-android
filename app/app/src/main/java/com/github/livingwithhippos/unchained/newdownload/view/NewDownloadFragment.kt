package com.github.livingwithhippos.unchained.newdownload.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.NewDownloadFragmentBinding
import com.github.livingwithhippos.unchained.newdownload.viewmodel.NewDownloadViewModel
import com.github.livingwithhippos.unchained.utilities.REMOTE_TRAFFIC_OFF
import com.github.livingwithhippos.unchained.utilities.REMOTE_TRAFFIC_ON
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewDownloadFragment : Fragment() {

    private val viewModel: NewDownloadViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val downloadBinding = NewDownloadFragmentBinding.inflate(inflater, container, false)

        // see AutoCompleteTextView.setAdapter(contents: List<String>) in [Extension.kt]
        // the first value is used as default, see AutoCompleteTextView's android:text="@{list[0]}" in the layout
        downloadBinding.list = listOf(getString(R.string.no), getString(R.string.yes))
        // add the unrestrict button listener
        downloadBinding.bUnrestrict.setOnClickListener {
            //todo: check acceptable values and convert them (check blank and not url etc.)
            val link: String = downloadBinding.etLink.text.toString()
            val password: String? = downloadBinding.etPassword.text.toString()
            val remote: Int = if (downloadBinding.dropdownItems.text.toString()
                    .equals(getString(R.string.yes), ignoreCase = true)
            ) REMOTE_TRAFFIC_ON else REMOTE_TRAFFIC_OFF

            viewModel.fetchUnrestrictedLink(
                link,
                password,
                remote
            )
        }
        return downloadBinding.root
    }

}