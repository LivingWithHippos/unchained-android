package com.github.livingwithhippos.unchained.data.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment
import timber.log.Timber

/**
 * Restarts [ForegroundTorrentService] after it stopped itself for hitting Android's foreground
 * service time limit (6 hours of `dataSync` type foreground time in a rolling 24 hour window,
 * enforced starting on API 35). Scheduled with a delay by the service itself instead of retrying
 * immediately, since none of that time limit will have freed up yet.
 */
class TorrentMonitoringRestartWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val monitoringStillEnabled =
            preferences.getBoolean(SettingsFragment.KEY_TORRENT_NOTIFICATIONS, false)

        if (!monitoringStillEnabled) {
            Timber.d("Torrent monitoring was disabled during the restart cooldown, skipping")
            return Result.success()
        }

        Timber.d("Restarting torrent monitoring after its time limit cooldown")
        ContextCompat.startForegroundService(
            applicationContext,
            Intent(applicationContext, ForegroundTorrentService::class.java),
        )
        return Result.success()
    }
}
