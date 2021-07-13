package com.github.livingwithhippos.unchained.data.local

import android.content.Context
import com.github.livingwithhippos.unchained.utilities.extension.smartList
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AssetsManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {

    private val SYSTEM_ASSETS_FOLDER = listOf("images", "webkit")

    private fun getAssetsPath(path: String): Array<String> {
        // on some phones the path works with / at the end, on others not.
        return appContext.assets.smartList(path) ?: emptyArray()
    }

    // TODO: check if this returns true also for empty directories
    fun isFile(path: String): Boolean {
        return appContext.assets.list(path).isNullOrEmpty()
    }

    /**
     * Returns a list of paths of the files with a certain extension in a certain folder
     *
     * @param fileType the file extension
     * @param folder the starting folder
     * @param skipSystemFolders skip certain system folders usually present in assets
     * @return a list of file paths
     */
    fun searchFiles(
        fileType: String,
        folder: String,
        skipSystemFolders: Boolean = true
    ): List<String> {
        val results: MutableList<String> = mutableListOf()
        val pathList = getAssetsPath(folder)

        for (path in pathList) {
            // skip if system folder
            if (skipSystemFolders && SYSTEM_ASSETS_FOLDER.contains(path))
                continue

            val newPath = if (folder == "") path else folder.plus("/").plus(path)
            // the path is a file
            if (isFile(newPath)) {
                // if the name is correct we add it to the list
                if (newPath.endsWith(fileType, ignoreCase = true))
                    results.add(newPath)
            }
        }

        return results
    }
}
