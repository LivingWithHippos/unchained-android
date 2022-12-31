package com.github.livingwithhippos.unchained.repository.view

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.repository.model.RepositoryListItem
import com.github.livingwithhippos.unchained.repository.viewmodel.PluginRepositoryEvent
import com.github.livingwithhippos.unchained.repository.viewmodel.RepositoryViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ManageRepositoryDialogFragment : DialogFragment() {

    private val viewModel: RepositoryViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {activity ->

            val repository = arguments?.getParcelable<RepositoryListItem.Repository>(REPOSITORY_KEY) ?: throw IllegalArgumentException("Repository cannot be null")

            // Use the Builder class for convenient dialog construction
            val builder = MaterialAlertDialogBuilder(activity)

            // Get the layout inflater
            val inflater = activity.layoutInflater

            val view = inflater.inflate(R.layout.dialog_manage_repository, null)

            val progressBar = view.findViewById<LinearProgressIndicator>(R.id.progressBar)
            view.findViewById<TextView>(R.id.tvName).text = repository.name
            view.findViewById<TextView>(R.id.tvAuthor).text = repository.author
            view.findViewById<TextView>(R.id.tvDescription).text = repository.description

            view.findViewById<Button>(R.id.bInstallAll).setOnClickListener {
                progressBar.isIndeterminate = true
                viewModel.installAllRepositoryPlugins(activity, repository)
            }
            view.findViewById<Button>(R.id.bUpdateAll).setOnClickListener {
                progressBar.isIndeterminate = true
                viewModel.updateAllRepositoryPlugins(activity, repository)
            }
            view.findViewById<Button>(R.id.bUninstallAll).setOnClickListener {
                viewModel.uninstallAllRepositoryPlugins(activity, repository)
            }
            view.findViewById<Button>(R.id.bUninstallRepo).setOnClickListener {
                viewModel.uninstallRepository(activity, repository)
            }

            builder.setView(view)
                .setNeutralButton(getString(R.string.close)) { dialog, _ ->
                    dialog.cancel()
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
            // todo: check if this gets triggered every time we open the dialog
            when (it.peekContent()) {
                is PluginRepositoryEvent.MultipleInstallation -> this.dismiss()
                is PluginRepositoryEvent.Uninstalled -> this.dismiss()
                else -> {
                    // is PluginRepositoryEvent.Installation -> TODO()
                    // PluginRepositoryEvent.Updated -> TODO()
                    // is PluginRepositoryEvent.FullData -> TODO()
                    // do nothing
                }
            }
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }


    companion object {
        const val REPOSITORY_KEY = "key_repository"
    }
}