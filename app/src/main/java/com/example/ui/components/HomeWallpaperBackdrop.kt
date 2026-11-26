package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import com.example.data.model.ThemeColorSource
import com.example.data.model.WallpaperConfig
import com.example.data.model.WallpaperPreset
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.LocalThemeColorSource

/**
 * Renders the active Home screen wallpaper:
 * - Custom Photo picked via Android's zero-permission Photo Picker
 * - Or one of 4 crisp procedural Canvas wallpapers: Nordic Peaks, Forest Horizon,
 *   Aurora Glass, and Sunset Dunes.
 * Applies a subtle user-tunable dim scrim so home icons and widgets always stay readable.
 */
@Composable
fun HomeWallpaperBackdrop(
  wallpaperConfig: WallpaperConfig = WallpaperConfig(),
  customWallpaperBitmap: ImageBitmap? = null,
  darkTheme: Boolean = LocalIsDarkTheme.current,
  colorSource: ThemeColorSource = LocalThemeColorSource.current,
  modifier: Modifier = Modifier,
) {
  val effectivePreset =
    if (wallpaperConfig.preset == WallpaperPreset.CUSTOM_PHOTO && customWallpaperBitmap != null) {
      WallpaperPreset.CUSTOM_PHOTO
    } else if (wallpaperConfig.preset == WallpaperPreset.CUSTOM_PHOTO) {
      WallpaperPreset.NORDIC_PEAKS
    } else {
      wallpaperConfig.preset
    }

  Box(modifier = modifier.fillMaxSize()) {
    when (effectivePreset) {
      WallpaperPreset.CUSTOM_PHOTO -> {
        if (customWallpaperBitmap != null) {
          Image(
            bitmap = customWallpaperBitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
      WallpaperPreset.NORDIC_PEAKS -> {
        NordicMountainCanvas(
          darkTheme = darkTheme,
          forestTint = colorSource == ThemeColorSource.FOREST,
          modifier = Modifier.fillMaxSize(),
        )
      }
      WallpaperPreset.CYBER_MESH -> {
        CyberMeshCanvas(modifier = Modifier.fillMaxSize())
      }
      WallpaperPreset.MATCHA_AURA -> {
        MatchaAuraCanvas(darkTheme = darkTheme, modifier = Modifier.fillMaxSize())
      }
      WallpaperPreset.MIDNIGHT_VAPOR -> {
        MidnightVaporCanvas(modifier = Modifier.fillMaxSize())
      }
      WallpaperPreset.FOREST_HORIZON -> {
        ForestHorizonCanvas(darkTheme = darkTheme, modifier = Modifier.fillMaxSize())
      }
      WallpaperPreset.AURORA_GLASS -> {
        AuroraGlassCanvas(modifier = Modifier.fillMaxSize())
      }
      WallpaperPreset.SUNSET_DUNES -> {
        SunsetDunesCanvas(modifier = Modifier.fillMaxSize())
      }
    }

    // Adjustable readability dim overlay (strongest on custom photos / bright scenes)
    val dimAlpha =
      when (effectivePreset) {
        WallpaperPreset.CUSTOM_PHOTO -> (wallpaperConfig.dimPercent / 100f).coerceIn(0f, 0.60f)
        else -> ((wallpaperConfig.dimPercent - 15).coerceAtLeast(0) / 100f).coerceIn(0f, 0.45f)
      }
    if (dimAlpha > 0.01f) {
      Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimAlpha)))
    }
  }
}

@Composable
fun WallpaperPresetThumbnail(
  preset: WallpaperPreset,
  customBitmap: ImageBitmap?,
  darkTheme: Boolean,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier) {
    when (preset) {
      WallpaperPreset.CUSTOM_PHOTO -> {
        if (customBitmap != null) {
          Image(
            bitmap = customBitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          AuroraGlassCanvas(modifier = Modifier.fillMaxSize())
        }
      }
      WallpaperPreset.NORDIC_PEAKS -> {
        NordicMountainCanvas(darkTheme = darkTheme, forestTint = false, modifier = Modifier.fillMaxSize())
      }
      WallpaperPreset.CYBER_MESH -> {
        CyberMeshCanvas(modifier = Modifier.fillMaxSize())
      }
      WallpaperPreset.MATCHA_AURA -> {
        MatchaAuraCanvas(darkTheme = darkTheme, modifier = Modifier.fillMaxSize())
      }
      WallpaperPreset.MIDNIGHT_VAPOR -> {
        MidnightVaporCanvas(modifier = Modifier.fillMaxSize())
      }
      WallpaperPreset.FOREST_HORIZON -> {
        ForestHorizonCanvas(darkTheme = darkTheme, modifier = Modifier.fillMaxSize())
      }
      WallpaperPreset.AURORA_GLASS -> {
        AuroraGlassCanvas(modifier = Modifier.fillMaxSize())
      }
      WallpaperPreset.SUNSET_DUNES -> {
        SunsetDunesCanvas(modifier = Modifier.fillMaxSize())
      }
    }
  }
}

@Composable
private fun NordicMountainCanvas(
  darkTheme: Boolean,
  forestTint: Boolean,
  modifier: Modifier = Modifier,
) {
  val skyTop =
    when {
      forestTint && darkTheme -> Color(0xFF10190E)
      forestTint -> Color(0xFFE4ECE0)
      darkTheme -> Color(0xFF0E1422)
      else -> Color(0xFFE4E6EA)
    }
  val skyBottom =
    when {
      forestTint && darkTheme -> Color(0xFF1C2A17)
      forestTint -> Color(0xFFCFDDC8)
      darkTheme -> Color(0xFF1A2338)
      else -> Color(0xFFD0D3D8)
    }
  val haloCenter =
    when {
      forestTint && darkTheme -> Color(0xFF2B4223)
      forestTint -> Color(0xFFBFD1B8)
      darkTheme -> Color(0xFF283552)
      else -> Color(0xFFC0C3C8)
    }
  val haloEdge =
    when {
      forestTint && darkTheme -> Color(0xFF152212)
      forestTint -> Color(0xFFA2B89A)
      darkTheme -> Color(0xFF141C2E)
      else -> Color(0xFFA6AAB1)
    }
  val mountainBody =
    when {
      forestTint && darkTheme -> Color(0xFF334B2B)
      forestTint -> Color(0xFF6B8065)
      darkTheme -> Color(0xFF33415C)
      else -> Color(0xFF7B7F85)
    }
  val mountainBodySecondary =
    when {
      forestTint && darkTheme -> Color(0xFF263820)
      forestTint -> Color(0xFF5D7057)
      darkTheme -> Color(0xFF27334A)
      else -> Color(0xFF6B6F76)
    }
  val peakCap =
    when {
      forestTint && darkTheme -> Color(0xFF0A1209)
      forestTint -> Color(0xFF192416)
      darkTheme -> Color(0xFF0B101B)
      else -> Color(0xFF1C1E21)
    }
  val foregroundHill =
    when {
      forestTint && darkTheme -> Color(0xFF111B0F)
      forestTint -> Color(0xFFD2DFCD)
      darkTheme -> Color(0xFF141B2B)
      else -> Color(0xFFD2D5DA)
    }
  val starColor =
    if (darkTheme) Color.White.copy(alpha = 0.42f) else Color(0xFF374151).copy(alpha = 0.35f)

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    drawRect(brush = Brush.verticalGradient(colors = listOf(skyTop, skyBottom)), size = size)

    val arcCenter = Offset(w * 0.5f, h * 0.62f)
    val arcRadius = w * 0.92f
    drawCircle(
      brush =
        Brush.radialGradient(
          colorStops =
            arrayOf(
              0.0f to haloCenter,
              0.82f to haloCenter.copy(alpha = 0.9f),
              0.97f to haloEdge.copy(alpha = 0.55f),
              1.0f to Color.Transparent,
            ),
          center = arcCenter,
          radius = arcRadius,
        ),
      radius = arcRadius,
      center = arcCenter,
    )

    val speckles =
      listOf(
        0.12f to 0.06f, 0.28f to 0.09f, 0.64f to 0.04f, 0.84f to 0.11f,
        0.19f to 0.17f, 0.76f to 0.18f, 0.91f to 0.22f, 0.08f to 0.26f,
        0.34f to 0.29f, 0.68f to 0.27f, 0.52f to 0.32f, 0.15f to 0.36f,
        0.82f to 0.35f, 0.26f to 0.41f, 0.72f to 0.40f, 0.42f to 0.15f,
      )
    speckles.forEachIndexed { idx, (rx, ry) ->
      drawCircle(
        color = starColor,
        radius = if (idx % 3 == 0) 2.2f else 1.4f,
        center = Offset(w * rx, h * ry),
      )
    }

    val baselineY = h * 0.68f
    drawMountainWithCap(
      peak = Offset(w * 0.27f, h * 0.49f),
      leftBase = Offset(w * 0.04f, baselineY),
      rightBase = Offset(w * 0.50f, baselineY),
      capRatio = 0.34f,
      teeth = 4,
      bodyColor = mountainBodySecondary,
      capColor = peakCap,
    )
    drawMountainWithCap(
      peak = Offset(w * 0.76f, h * 0.52f),
      leftBase = Offset(w * 0.55f, baselineY),
      rightBase = Offset(w * 0.97f, baselineY),
      capRatio = 0.34f,
      teeth = 4,
      bodyColor = mountainBodySecondary,
      capColor = peakCap,
    )
    drawMountainWithCap(
      peak = Offset(w * 0.49f, h * 0.425f),
      leftBase = Offset(w * 0.17f, baselineY),
      rightBase = Offset(w * 0.82f, baselineY),
      capRatio = 0.36f,
      teeth = 5,
      bodyColor = mountainBody,
      capColor = peakCap,
    )

    val hillPath =
      Path().apply {
        moveTo(0f, h * 0.64f)
        quadraticTo(w * 0.5f, h * 0.61f, w, h * 0.645f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
      }
    drawPath(path = hillPath, color = foregroundHill)
  }
}

@Composable
private fun ForestHorizonCanvas(
  darkTheme: Boolean,
  modifier: Modifier = Modifier,
) {
  val top = if (darkTheme) Color(0xFF1E2D12) else Color(0xFFD8E6CD)
  val mid = if (darkTheme) Color(0xFF283D18) else Color(0xFFB7CFA7)
  val bottom = if (darkTheme) Color(0xFF0A1008) else Color(0xFF8CA87E)
  val glow = if (darkTheme) Color(0xFF537B2C).copy(alpha = 0.45f) else Color(0xFFF3FAE8).copy(alpha = 0.72f)
  val ridgeFar = if (darkTheme) Color(0xFF17250F) else Color(0xFF9DB98E)
  val ridgeNear = if (darkTheme) Color(0xFF0E170B) else Color(0xFF7B9B6E)

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    drawRect(brush = Brush.verticalGradient(listOf(top, mid, bottom)), size = size)

    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(glow, Color.Transparent),
          center = Offset(w * 0.35f, h * 0.32f),
          radius = w * 0.85f,
        ),
      radius = w * 0.85f,
      center = Offset(w * 0.35f, h * 0.32f),
    )

    val farPath =
      Path().apply {
        moveTo(0f, h * 0.58f)
        cubicTo(w * 0.28f, h * 0.48f, w * 0.68f, h * 0.64f, w, h * 0.52f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
      }
    drawPath(farPath, color = ridgeFar)

    val nearPath =
      Path().apply {
        moveTo(0f, h * 0.68f)
        cubicTo(w * 0.35f, h * 0.62f, w * 0.72f, h * 0.72f, w, h * 0.65f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
      }
    drawPath(nearPath, color = ridgeNear)
  }
}

@Composable
private fun AuroraGlassCanvas(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    drawRect(
      brush =
        Brush.verticalGradient(
          colors = listOf(Color(0xFF070B1E), Color(0xFF10193B), Color(0xFF070A16))
        ),
      size = size,
    )

    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0xFF2DD4BF).copy(alpha = 0.46f), Color.Transparent),
          center = Offset(w * 0.80f, h * 0.30f),
          radius = w * 0.75f,
        ),
      radius = w * 0.75f,
      center = Offset(w * 0.80f, h * 0.30f),
    )

    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0xFF7C3AED).copy(alpha = 0.55f), Color.Transparent),
          center = Offset(w * 0.22f, h * 0.52f),
          radius = w * 0.82f,
        ),
      radius = w * 0.82f,
      center = Offset(w * 0.22f, h * 0.52f),
    )

    val ribbon =
      Path().apply {
        moveTo(0f, h * 0.54f)
        cubicTo(w * 0.32f, h * 0.34f, w * 0.64f, h * 0.68f, w, h * 0.44f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
      }
    drawPath(
      path = ribbon,
      brush =
        Brush.verticalGradient(
          colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.32f), Color(0xFF090D1F))
        ),
    )
  }
}

@Composable
private fun SunsetDunesCanvas(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    drawRect(
      brush =
        Brush.verticalGradient(
          colors = listOf(Color(0xFF1C1329), Color(0xFF6B2D3C), Color(0xFFD97742))
        ),
      size = size,
    )

    // Warm sun orb
    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0xFFFDE68A), Color(0xFFFB923C).copy(alpha = 0.5f), Color.Transparent),
          center = Offset(w * 0.52f, h * 0.44f),
          radius = w * 0.42f,
        ),
      radius = w * 0.42f,
      center = Offset(w * 0.52f, h * 0.44f),
    )

    val duneFar =
      Path().apply {
        moveTo(0f, h * 0.58f)
        quadraticTo(w * 0.38f, h * 0.49f, w, h * 0.60f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
      }
    drawPath(duneFar, color = Color(0xFF4A1D2E))

    val duneNear =
      Path().apply {
        moveTo(0f, h * 0.67f)
        quadraticTo(w * 0.62f, h * 0.57f, w, h * 0.68f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
      }
    drawPath(duneNear, color = Color(0xFF23111E))
  }
}

@Composable
private fun CyberMeshCanvas(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    // Deep Y2K obsidian base
    drawRect(
      brush =
        Brush.verticalGradient(
          colors = listOf(Color(0xFF070712), Color(0xFF0E1024), Color(0xFF06060D))
        ),
      size = size,
    )

    // Acid lime ambient orb
    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0xFFCCFF00).copy(alpha = 0.40f), Color.Transparent),
          center = Offset(w * 0.20f, h * 0.24f),
          radius = w * 0.78f,
        ),
      radius = w * 0.78f,
      center = Offset(w * 0.20f, h * 0.24f),
    )

    // Electric cyan orb
    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0xFF22D3EE).copy(alpha = 0.44f), Color.Transparent),
          center = Offset(w * 0.85f, h * 0.56f),
          radius = w * 0.80f,
        ),
      radius = w * 0.80f,
      center = Offset(w * 0.85f, h * 0.56f),
    )

    // Hyper magenta lower glow
    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0xFFEC4899).copy(alpha = 0.30f), Color.Transparent),
          center = Offset(w * 0.35f, h * 0.84f),
          radius = w * 0.72f,
        ),
      radius = w * 0.72f,
      center = Offset(w * 0.35f, h * 0.84f),
    )

    // Subtle Y2K cyber grid lines
    val gridColor = Color(0xFFCCFF00).copy(alpha = 0.09f)
    val horizonY = h * 0.58f
    for (i in 0..8) {
      val t = i / 8f
      val y = horizonY + (h - horizonY) * (t * t)
      drawLine(
        color = gridColor,
        start = Offset(0f, y),
        end = Offset(w, y),
        strokeWidth = 1.4f,
      )
    }
    val vanish = Offset(w * 0.5f, horizonY)
    for (i in -4..4) {
      val endX = w * 0.5f + i * (w * 0.24f)
      drawLine(
        color = gridColor,
        start = vanish,
        end = Offset(endX, h),
        strokeWidth = 1.2f,
      )
    }
  }
}

@Composable
private fun MatchaAuraCanvas(
  darkTheme: Boolean,
  modifier: Modifier = Modifier,
) {
  val bgTop = if (darkTheme) Color(0xFF131811) else Color(0xFFFBF8F0)
  val bgBottom = if (darkTheme) Color(0xFF1D2418) else Color(0xFFEFE9DA)
  val matchaOrb = if (darkTheme) Color(0xFF84CC16).copy(alpha = 0.34f) else Color(0xFFA7D979).copy(alpha = 0.58f)
  val peachOrb = if (darkTheme) Color(0xFFE6B89C).copy(alpha = 0.28f) else Color(0xFFF6C6AD).copy(alpha = 0.52f)
  val creamOrb = if (darkTheme) Color(0xFFD9F99D).copy(alpha = 0.16f) else Color(0xFFFFFDF7).copy(alpha = 0.78f)

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    drawRect(brush = Brush.verticalGradient(listOf(bgTop, bgBottom)), size = size)

    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(matchaOrb, Color.Transparent),
          center = Offset(w * 0.24f, h * 0.30f),
          radius = w * 0.82f,
        ),
      radius = w * 0.82f,
      center = Offset(w * 0.24f, h * 0.30f),
    )

    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(peachOrb, Color.Transparent),
          center = Offset(w * 0.78f, h * 0.62f),
          radius = w * 0.85f,
        ),
      radius = w * 0.85f,
      center = Offset(w * 0.78f, h * 0.62f),
    )

    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(creamOrb, Color.Transparent),
          center = Offset(w * 0.52f, h * 0.46f),
          radius = w * 0.65f,
        ),
      radius = w * 0.65f,
      center = Offset(w * 0.52f, h * 0.46f),
    )

    val softWave =
      Path().apply {
        moveTo(0f, h * 0.74f)
        cubicTo(w * 0.32f, h * 0.64f, w * 0.68f, h * 0.82f, w, h * 0.70f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
      }
    drawPath(
      path = softWave,
      color = if (darkTheme) Color(0xFF0E120C).copy(alpha = 0.65f) else Color(0xFFD9E6CB).copy(alpha = 0.50f),
    )
  }
}

@Composable
private fun MidnightVaporCanvas(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    drawRect(
      brush =
        Brush.verticalGradient(
          colors = listOf(Color(0xFF0C0618), Color(0xFF1F0E3B), Color(0xFF110722))
        ),
      size = size,
    )

    // Hyper magenta aura
    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0xFFFF4FBF).copy(alpha = 0.48f), Color.Transparent),
          center = Offset(w * 0.76f, h * 0.28f),
          radius = w * 0.82f,
        ),
      radius = w * 0.82f,
      center = Offset(w * 0.76f, h * 0.28f),
    )

    // Electric indigo aura
    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.52f), Color.Transparent),
          center = Offset(w * 0.24f, h * 0.54f),
          radius = w * 0.86f,
        ),
      radius = w * 0.86f,
      center = Offset(w * 0.24f, h * 0.54f),
    )

    // Sky-cyan accent mist
    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.34f), Color.Transparent),
          center = Offset(w * 0.56f, h * 0.78f),
          radius = w * 0.74f,
        ),
      radius = w * 0.74f,
      center = Offset(w * 0.56f, h * 0.78f),
    )

    // Dreamy starlight sparkles
    val stars =
      listOf(
        0.16f to 0.10f, 0.38f to 0.16f, 0.68f to 0.12f, 0.86f to 0.20f,
        0.12f to 0.34f, 0.48f to 0.28f, 0.82f to 0.42f, 0.28f to 0.68f,
        0.74f to 0.66f, 0.90f to 0.78f,
      )
    stars.forEachIndexed { idx, (rx, ry) ->
      drawCircle(
        color = Color.White.copy(alpha = if (idx % 2 == 0) 0.55f else 0.30f),
        radius = if (idx % 3 == 0) 2.5f else 1.5f,
        center = Offset(w * rx, h * ry),
      )
    }

    val vaporWave =
      Path().apply {
        moveTo(0f, h * 0.62f)
        cubicTo(w * 0.30f, h * 0.48f, w * 0.70f, h * 0.72f, w, h * 0.56f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
      }
    drawPath(
      path = vaporWave,
      brush =
        Brush.verticalGradient(
          colors = listOf(Color(0xFFC026D3).copy(alpha = 0.24f), Color(0xFF0A0514))
        ),
    )
  }
}

private fun DrawScope.drawMountainWithCap(
  peak: Offset,
  leftBase: Offset,
  rightBase: Offset,
  capRatio: Float,
  teeth: Int,
  bodyColor: Color,
  capColor: Color,
) {
  val bodyPath =
    Path().apply {
      moveTo(peak.x, peak.y)
      lineTo(rightBase.x, rightBase.y)
      lineTo(leftBase.x, leftBase.y)
      close()
    }
  drawPath(path = bodyPath, color = bodyColor)

  val leftCapEdge =
    Offset(
      x = peak.x + (leftBase.x - peak.x) * capRatio,
      y = peak.y + (leftBase.y - peak.y) * capRatio,
    )
  val rightCapEdge =
    Offset(
      x = peak.x + (rightBase.x - peak.x) * capRatio,
      y = peak.y + (rightBase.y - peak.y) * capRatio,
    )

  val capPath =
    Path().apply {
      moveTo(peak.x, peak.y)
      lineTo(rightCapEdge.x, rightCapEdge.y)
      val dx = (leftCapEdge.x - rightCapEdge.x) / (teeth * 2f)
      val baseY = rightCapEdge.y
      val toothDepth = (rightCapEdge.y - peak.y) * 0.24f
      for (i in 1 until teeth * 2) {
        val x = rightCapEdge.x + dx * i
        val y = if (i % 2 == 1) baseY + toothDepth else baseY - toothDepth * 0.35f
        lineTo(x, y)
      }
      lineTo(leftCapEdge.x, leftCapEdge.y)
      close()
    }
  drawPath(path = capPath, color = capColor)
}
