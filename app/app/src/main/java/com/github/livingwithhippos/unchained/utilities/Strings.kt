package com.github.livingwithhippos.unchained.utilities

import com.github.livingwithhippos.unchained.data.repository.PluginRepository

val noWordDigitRegex = Regex("[^\\w\\d]+")

/**
 * Return a hashed string from the repository link, to be used as a folder name in the plugins
 * directory. For manually installed plugins the name is not hashed but
 * [MANUAL_PLUGINS_REPOSITORY_NAME] is used instead
 *
 * @param repository
 * @return
 */
fun getRepositoryString(repository: String): String {
    return if (repository == MANUAL_PLUGINS_REPOSITORY_NAME) MANUAL_PLUGINS_REPOSITORY_NAME
    else repository.lowercase().hashCode().toString()
}

fun getPluginFilename(plugin: String): String {
    // replace all characters which are not letters or numbers
    return plugin.lowercase().replace(noWordDigitRegex, "") + PluginRepository.TYPE_UNCHAINED
}

/**
 * Get a file name for a plugin installed manually and not from a repo
 *
 * @param plugin
 * @return
 */
fun getManualPluginFilename(author: String?, name: String): String {
    return (author?.lowercase()?.replace(noWordDigitRegex, "")
        ?: "unknownAuthor") +
        "_" +
        name.lowercase().replace(noWordDigitRegex, "") +
        PluginRepository.TYPE_UNCHAINED
}
