package com.github.livingwithhippos.unchained.repository.view

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.repository.viewmodel.InvalidLinkReason
import com.github.livingwithhippos.unchained.repository.viewmodel.PluginRepositoryEvent
import com.github.livingwithhippos.unchained.repository.viewmodel.RepositoryViewModel
import com.github.livingwithhippos.unchained.utilities.extension.isWebUrl
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddRepositoryDialogFragment : DialogFragment() {

    private val viewModel: RepositoryViewModel by activityViewModels()

    private lateinit var progressBar: LinearProgressIndicator

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { activity ->

            // Use the Builder class for convenient dialog construction
            val builder = MaterialAlertDialogBuilder(activity)

            // Get the layout inflater
            val inflater = activity.layoutInflater

            val view = inflater.inflate(R.layout.dialog_add_repository, null)

            progressBar = view.findViewById(R.id.progressBar)

            view.findViewById<Button>(R.id.bTestRepoLink).setOnClickListener {
                progressBar.isIndeterminate = true
                viewModel.checkRepositoryLink(
                    view.findViewById<TextInputEditText>(R.id.tiAdd).text.toString()
                )
            }

            builder.setView(view)
                .setNegativeButton(getString(R.string.close)) { dialog, _ ->
                    dialog.cancel()
                }
                .setPositiveButton(getString(R.string.save)) { dialog, _ ->
                    val url = view.findViewById<TextInputEditText>(R.id.tiAdd).text.toString().trim()
                    if (url.isWebUrl()) {
                        viewModel.addRepository(
                            view.findViewById<TextInputEditText>(R.id.tiAdd).text.toString()
                        )
                        dialog.cancel()
                    } else
                        context?.showToast(R.string.invalid_url)
                }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewModel.pluginsRepositoryLiveData.observe(this) {
            when (val result = it.peekContent()) {
                is PluginRepositoryEvent.ValidRepositoryLink -> {
                    progressBar.isIndeterminate = false
                    context?.showToast(R.string.connection_successful)
                }
                is PluginRepositoryEvent.InvalidRepositoryLink -> {
                    progressBar.isIndeterminate = false
                    when(result.reason) {
                        InvalidLinkReason.ConnectionError -> context?.showToast(R.string.network_error)
                        InvalidLinkReason.NotAnUrl -> context?.showToast(R.string.invalid_url)
                        InvalidLinkReason.ParsingError -> context?.showToast(R.string.parsing_error)
                    }
                }
                else -> {
                    // do nothing
                }
            }
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }
}