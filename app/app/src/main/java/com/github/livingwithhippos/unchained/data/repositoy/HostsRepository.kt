package com.github.livingwithhippos.unchained.data.repositoy

import com.github.livingwithhippos.unchained.data.local.HostRegexDao
import com.github.livingwithhippos.unchained.data.model.Host
import com.github.livingwithhippos.unchained.data.model.HostRegex
import com.github.livingwithhippos.unchained.data.remote.HostsApiHelper
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.inject.Inject


class HostsRepository @Inject constructor(
    private val hostsApiHelper: HostsApiHelper,
    private val hostRegexDao: HostRegexDao
) :
    BaseRepository() {

    suspend fun getHostsStatus(token: String): Host? {

        val hostResponse = safeApiCall(
            call = { hostsApiHelper.getHostsStatus("Bearer $token") },
            errorMessage = "Error Fetching Streaming Info"
        )

        return hostResponse;

    }

    /**
     * Gets the regexps to filter supported hosts from the network
     * @return the list of HostRegex from the network
     */
    private suspend fun getHostsRegexFromNetwork(addCustomRegexps: Boolean = true): List<HostRegex> {

        val hostResponse = safeApiCall(
            call = { hostsApiHelper.getHostsRegex() },
            errorMessage = "Error Fetching Hosts Regex"
        )
        val list = mutableListOf<HostRegex>()
        // add the regexps from the network
        hostResponse?.forEach {
            val regex = convertRegex(it)
            if (!regex.isBlank())
                list.add(HostRegex(regex))
        }
        if (addCustomRegexps) {
            // add the custom regexps
            CUSTOM_REGEXPS.forEach {
                val regex = convertRegex(it)
                if (!regex.isBlank())
                    list.add(HostRegex(regex))
            }
        }
        return list
    }

    /**
     * Gets the regexps to filter supported hosts from the db if any, otherwise tries to update them from the network
     * @return the list of HostRegex from the db or network, or an empty list
     */
    suspend fun getHostsRegex(): List<HostRegex> {

        var regexps = hostRegexDao.getAllCredentials()
        if (regexps.size < 10)
            regexps = updateHostsRegex()
        return regexps

    }

    /**
     * Gets the regexps to filter supported hosts from the network and saves them in the local database, deleting the old ones
     * @return the list of HostRegex saved in the database, or an empty list
     */
    private suspend fun updateHostsRegex(): List<HostRegex> {
        val newRegexps = getHostsRegexFromNetwork()
        if (newRegexps.size > 10) {
            hostRegexDao.deleteAll()
            hostRegexDao.insertAll(newRegexps)
            return newRegexps
        } else
            return emptyList()
    }

    /**
     * Converts the regex from the original received language (unknown) to the Java regex language
     * @return the new regex or an empty string if the pattern could not be compiled
     */
    private fun convertRegex(originalRegex: String): String {
        var newRegex = originalRegex
            .trim()
            .replace(
            "/(http|https):\\/\\/(\\w+\\.)?",
            "^https?:\\/\\/(www?\\d?\\.)?",
            ignoreCase = true
        )
        if (newRegex[newRegex.lastIndex] == "/"[0])
            // substring endIndex is not included
            newRegex = newRegex.substring(0,newRegex.lastIndex)+"$"
        try {
            Pattern.compile(newRegex)
        } catch (e: PatternSyntaxException) {
            newRegex = ""
        }
        return newRegex
    }

    companion object {
        // some of the converted host regexps are not enough, these are added to the db manually
        val CUSTOM_REGEXPS = arrayOf(
            "^https?:\\/\\/(www?\\d?\\.)?rapidgator\\.(net|asia)\\/file\\/[0-9a-z]{6,32}/([^(\\/| |\"|'|>|<|\\r\\n\\|\\r|\\n|:|\$)]+)\$"
        )
    }
}