package com.github.livingwithhippos.unchained.utilities

import com.github.livingwithhippos.unchained.data.repository.PluginRepository

fun getRepositoryString(repository: String): String {
    return repository.lowercase().hashCode().toString()
}

fun getPluginFilename(plugin: String): String {
    return plugin.replace(Regex("\\s+"), "") + PluginRepository.TYPE_UNCHAINED
}

fun getPluginFilenameFromUrl(url: String): String {
    return url.hashCode().toString() + PluginRepository.TYPE_UNCHAINED
}
