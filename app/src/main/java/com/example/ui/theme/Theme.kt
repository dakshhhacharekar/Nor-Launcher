package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.IconShapeOption
import com.example.data.model.ThemeColorSource
import com.example.data.model.ThemeConfig
import com.example.data.model.ThemeModeOption

val LocalIconShapeOption = compositionLocalOf { IconShapeOption.SQUIRCLE }
val LocalIsDarkTheme = compositionLocalOf { false }
val LocalThemeColorSource = compositionLocalOf { ThemeColorSource.MATERIAL_YOU }

fun shapeForIcon(option: IconShapeOption): Shape =
  when (option) {
    IconShapeOption.SQUIRCLE -> RoundedCornerShape(percent = 28)
    IconShapeOption.CIRCLE -> CircleShape
    IconShapeOption.ROUNDED_SQUARE -> RoundedCornerShape(percent = 16)
    IconShapeOption.TEARDROP ->
      RoundedCornerShape(
        topStartPercent = 50,
        topEndPercent = 50,
        bottomEndPercent = 14,
        bottomStartPercent = 50,
      )
  }

val NorDarkColorScheme =
  darkColorScheme(
    primary = NorSkyPrimaryDark,
    onPrimary = NorOnPrimaryDark,
    primaryContainer = NorPrimaryContainerDark,
    onPrimaryContainer = NorOnPrimaryContainerDark,
    secondary = NorMintSecondaryDark,
    onSecondary = NorOnSecondaryDark,
    secondaryContainer = NorSecondaryContainerDark,
    onSecondaryContainer = NorOnSecondaryContainerDark,
    tertiary = NorAmberTertiaryDark,
    onTertiary = NorOnTertiaryDark,
    background = NorBgDark,
    onBackground = NorOnSurfaceDark,
    surface = NorSurfaceDark,
    onSurface = NorOnSurfaceDark,
    surfaceVariant = NorSurfaceVariantDark,
    onSurfaceVariant = NorOnSurfaceVariantDark,
    surfaceContainer = NorSurfaceContainerDark,
    surfaceContainerHigh = NorSurfaceContainerHighDark,
    outline = NorOutlineDark,
    outlineVariant = NorOutlineVariantDark,
    error = NorErrorDark,
  )

val NorLightColorScheme =
  lightColorScheme(
    primary = NorCobaltPrimaryLight,
    onPrimary = NorOnPrimaryLight,
    primaryContainer = NorPrimaryContainerLight,
    onPrimaryContainer = NorOnPrimaryContainerLight,
    secondary = NorTealSecondaryLight,
    onSecondary = NorOnSecondaryLight,
    secondaryContainer = NorSecondaryContainerLight,
    onSecondaryContainer = NorOnSecondaryContainerLight,
    tertiary = NorAmberTertiaryLight,
    onTertiary = NorOnTertiaryLight,
    background = NorBgLight,
    onBackground = NorOnSurfaceLight,
    surface = NorSurfaceLight,
    onSurface = NorOnSurfaceLight,
    surfaceVariant = NorSurfaceVariantLight,
    onSurfaceVariant = NorOnSurfaceVariantLight,
    surfaceContainer = NorSurfaceContainerLight,
    surfaceContainerHigh = NorSurfaceContainerHighLight,
    outline = NorOutlineLight,
    outlineVariant = NorOutlineVariantLight,
    error = NorErrorLight,
  )

val ForestDarkColorScheme =
  darkColorScheme(
    primary = ForestLimePrimaryDark,
    onPrimary = ForestOnPrimaryDark,
    primaryContainer = ForestPrimaryContainerDark,
    onPrimaryContainer = ForestOnPrimaryContainerDark,
    secondary = ForestSageSecondaryDark,
    onSecondary = ForestOnSecondaryDark,
    secondaryContainer = ForestSecondaryContainerDark,
    onSecondaryContainer = ForestOnSecondaryContainerDark,
    tertiary = ForestAmberTertiaryDark,
    onTertiary = ForestOnTertiaryDark,
    background = ForestBgDark,
    onBackground = ForestOnSurfaceDark,
    surface = ForestSurfaceDark,
    onSurface = ForestOnSurfaceDark,
    surfaceVariant = ForestSurfaceVariantDark,
    onSurfaceVariant = ForestOnSurfaceVariantDark,
    surfaceContainer = ForestSurfaceContainerDark,
    surfaceContainerHigh = ForestSurfaceContainerHighDark,
    outline = ForestOutlineDark,
    outlineVariant = ForestOutlineVariantDark,
    error = NorErrorDark,
  )

val ForestLightColorScheme =
  lightColorScheme(
    primary = ForestOlivePrimaryLight,
    onPrimary = ForestOnPrimaryLight,
    primaryContainer = ForestPrimaryContainerLight,
    onPrimaryContainer = ForestOnPrimaryContainerLight,
    secondary = ForestEmeraldSecondaryLight,
    onSecondary = ForestOnSecondaryLight,
    secondaryContainer = ForestSecondaryContainerLight,
    onSecondaryContainer = ForestOnSecondaryContainerLight,
    tertiary = ForestAmberTertiaryLight,
    onTertiary = ForestOnTertiaryLight,
    background = ForestBgLight,
    onBackground = ForestOnSurfaceLight,
    surface = ForestSurfaceLight,
    onSurface = ForestOnSurfaceLight,
    surfaceVariant = ForestSurfaceVariantLight,
    onSurfaceVariant = ForestOnSurfaceVariantLight,
    surfaceContainer = ForestSurfaceContainerLight,
    surfaceContainerHigh = ForestSurfaceContainerHighLight,
    outline = ForestOutlineLight,
    outlineVariant = ForestOutlineVariantLight,
    error = NorErrorLight,
  )

val CyberY2kDarkColorScheme =
  darkColorScheme(
    primary = CyberLimePrimaryDark,
    onPrimary = CyberOnPrimaryDark,
    primaryContainer = CyberPrimaryContainerDark,
    onPrimaryContainer = CyberOnPrimaryContainerDark,
    secondary = CyberCyanSecondaryDark,
    onSecondary = CyberOnSecondaryDark,
    secondaryContainer = CyberSecondaryContainerDark,
    onSecondaryContainer = CyberOnSecondaryContainerDark,
    tertiary = CyberPinkTertiaryDark,
    onTertiary = CyberOnTertiaryDark,
    background = CyberBgDark,
    onBackground = CyberOnSurfaceDark,
    surface = CyberSurfaceDark,
    onSurface = CyberOnSurfaceDark,
    surfaceVariant = CyberSurfaceVariantDark,
    onSurfaceVariant = CyberOnSurfaceVariantDark,
    surfaceContainer = CyberSurfaceContainerDark,
    surfaceContainerHigh = CyberSurfaceContainerHighDark,
    outline = CyberOutlineDark,
    outlineVariant = CyberOutlineVariantDark,
    error = NorErrorDark,
  )

val CyberY2kLightColorScheme =
  lightColorScheme(
    primary = CyberPrimaryLight,
    onPrimary = CyberOnPrimaryLight,
    primaryContainer = CyberPrimaryContainerLight,
    onPrimaryContainer = CyberOnPrimaryContainerLight,
    secondary = CyberSecondaryLight,
    onSecondary = CyberOnSecondaryLight,
    secondaryContainer = CyberSecondaryContainerLight,
    onSecondaryContainer = CyberOnSecondaryContainerLight,
    tertiary = CyberTertiaryLight,
    onTertiary = CyberOnTertiaryLight,
    background = CyberBgLight,
    onBackground = CyberOnSurfaceLight,
    surface = CyberSurfaceLight,
    onSurface = CyberOnSurfaceLight,
    surfaceVariant = CyberSurfaceVariantLight,
    onSurfaceVariant = CyberOnSurfaceVariantLight,
    surfaceContainer = CyberSurfaceContainerLight,
    surfaceContainerHigh = CyberSurfaceContainerHighLight,
    outline = CyberOutlineLight,
    outlineVariant = CyberOutlineVariantLight,
    error = NorErrorLight,
  )

val MatchaCreamDarkColorScheme =
  darkColorScheme(
    primary = MatchaPrimaryDark,
    onPrimary = MatchaOnPrimaryDark,
    primaryContainer = MatchaPrimaryContainerDark,
    onPrimaryContainer = MatchaOnPrimaryContainerDark,
    secondary = MatchaSecondaryDark,
    onSecondary = MatchaOnSecondaryDark,
    secondaryContainer = MatchaSecondaryContainerDark,
    onSecondaryContainer = MatchaOnSecondaryContainerDark,
    tertiary = MatchaTertiaryDark,
    onTertiary = MatchaOnTertiaryDark,
    background = MatchaBgDark,
    onBackground = MatchaOnSurfaceDark,
    surface = MatchaSurfaceDark,
    onSurface = MatchaOnSurfaceDark,
    surfaceVariant = MatchaSurfaceVariantDark,
    onSurfaceVariant = MatchaOnSurfaceVariantDark,
    surfaceContainer = MatchaSurfaceContainerDark,
    surfaceContainerHigh = MatchaSurfaceContainerHighDark,
    outline = MatchaOutlineDark,
    outlineVariant = MatchaOutlineVariantDark,
    error = NorErrorDark,
  )

val MatchaCreamLightColorScheme =
  lightColorScheme(
    primary = MatchaPrimaryLight,
    onPrimary = MatchaOnPrimaryLight,
    primaryContainer = MatchaPrimaryContainerLight,
    onPrimaryContainer = MatchaOnPrimaryContainerLight,
    secondary = MatchaSecondaryLight,
    onSecondary = MatchaOnSecondaryLight,
    secondaryContainer = MatchaSecondaryContainerLight,
    onSecondaryContainer = MatchaOnSecondaryContainerLight,
    tertiary = MatchaTertiaryLight,
    onTertiary = MatchaOnTertiaryLight,
    background = MatchaBgLight,
    onBackground = MatchaOnSurfaceLight,
    surface = MatchaSurfaceLight,
    onSurface = MatchaOnSurfaceLight,
    surfaceVariant = MatchaSurfaceVariantLight,
    onSurfaceVariant = MatchaOnSurfaceVariantLight,
    surfaceContainer = MatchaSurfaceContainerLight,
    surfaceContainerHigh = MatchaSurfaceContainerHighLight,
    outline = MatchaOutlineLight,
    outlineVariant = MatchaOutlineVariantLight,
    error = NorErrorLight,
  )

val NeonBerryDarkColorScheme =
  darkColorScheme(
    primary = BerryPrimaryDark,
    onPrimary = BerryOnPrimaryDark,
    primaryContainer = BerryPrimaryContainerDark,
    onPrimaryContainer = BerryOnPrimaryContainerDark,
    secondary = BerrySecondaryDark,
    onSecondary = BerryOnSecondaryDark,
    secondaryContainer = BerrySecondaryContainerDark,
    onSecondaryContainer = BerryOnSecondaryContainerDark,
    tertiary = BerryTertiaryDark,
    onTertiary = BerryOnTertiaryDark,
    background = BerryBgDark,
    onBackground = BerryOnSurfaceDark,
    surface = BerrySurfaceDark,
    onSurface = BerryOnSurfaceDark,
    surfaceVariant = BerrySurfaceVariantDark,
    onSurfaceVariant = BerryOnSurfaceVariantDark,
    surfaceContainer = BerrySurfaceContainerDark,
    surfaceContainerHigh = BerrySurfaceContainerHighDark,
    outline = BerryOutlineDark,
    outlineVariant = BerryOutlineVariantDark,
    error = NorErrorDark,
  )

val NeonBerryLightColorScheme =
  lightColorScheme(
    primary = BerryPrimaryLight,
    onPrimary = BerryOnPrimaryLight,
    primaryContainer = BerryPrimaryContainerLight,
    onPrimaryContainer = BerryOnPrimaryContainerLight,
    secondary = BerrySecondaryLight,
    onSecondary = BerryOnSecondaryLight,
    secondaryContainer = BerrySecondaryContainerLight,
    onSecondaryContainer = BerryOnSecondaryContainerLight,
    tertiary = BerryTertiaryLight,
    onTertiary = BerryOnTertiaryLight,
    background = BerryBgLight,
    onBackground = BerryOnSurfaceLight,
    surface = BerrySurfaceLight,
    onSurface = BerryOnSurfaceLight,
    surfaceVariant = BerrySurfaceVariantLight,
    onSurfaceVariant = BerryOnSurfaceVariantLight,
    surfaceContainer = BerrySurfaceContainerLight,
    surfaceContainerHigh = BerrySurfaceContainerHighLight,
    outline = BerryOutlineLight,
    outlineVariant = BerryOutlineVariantLight,
    error = NorErrorLight,
  )

@Composable
fun NorLauncherTheme(
  themeConfig: ThemeConfig = ThemeConfig(),
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val isDark =
    when (themeConfig.themeMode) {
      ThemeModeOption.SYSTEM -> systemDark
      ThemeModeOption.LIGHT -> false
      ThemeModeOption.DARK -> true
    }

  val context = LocalContext.current
  val wallpaperScheme =
    rememberWallpaperColorScheme(
      darkTheme = isDark,
      fallbackDark = NorDarkColorScheme,
      fallbackLight = NorLightColorScheme,
    )

  val colorScheme =
    when (themeConfig.colorSource) {
      ThemeColorSource.MATERIAL_YOU -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
          wallpaperScheme
        }
      }
      ThemeColorSource.WALLPAPER -> wallpaperScheme
      ThemeColorSource.CYBER_Y2K -> if (isDark) CyberY2kDarkColorScheme else CyberY2kLightColorScheme
      ThemeColorSource.MATCHA_CREAM -> if (isDark) MatchaCreamDarkColorScheme else MatchaCreamLightColorScheme
      ThemeColorSource.NEON_BERRY -> if (isDark) NeonBerryDarkColorScheme else NeonBerryLightColorScheme
      ThemeColorSource.NORDIC -> if (isDark) NorDarkColorScheme else NorLightColorScheme
      ThemeColorSource.FOREST -> if (isDark) ForestDarkColorScheme else ForestLightColorScheme
    }

  val typography = remember(themeConfig.fontChoice) { typographyFor(themeConfig.fontChoice) }

  CompositionLocalProvider(
    LocalIconShapeOption provides themeConfig.iconShape,
    LocalIsDarkTheme provides isDark,
    LocalThemeColorSource provides themeConfig.colorSource,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = typography,
      content = content,
    )
  }
}
