package com.example.data.news

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.store.LauncherPreferencesRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Battery-friendly WorkManager worker that periodically refreshes enabled RSS/Atom feeds
 * when a network connection is available and updates both disk cache and live [NewsFeedStore].
 */
class NewsSyncWorker(
  appContext: Context,
  workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val prefsRepo = LauncherPreferencesRepository(applicationContext)
    val newsRepo = NewsFeedRepository(applicationContext)
    val prefs = prefsRepo.preferencesFlow.first()

    if (!prefs.newsPersonalization.isCompleted || prefs.newsSources.none { it.isEnabled }) {
      return Result.success()
    }

    val outcome = newsRepo.refreshFeeds(prefs.newsSources)
    return if (outcome.success) {
      Result.success()
    } else if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
      Result.retry()
    } else {
      Result.failure()
    }
  }

  companion object {
    private const val MAX_RETRY_ATTEMPTS = 3
  }
}

object NewsSyncScheduler {
  private const val UNIQUE_PERIODIC_WORK_NAME = "nor_news_periodic_sync"
  private const val SYNC_INTERVAL_MINUTES = 30L

  fun schedulePeriodicSync(context: Context) {
    val constraints =
      Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    val periodicRequest =
      PeriodicWorkRequestBuilder<NewsSyncWorker>(
          repeatInterval = SYNC_INTERVAL_MINUTES,
          repeatIntervalTimeUnit = TimeUnit.MINUTES,
        )
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
        .build()

    runCatching {
      WorkManager.getInstance(context.applicationContext)
        .enqueueUniquePeriodicWork(
          UNIQUE_PERIODIC_WORK_NAME,
          ExistingPeriodicWorkPolicy.KEEP,
          periodicRequest,
        )
    }
  }
}
