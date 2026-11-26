package com.example.data.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages user-selected wallpapers safely using Android's zero-permission Photo Picker URI:
 * - Copies & downscales selected photo into app-private storage (`filesDir/nor_custom_wallpaper.jpg`)
 *   so it survives device reboots without requiring any storage permissions.
 * - Optionally applies the photo directly to the Android home wallpaper via [WallpaperManager].
 */
class WallpaperRepository(
  private val context: Context,
) {

  private val customWallpaperFile: File
    get() = File(context.filesDir, CUSTOM_WALLPAPER_FILE_NAME)

  suspend fun importPhotoFromUri(uri: Uri): Boolean =
    withContext(Dispatchers.IO) {
      runCatching {
        val boundsOptions =
          BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, boundsOptions)
        } ?: return@withContext false

        val srcWidth = boundsOptions.outWidth
        val srcHeight = boundsOptions.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) return@withContext false

        val sampleSize = computeInSampleSize(srcWidth, srcHeight, MAX_WALLPAPER_DIMENSION)
        val decodeOptions =
          BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
          }

        val decoded =
          context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
          } ?: return@withContext false

        val scaled = scaleDownIfNeeded(decoded, MAX_WALLPAPER_DIMENSION)
        if (scaled !== decoded) {
          decoded.recycle()
        }

        val tempFile = File(context.filesDir, "$CUSTOM_WALLPAPER_FILE_NAME.tmp")
        FileOutputStream(tempFile).use { out ->
          scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        scaled.recycle()

        if (customWallpaperFile.exists()) {
          customWallpaperFile.delete()
        }
        tempFile.renameTo(customWallpaperFile)
      }.getOrElse { false }
    }

  suspend fun loadCustomWallpaperBitmap(): ImageBitmap? =
    withContext(Dispatchers.IO) {
      val file = customWallpaperFile
      if (!file.exists()) return@withContext null
      runCatching {
        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
      }.getOrNull()
    }

  suspend fun applyCustomPhotoToSystemWallpaper(): Boolean =
    withContext(Dispatchers.IO) {
      val file = customWallpaperFile
      if (!file.exists()) return@withContext false
      runCatching {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext false
        try {
          val manager = WallpaperManager.getInstance(context)
          manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
          true
        } finally {
          bitmap.recycle()
        }
      }.getOrElse { false }
    }

  fun openSystemWallpaperPicker(): Boolean =
    runCatching {
      val intent =
        Intent(Intent.ACTION_SET_WALLPAPER).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      true
    }.getOrElse { false }

  private fun computeInSampleSize(width: Int, height: Int, maxDim: Int): Int {
    var sample = 1
    var halfW = width / 2
    var halfH = height / 2
    while (halfW / sample >= maxDim || halfH / sample >= maxDim) {
      sample *= 2
    }
    return sample.coerceAtLeast(1)
  }

  private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
    val longest = max(bitmap.width, bitmap.height)
    if (longest <= maxDim) return bitmap
    val ratio = maxDim.toFloat() / longest.toFloat()
    val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
    val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
  }

  companion object {
    private const val CUSTOM_WALLPAPER_FILE_NAME = "nor_custom_wallpaper.jpg"
    private const val MAX_WALLPAPER_DIMENSION = 1920
  }
}
