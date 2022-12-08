package com.github.livingwithhippos.unchained.utilities

import com.github.livingwithhippos.unchained.data.repository.PluginRepository


fun getRepositoryString(repository: String): String {
    return repository.hashCode().toString()
}

fun getPluginFilename(plugin: String): String {
    return plugin.replace(Regex("\\s+"),"")  + PluginRepository.TYPE_UNCHAINED
}