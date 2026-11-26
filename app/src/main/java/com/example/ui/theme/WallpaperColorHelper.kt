package com.example.ui.theme

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils

/**
 * Extracts live accent colors from the user's active system wallpaper using
 * [WallpaperManager.getWallpaperColors] (API 27+, zero storage permissions required)
 * and reacts dynamically via [WallpaperManager.OnColorsChangedListener].
 */
@Composable
fun rememberWallpaperColorScheme(
  darkTheme: Boolean,
  fallbackDark: ColorScheme,
  fallbackLight: ColorScheme,
): ColorScheme {
  val context = LocalContext.current

  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
    return if (darkTheme) fallbackDark else fallbackLight
  }

  var wallpaperColors by remember(context) {
    mutableStateOf(readSystemWallpaperColors(context))
  }

  DisposableEffect(context) {
    val manager = runCatching { WallpaperManager.getInstance(context) }.getOrNull()
    val listener =
      WallpaperManager.OnColorsChangedListener { colors, which ->
        if ((which and WallpaperManager.FLAG_SYSTEM) != 0) {
          wallpaperColors = colors
        }
      }
    if (manager != null) {
      runCatching {
        manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
      }
    }
    onDispose {
      if (manager != null) {
        runCatching { manager.removeOnColorsChangedListener(listener) }
      }
    }
  }

  return remember(wallpaperColors, darkTheme, fallbackDark, fallbackLight) {
    val colors = wallpaperColors ?: return@remember if (darkTheme) fallbackDark else fallbackLight
    buildSchemeFromWallpaperColors(
      wallpaperColors = colors,
      darkTheme = darkTheme,
      fallbackDark = fallbackDark,
      fallbackLight = fallbackLight,
    )
  }
}

@RequiresApi(Build.VERSION_CODES.O_MR1)
private fun readSystemWallpaperColors(context: android.content.Context): WallpaperColors? =
  runCatching {
    WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
  }.getOrNull()

@RequiresApi(Build.VERSION_CODES.O_MR1)
private fun buildSchemeFromWallpaperColors(
  wallpaperColors: WallpaperColors,
  darkTheme: Boolean,
  fallbackDark: ColorScheme,
  fallbackLight: ColorScheme,
): ColorScheme {
  val seedPrimary = wallpaperColors.primaryColor.toArgb()
  val seedSecondary = wallpaperColors.secondaryColor?.toArgb() ?: shiftHue(seedPrimary, 36f)
  val seedTertiary = wallpaperColors.tertiaryColor?.toArgb() ?: shiftHue(seedPrimary, -42f)

  return if (darkTheme) {
    val primary = toneFor(seedPrimary, targetLightness = 0.72f, minSaturation = 0.48f)
    val primaryContainer = toneFor(seedPrimary, targetLightness = 0.26f, minSaturation = 0.35f)
    val secondary = toneFor(seedSecondary, targetLightness = 0.72f, minSaturation = 0.38f)
    val secondaryContainer = toneFor(seedSecondary, targetLightness = 0.24f, minSaturation = 0.30f)
    val tertiary = toneFor(seedTertiary, targetLightness = 0.76f, minSaturation = 0.42f)

    val bg = blend(0xFF0A0D14.toInt(), seedPrimary, 0.08f)
    val surface = blend(0xFF101520.toInt(), seedPrimary, 0.09f)
    val surfaceContainer = blend(0xFF161D2C.toInt(), seedPrimary, 0.11f)
    val surfaceContainerHigh = blend(0xFF202A3E.toInt(), seedPrimary, 0.13f)
    val surfaceVariant = blend(0xFF1C2436.toInt(), seedPrimary, 0.12f)
    val outline = blend(0xFF3A465C.toInt(), seedPrimary, 0.18f)
    val outlineVariant = blend(0xFF253046.toInt(), seedPrimary, 0.14f)

    darkColorScheme(
      primary = primary,
      onPrimary = contrastingOnColor(primary),
      primaryContainer = primaryContainer,
      onPrimaryContainer = Color(0xFFF1F5F9),
      secondary = secondary,
      onSecondary = contrastingOnColor(secondary),
      secondaryContainer = secondaryContainer,
      onSecondaryContainer = Color(0xFFE2E8F0),
      tertiary = tertiary,
      onTertiary = contrastingOnColor(tertiary),
      background = bg,
      onBackground = Color(0xFFF1F5F9),
      surface = surface,
      onSurface = Color(0xFFF1F5F9),
      surfaceVariant = surfaceVariant,
      onSurfaceVariant = Color(0xFFA9B6C9),
      surfaceContainer = surfaceContainer,
      surfaceContainerHigh = surfaceContainerHigh,
      outline = outline,
      outlineVariant = outlineVariant,
      error = fallbackDark.error,
    )
  } else {
    val primary = toneFor(seedPrimary, targetLightness = 0.38f, minSaturation = 0.48f)
    val primaryContainer = toneFor(seedPrimary, targetLightness = 0.88f, minSaturation = 0.36f)
    val secondary = toneFor(seedSecondary, targetLightness = 0.36f, minSaturation = 0.38f)
    val secondaryContainer = toneFor(seedSecondary, targetLightness = 0.89f, minSaturation = 0.30f)
    val tertiary = toneFor(seedTertiary, targetLightness = 0.42f, minSaturation = 0.42f)

    val bg = blend(0xFFF6F7FA.toInt(), seedPrimary, 0.05f)
    val surface = blend(0xFFFFFFFF.toInt(), seedPrimary, 0.02f)
    val surfaceContainer = blend(0xFFEEF1F6.toInt(), seedPrimary, 0.06f)
    val surfaceContainerHigh = blend(0xFFE4E9F2.toInt(), seedPrimary, 0.08f)
    val surfaceVariant = blend(0xFFE7ECF4.toInt(), seedPrimary, 0.07f)
    val outline = blend(0xFFC4CEDE.toInt(), seedPrimary, 0.12f)
    val outlineVariant = blend(0xFFDCE3EE.toInt(), seedPrimary, 0.09f)

    lightColorScheme(
      primary = primary,
      onPrimary = contrastingOnColor(primary),
      primaryContainer = primaryContainer,
      onPrimaryContainer = Color(0xFF0F172A),
      secondary = secondary,
      onSecondary = contrastingOnColor(secondary),
      secondaryContainer = secondaryContainer,
      onSecondaryContainer = Color(0xFF0F172A),
      tertiary = tertiary,
      onTertiary = contrastingOnColor(tertiary),
      background = bg,
      onBackground = Color(0xFF0F172A),
      surface = surface,
      onSurface = Color(0xFF0F172A),
      surfaceVariant = surfaceVariant,
      onSurfaceVariant = Color(0xFF475569),
      surfaceContainer = surfaceContainer,
      surfaceContainerHigh = surfaceContainerHigh,
      outline = outline,
      outlineVariant = outlineVariant,
      error = fallbackLight.error,
    )
  }
}

private fun toneFor(argb: Int, targetLightness: Float, minSaturation: Float): Color {
  val hsl = FloatArray(3)
  ColorUtils.colorToHSL(argb, hsl)
  hsl[1] = hsl[1].coerceIn(minSaturation, 0.82f)
  hsl[2] = targetLightness.coerceIn(0.12f, 0.94f)
  return Color(ColorUtils.HSLToColor(hsl))
}

private fun shiftHue(argb: Int, degrees: Float): Int {
  val hsl = FloatArray(3)
  ColorUtils.colorToHSL(argb, hsl)
  hsl[0] = (hsl[0] + degrees + 360f) % 360f
  return ColorUtils.HSLToColor(hsl)
}

private fun blend(baseArgb: Int, tintArgb: Int, ratio: Float): Color =
  Color(ColorUtils.blendARGB(baseArgb, tintArgb, ratio))

private fun contrastingOnColor(background: Color): Color {
  val bgInt = background.toArgb()
  val whiteContrast = ColorUtils.calculateContrast(0xFFFFFFFF.toInt(), bgInt)
  val darkContrast = ColorUtils.calculateContrast(0xFF0B111E.toInt(), bgInt)
  return if (whiteContrast >= darkContrast) Color.White else Color(0xFF0B111E)
}
