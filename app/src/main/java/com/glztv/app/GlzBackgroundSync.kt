package com.glztv.app

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.glztv.app.data.EpgRepository
import com.glztv.app.data.PlaylistRepository
import com.glztv.app.data.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.glztv.app.data.createPermissiveOkHttpClient
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object GlzBackgroundSync {
    private const val UNIQUE_WORK = "glz-periodic-content-sync"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<GlzSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

class GlzSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val preferences = PreferencesRepository(applicationContext)
        val prefs = preferences.sharedPreferences
        val client = createPermissiveOkHttpClient()
        runCatching {
            GlzHubManager.sync(prefs, client)
            GlzHubManager.heartbeat(prefs, client)
            PlaylistRepository(applicationContext, preferences, client).load(forceRefresh = true)
            EpgRepository(applicationContext, preferences, client).load(forceRefresh = true)
            GithubUpdateManager.check(client)?.let { update ->
                prefs.edit()
                    .putString("background_update_version", update.version)
                    .putString("background_update_url", update.releaseUrl)
                    .apply()
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = {
                if (runAttemptCount < 4) Result.retry()
                else Result.failure()
            }
        )
    }

}
