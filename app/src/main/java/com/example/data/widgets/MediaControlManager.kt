package com.example.data.widgets

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.model.MediaPlaybackState
import com.example.service.NorNotificationListenerService
import com.example.service.NotificationAccessHelper
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real Android media controller for the Smart Music Widget:
 * - When Notification Access is enabled, observes live active [MediaController] sessions via
 *   [MediaSessionManager] for track title, artist, album artwork, playback state, and direct
 *   [MediaController.TransportControls].
 * - Without Notification Access, safely falls back to [AudioManager.isMusicActive] and hardware
 *   media key dispatch (`KEYCODE_MEDIA_PLAY_PAUSE`, `NEXT`, `PREVIOUS`) so controls always work.
 */
class MediaControlManager(
  private val appContext: Context,
) {
  private val mediaSessionManager: MediaSessionManager? =
    runCatching {
      appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    }.getOrNull()

  private val audioManager: AudioManager? =
    runCatching {
      appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }.getOrNull()

  private val listenerComponent =
    ComponentName(appContext, NorNotificationListenerService::class.java)

  private val _playbackState = MutableStateFlow(MediaPlaybackState())
  val playbackState: StateFlow<MediaPlaybackState> = _playbackState.asStateFlow()

  private var activeController: MediaController? = null
  private var isObservingSessions = false

  private val controllerCallback =
    object : MediaController.Callback() {
      override fun onPlaybackStateChanged(state: PlaybackState?) {
        publishFromController(activeController)
      }

      override fun onMetadataChanged(metadata: MediaMetadata?) {
        publishFromController(activeController)
      }

      override fun onSessionDestroyed() {
        refreshSessions()
      }
    }

  private val sessionsChangedListener =
    MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
      attachPreferredController(controllers.orEmpty())
    }

  fun refreshSessions() {
    val sessionManager = mediaSessionManager
    if (NotificationAccessHelper.isAccessGranted(appContext) && sessionManager != null) {
      if (!isObservingSessions) {
        runCatching {
          sessionManager.addOnActiveSessionsChangedListener(
            sessionsChangedListener,
            listenerComponent,
          )
          isObservingSessions = true
        }
      }
      val controllers =
        runCatching { sessionManager.getActiveSessions(listenerComponent) }
          .getOrNull()
      if (controllers != null) {
        attachPreferredController(controllers)
        return
      }
    } else {
      detachSessionListener()
    }

    // Fallback without Notification Access: report AudioManager music active state
    detachController()
    val playing = runCatching { audioManager?.isMusicActive == true }.getOrDefault(false)
    _playbackState.value =
      MediaPlaybackState(
        isPlaying = playing,
        hasActiveSession = false,
      )
  }

  fun stopObserving() {
    detachSessionListener()
    detachController()
  }

  fun togglePlayPause() {
    val controller = activeController
    if (controller != null) {
      val state = controller.playbackState?.state
      val isPlaying =
        state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
      runCatching {
        if (isPlaying) {
          controller.transportControls.pause()
        } else {
          controller.transportControls.play()
        }
      }
      publishFromController(controller)
      return
    }
    dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    val nextPlaying = !(_playbackState.value.isPlaying)
    _playbackState.value = _playbackState.value.copy(isPlaying = nextPlaying)
  }

  fun skipNext() {
    val controller = activeController
    if (controller != null) {
      runCatching { controller.transportControls.skipToNext() }
      return
    }
    dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
  }

  fun skipPrevious() {
    val controller = activeController
    if (controller != null) {
      runCatching { controller.transportControls.skipToPrevious() }
      return
    }
    dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
  }

  private fun dispatchMediaKey(keyCode: Int) {
    val am = audioManager ?: return
    val eventTime = SystemClock.uptimeMillis()
    runCatching {
      am.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
      am.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))
    }
  }

  private fun attachPreferredController(controllers: List<MediaController>) {
    val preferred =
      controllers.firstOrNull {
        val st = it.playbackState?.state
        st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_BUFFERING
      } ?: controllers.firstOrNull()

    if (preferred?.sessionToken != activeController?.sessionToken) {
      detachController()
      if (preferred != null) {
        activeController = preferred
        runCatching { preferred.registerCallback(controllerCallback) }
      }
    }
    publishFromController(activeController)
  }

  private fun publishFromController(controller: MediaController?) {
    if (controller == null) {
      val playing = runCatching { audioManager?.isMusicActive == true }.getOrDefault(false)
      _playbackState.value = MediaPlaybackState(isPlaying = playing, hasActiveSession = false)
      return
    }
    val metadata = runCatching { controller.metadata }.getOrNull()
    val state = runCatching { controller.playbackState?.state }.getOrNull()
    val isPlaying =
      state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING

    val title =
      metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
        .ifEmpty { metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)?.trim().orEmpty() }
    val artist =
      metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim().orEmpty()
        .ifEmpty {
          metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.trim().orEmpty()
        }
        .ifEmpty {
          metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)?.trim().orEmpty()
        }
    val rawArt =
      metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
    val safeArt = rawArt?.let { downscaleArtwork(it, MAX_ARTWORK_PX) }

    _playbackState.value =
      MediaPlaybackState(
        title = title,
        artist = artist,
        packageName = runCatching { controller.packageName }.getOrNull(),
        isPlaying = isPlaying,
        hasActiveSession = true,
        artwork = safeArt,
      )
  }

  private fun detachController() {
    activeController?.let { ctrl -> runCatching { ctrl.unregisterCallback(controllerCallback) } }
    activeController = null
  }

  private fun detachSessionListener() {
    if (!isObservingSessions) return
    runCatching { mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener) }
    isObservingSessions = false
  }

  private fun downscaleArtwork(src: Bitmap, maxDimPx: Int): ImageBitmap? =
    runCatching {
      if (src.isRecycled) return null
      val longest = max(src.width, src.height)
      if (longest <= maxDimPx) {
        src.asImageBitmap()
      } else {
        val ratio = maxDimPx.toFloat() / longest.toFloat()
        val w = (src.width * ratio).roundToInt().coerceAtLeast(1)
        val h = (src.height * ratio).roundToInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(src, w, h, true).asImageBitmap()
      }
    }.getOrNull()

  companion object {
    private const val MAX_ARTWORK_PX = 144

    fun openMediaPlayerApp(context: Context, preferredPackage: String?): Boolean {
      if (!preferredPackage.isNullOrBlank()) {
        val direct =
          context.packageManager.getLaunchIntentForPackage(preferredPackage)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        if (direct != null && runCatching { context.startActivity(direct) }.isSuccess) {
          return true
        }
      }

      val categoryIntent =
        Intent(Intent.ACTION_MAIN).apply {
          addCategory(Intent.CATEGORY_APP_MUSIC)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      return runCatching {
        context.startActivity(categoryIntent)
        true
      }.getOrElse { false }
    }
  }
}
