package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.HostRegexDao
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.HostRegex
import com.github.livingwithhippos.unchained.data.model.REGEX_TYPE_FOLDER
import com.github.livingwithhippos.unchained.data.model.REGEX_TYPE_HOST
import com.github.livingwithhippos.unchained.data.remote.HostsApiHelper
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.inject.Inject

class HostsRepository
@Inject
constructor(
    protoStore: ProtoStore,
    private val hostsApiHelper: HostsApiHelper,
    private val hostRegexDao: HostRegexDao
) : BaseRepository(protoStore) {

    /**
     * Gets the regexps to filter supported hosts from the network. Custom regexps are also added
     * here.
     *
     * @return the list of HostRegex from the network
     */
    private suspend fun getHostsRegexFromNetwork(): List<HostRegex> {

        val hostResponse =
            safeApiCall(
                call = { hostsApiHelper.getHostsRegex() },
                errorMessage = "Error Fetching Hosts Regex")
        val list = mutableListOf<HostRegex>()
        // add the regexps from the network
        hostResponse?.forEach {
            val regex = convertRegex(it)
            if (regex.isNotBlank()) list.add(HostRegex(regex, type = REGEX_TYPE_HOST))
        }
        return list
    }

    /**
     * Gets the regexps to filter supported hosts folders from the network.
     *
     * @return the list of HostRegex from the network
     */
    private suspend fun getFoldersRegexFromNetwork(): List<HostRegex> {

        val hostResponse =
            safeApiCall(
                call = { hostsApiHelper.getHostsFoldersRegex() },
                errorMessage = "Error Fetching Hosts Folders Regex")
        val list = mutableListOf<HostRegex>()
        // add the regexps from the network
        hostResponse?.forEach {
            val regex = convertRegex(it)
            if (regex.isNotBlank()) list.add(HostRegex(regex, type = REGEX_TYPE_FOLDER))
        }
        return list
    }

    /**
     * Gets the regexps to filter supported hosts from the db if any, otherwise tries to update them
     * from the network
     *
     * @return the list of HostRegex from the db or network, or an empty list
     */
    suspend fun getHostsRegex(addCustomRegexps: Boolean = true): List<HostRegex> {

        val regexps = mutableListOf<HostRegex>()
        regexps.addAll(hostRegexDao.getHostRegexps())
        if (regexps.size < 10) {
            regexps.clear()
            regexps.addAll(updateHostsRegex())
        }

        // add the custom regexps
        if (addCustomRegexps) regexps.addAll(CUSTOM_REGEXPS.map { HostRegex(it) })

        return regexps
    }

    /**
     * Gets the regexps to filter supported hosts from the network and saves them in the local
     * database, deleting the old ones
     *
     * @return the list of HostRegex saved in the database, or an empty list
     */
    suspend fun updateHostsRegex(): List<HostRegex> {
        val newRegexps = getHostsRegexFromNetwork()
        if (newRegexps.size > 10) {
            hostRegexDao.deleteAllHosts()
            hostRegexDao.insertAll(newRegexps)
            return newRegexps
        } else return emptyList()
    }

    /**
     * Gets the regexps to filter supported hosts from the db if any, otherwise tries to update them
     * from the network
     *
     * @return the list of HostRegex from the db or network, or an empty list
     */
    suspend fun getFoldersRegex(addCustomRegexps: Boolean = true): List<HostRegex> {

        val regexps = mutableListOf<HostRegex>()
        regexps.addAll(hostRegexDao.getFoldersRegexps())
        if (regexps.size < 10) {
            regexps.clear()
            regexps.addAll(updateFoldersRegex())
        }

        // add the custom regexps
        if (addCustomRegexps) regexps.addAll(CUSTOM_FOLDER_REGEXPS.map { HostRegex(it) })

        return regexps
    }

    /**
     * Gets the regexps to filter supported hosts from the network and saves them in the local
     * database, deleting the old ones
     *
     * @return the list of HostRegex saved in the database, or an empty list
     */
    suspend fun updateFoldersRegex(): List<HostRegex> {
        val newRegexps = getFoldersRegexFromNetwork()
        if (newRegexps.size > 10) {
            hostRegexDao.deleteAllFolders()
            hostRegexDao.insertAll(newRegexps)
            return newRegexps
        } else return emptyList()
    }

    /**
     * Converts the regex from the original received language (unknown) to the Java regex language
     *
     * @return the new regex or an empty string if the pattern could not be compiled
     */
    private fun convertRegex(originalRegex: String): String {
        var newRegex =
            originalRegex
                .trim()
                .replace("/(http|https):\\/\\/", "^https?:\\/\\/", ignoreCase = true)
        if (newRegex[newRegex.lastIndex] == "/"[0]) {
            // substring endIndex is not included
            newRegex = newRegex.substring(0, newRegex.lastIndex) + "$"
        }
        try {
            Pattern.compile(newRegex)
        } catch (e: PatternSyntaxException) {
            newRegex = ""
        }
        return newRegex
    }

    companion object {
        // some of the converted host regexps are not enough, these are added to the db manually
        val CUSTOM_REGEXPS =
            arrayOf(
                "^https?:\\/\\/(www?\\d?\\.)?rapidgator\\.(net|asia)\\/file\\/[0-9a-z]{6,32}/([^(\\/| |\"|'|>|<|\\r\\n\\|\\r|\\n|:|\$)]+)\$",
                "^(https?:\\/\\/)?(www?\\d?\\.)?(m\\.)?youtu(be)?\\.(com|be)\\/([^(|\"|'|>|<|\\s|\\r\\n\\|\\r|\\n|:|\$)]+)\$",
                "^(https?://)?(www?\\d?\\.)?uploadgig\\.com/file/download/\\w{16}/[^\\s]+\$",
                "^(https?://)?(www?\\d?\\.)?dropapk\\.to/\\w+/[^\\s]+\$",
                "^(https?://)?(www?\\d?\\.)?katfile\\.com/\\w+/[^\\s]+\$",
                "^(https?://)?(www?\\d?\\.)?clicknupload\\.cc/\\w+/[^\\s]+\$",
                "^(https?://)?(www?\\d?\\.)?fastclick\\.to/\\w+/[^\\s]+\$",
                "^(https?://)?(www?\\d?\\.)?drop\\.download/\\w+/[^\\s]+\$")

        // if any of the converted folder regexps are wrong, we can add these to the db manually
        val CUSTOM_FOLDER_REGEXPS = emptyArray<String>()
    }
}
