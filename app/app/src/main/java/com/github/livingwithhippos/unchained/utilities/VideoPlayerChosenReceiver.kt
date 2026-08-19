package com.github.livingwithhippos.unchained.utilities

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import com.github.livingwithhippos.unchained.utilities.extension.parcelable

/** SharedPreferences key remembering the package name of the user's chosen video player. */
const val KEY_PREFERRED_VIDEO_PLAYER = "preferred_video_player_package"

/**
 * Receives the EXTRA_CHOSEN_COMPONENT broadcast fired by the IntentSender passed to
 * Intent.createChooser, and remembers the picked app as the preferred video player.
 *
 * This app manages that choice itself instead of relying on Android's own "always" mechanism:
 * undoing an "always" choice from a third party app requires clearPackagePreferredActivities,
 * which needs a signature level permission regular apps cannot hold, and on some devices (seen on
 * Android TV) the Settings screen meant to undo it does not work either, leaving no way back to
 * the picker. Remembering the choice ourselves means changing it is just clearing our own
 * preference, which always works.
 */
class VideoPlayerChosenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val chosen = intent.parcelable<ComponentName>(Intent.EXTRA_CHOSEN_COMPONENT) ?: return
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(KEY_PREFERRED_VIDEO_PLAYER, chosen.packageName)
            .apply()
    }
}
