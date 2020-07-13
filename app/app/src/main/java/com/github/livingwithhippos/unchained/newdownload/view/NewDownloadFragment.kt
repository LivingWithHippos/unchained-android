package com.github.livingwithhippos.unchained.newdownload.view

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.NewDownloadFragmentBinding
import com.github.livingwithhippos.unchained.newdownload.viewmodel.NewDownloadViewModel
import com.github.livingwithhippos.unchained.utilities.REMOTE_TRAFFIC_ON
import com.github.livingwithhippos.unchained.utilities.showToast
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

        viewModel.linkLiveData.observe(viewLifecycleOwner, Observer {
            //todo: navigate to download details fragment
            if (it != null) {
                val action =
                    NewDownloadFragmentDirections.actionUnrestrictDownloadToDetailsFragment(it)
                findNavController().navigate(action)
            }
        })
        // add the unrestrict button listener
        downloadBinding.bUnrestrict.setOnClickListener {
            val link: String = downloadBinding.etLink.text.toString()
            if (Patterns.WEB_URL.matcher(link).matches()) {

                downloadBinding.bUnrestrict.isEnabled = false

                var password: String? = downloadBinding.etPassword.text.toString()
                // we don't pass the password if it is blank.
                // N.B. it won't work if your password is made up of spaces but then again you deserve it
                if (password==null || password.isBlank())
                    password = null
                val remote: Int? = if (downloadBinding.dropdownItems.text.toString()
                        .equals(getString(R.string.yes), ignoreCase = true)
                ) REMOTE_TRAFFIC_ON else null

                viewModel.fetchUnrestrictedLink(
                    link,
                    password,
                    remote
                )

            } else
                showToast(R.string.invalid_url)

        }
        return downloadBinding.root
    }
}