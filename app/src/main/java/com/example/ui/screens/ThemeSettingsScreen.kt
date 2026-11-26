package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.FontChoice
import com.example.data.model.IconShapeOption
import com.example.data.model.ThemeColorSource
import com.example.data.model.ThemeConfig
import com.example.data.model.ThemeModeOption
import com.example.ui.theme.BerryPrimaryDark
import com.example.ui.theme.CyberLimePrimaryDark
import com.example.ui.theme.ForestLimePrimaryDark
import com.example.ui.theme.MatchaPrimaryDark
import com.example.ui.theme.NorSkyPrimaryDark
import com.example.ui.theme.fontFamilyFor
import com.example.ui.theme.shapeForIcon

@Composable
fun ThemeSettingsScreen(
  themeConfig: ThemeConfig,
  onSelectColorSource: (ThemeColorSource) -> Unit,
  onSelectThemeMode: (ThemeModeOption) -> Unit,
  onSelectFontChoice: (FontChoice) -> Unit,
  onSelectIconShape: (IconShapeOption) -> Unit,
  onToggleHideSystemBars: (Boolean) -> Unit,
  activeIconPackLabel: String?,
  onOpenIconPackPicker: () -> Unit,
  onOpenWallpaperPicker: () -> Unit,
  onResetDefaults: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(enabled = true) { onBack() }

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = modifier.fillMaxSize().testTag("theme_settings_screen"),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Top bar
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 640.dp)
            .padding(horizontal = 8.dp, vertical = 10.dp),
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("theme_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.cd_back_button),
            tint = MaterialTheme.colorScheme.onBackground,
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.theme_screen_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = stringResource(R.string.theme_screen_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Main scrollable controls
      LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.weight(1f).fillMaxWidth().widthIn(max = 640.dp),
      ) {
        item(key = "live_preview") {
          ThemeLivePreviewCard(iconShape = themeConfig.iconShape)
        }

        item(key = "wallpaper_shortcut") {
          SectionHeader(title = stringResource(R.string.theme_section_wallpaper))
          Spacer(modifier = Modifier.height(10.dp))
          WallpaperShortcutCard(onClick = onOpenWallpaperPicker)
        }

        item(key = "color_palette") {
          SectionHeader(title = stringResource(R.string.theme_section_colors))
          Spacer(modifier = Modifier.height(10.dp))
          ColorPaletteSection(
            selected = themeConfig.colorSource,
            onSelect = onSelectColorSource,
          )
        }

        item(key = "appearance_mode") {
          SectionHeader(title = stringResource(R.string.theme_section_mode))
          Spacer(modifier = Modifier.height(10.dp))
          AppearanceModeRow(
            selected = themeConfig.themeMode,
            onSelect = onSelectThemeMode,
          )
        }

        item(key = "typography") {
          SectionHeader(title = stringResource(R.string.theme_section_font))
          Spacer(modifier = Modifier.height(10.dp))
          TypographySection(
            selected = themeConfig.fontChoice,
            onSelect = onSelectFontChoice,
          )
        }

        item(key = "icon_pack_shortcut") {
          SectionHeader(title = stringResource(R.string.theme_section_icon_pack))
          Spacer(modifier = Modifier.height(10.dp))
          IconPackShortcutCard(
            activePackLabel = activeIconPackLabel,
            onClick = onOpenIconPackPicker,
          )
        }

        item(key = "icon_shape") {
          SectionHeader(title = stringResource(R.string.theme_section_icon_shape))
          Spacer(modifier = Modifier.height(10.dp))
          IconShapeSection(
            selected = themeConfig.iconShape,
            onSelect = onSelectIconShape,
          )
        }

        item(key = "display_system_bars") {
          SectionHeader(title = stringResource(R.string.theme_section_display))
          Spacer(modifier = Modifier.height(10.dp))
          HideSystemBarsCard(
            checked = themeConfig.hideSystemBars,
            onCheckedChange = onToggleHideSystemBars,
          )
        }

        item(key = "bottom_spacer") {
          Spacer(modifier = Modifier.height(8.dp))
        }
      }

      // One-handed bottom action bar
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 640.dp)
            .padding(horizontal = 20.dp, vertical = 14.dp),
      ) {
        OutlinedButton(
          onClick = onResetDefaults,
          shape = RoundedCornerShape(16.dp),
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("theme_reset_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.RestartAlt,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = stringResource(R.string.theme_reset_defaults))
        }

        Button(
          onClick = onBack,
          shape = RoundedCornerShape(16.dp),
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("theme_done_button"),
        ) {
          Text(text = stringResource(R.string.gesture_done))
        }
      }
    }
  }
}

@Composable
private fun SectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleSmall,
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.primary,
  )
}

@Composable
private fun ThemeLivePreviewCard(iconShape: IconShapeOption) {
  val previewShape = shapeForIcon(iconShape)
  val cardBrush =
    Brush.linearGradient(
      colors =
        listOf(
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
          MaterialTheme.colorScheme.surfaceContainerHigh,
          MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
        )
    )

  Surface(
    shape = RoundedCornerShape(26.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    color = Color.Transparent,
    modifier = Modifier.fillMaxWidth().testTag("theme_live_preview"),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth().background(cardBrush).padding(vertical = 18.dp, horizontal = 16.dp),
    ) {
      Text(
        text = "12:46",
        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(14.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val sampleIcons =
          listOf(
            Icons.Rounded.Phone to MaterialTheme.colorScheme.primary,
            Icons.Rounded.Public to MaterialTheme.colorScheme.secondary,
            Icons.Rounded.CameraAlt to MaterialTheme.colorScheme.tertiary,
            Icons.Rounded.MusicNote to MaterialTheme.colorScheme.primary,
          )
        sampleIcons.forEach { (icon, tint) ->
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(44.dp)
                .clip(previewShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, previewShape),
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = tint,
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ColorPaletteSection(
  selected: ThemeColorSource,
  onSelect: (ThemeColorSource) -> Unit,
) {
  val items =
    listOf(
      PaletteOptionSpec(
        source = ThemeColorSource.MATERIAL_YOU,
        titleRes = R.string.theme_color_material_you,
        descRes = R.string.theme_color_material_you_desc,
        icon = Icons.Rounded.AutoAwesome,
        swatchColor = MaterialTheme.colorScheme.primary,
      ),
      PaletteOptionSpec(
        source = ThemeColorSource.WALLPAPER,
        titleRes = R.string.theme_color_wallpaper,
        descRes = R.string.theme_color_wallpaper_desc,
        icon = Icons.Rounded.Wallpaper,
        swatchColor = MaterialTheme.colorScheme.secondary,
      ),
      PaletteOptionSpec(
        source = ThemeColorSource.CYBER_Y2K,
        titleRes = R.string.theme_color_cyber_y2k,
        descRes = R.string.theme_color_cyber_y2k_desc,
        icon = Icons.Rounded.Bolt,
        swatchColor = CyberLimePrimaryDark,
      ),
      PaletteOptionSpec(
        source = ThemeColorSource.MATCHA_CREAM,
        titleRes = R.string.theme_color_matcha_cream,
        descRes = R.string.theme_color_matcha_cream_desc,
        icon = Icons.Rounded.Spa,
        swatchColor = MatchaPrimaryDark,
      ),
      PaletteOptionSpec(
        source = ThemeColorSource.NEON_BERRY,
        titleRes = R.string.theme_color_neon_berry,
        descRes = R.string.theme_color_neon_berry_desc,
        icon = Icons.Rounded.GraphicEq,
        swatchColor = BerryPrimaryDark,
      ),
      PaletteOptionSpec(
        source = ThemeColorSource.NORDIC,
        titleRes = R.string.theme_color_nordic,
        descRes = R.string.theme_color_nordic_desc,
        icon = Icons.Rounded.Landscape,
        swatchColor = NorSkyPrimaryDark,
      ),
      PaletteOptionSpec(
        source = ThemeColorSource.FOREST,
        titleRes = R.string.theme_color_forest,
        descRes = R.string.theme_color_forest_desc,
        icon = Icons.Rounded.Forest,
        swatchColor = ForestLimePrimaryDark,
      ),
    )

  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      items.forEachIndexed { index, spec ->
        val isSelected = spec.source == selected
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .clickable { onSelect(spec.source) }
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .testTag("theme_color_${spec.source.id}"),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                  if (isSelected) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceContainerHigh
                ),
          ) {
            Icon(
              imageVector = spec.icon,
              contentDescription = null,
              tint =
                if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else spec.swatchColor,
              modifier = Modifier.size(21.dp),
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(spec.titleRes),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color =
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = stringResource(spec.descRes),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          if (isSelected) {
            Icon(
              imageVector = Icons.Rounded.Check,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        if (index < items.lastIndex) {
          HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            modifier = Modifier.padding(horizontal = 16.dp),
          )
        }
      }
    }
  }
}

private data class PaletteOptionSpec(
  val source: ThemeColorSource,
  val titleRes: Int,
  val descRes: Int,
  val icon: ImageVector,
  val swatchColor: Color,
)

@Composable
private fun AppearanceModeRow(
  selected: ThemeModeOption,
  onSelect: (ThemeModeOption) -> Unit,
) {
  val modes =
    listOf(
      Triple(ThemeModeOption.SYSTEM, R.string.theme_mode_system, Icons.Rounded.SettingsBrightness),
      Triple(ThemeModeOption.LIGHT, R.string.theme_mode_light, Icons.Rounded.LightMode),
      Triple(ThemeModeOption.DARK, R.string.theme_mode_dark, Icons.Rounded.DarkMode),
    )

  Row(
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    modes.forEach { (mode, labelRes, icon) ->
      val isSelected = mode == selected
      Surface(
        color =
          if (isSelected) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        border =
          BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color =
              if (isSelected) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.outlineVariant,
          ),
        modifier =
          Modifier.weight(1f)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelect(mode) }
            .testTag("theme_mode_${mode.id}"),
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint =
              if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
              else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelLarge,
            color =
              if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
              else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun TypographySection(
  selected: FontChoice,
  onSelect: (FontChoice) -> Unit,
) {
  val fonts =
    listOf(
      FontChoice.OUTFIT to R.string.theme_font_outfit,
      FontChoice.PLUS_JAKARTA to R.string.theme_font_jakarta,
      FontChoice.SPACE_GROTESK to R.string.theme_font_space,
      FontChoice.JETBRAINS_MONO to R.string.theme_font_jetbrains,
      FontChoice.SYSTEM to R.string.theme_font_system,
    )

  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      fonts.forEachIndexed { index, (choice, labelRes) ->
        val isSelected = choice == selected
        val previewFamily = fontFamilyFor(choice)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .clickable { onSelect(choice) }
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .testTag("theme_font_${choice.id}"),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(38.dp)
                .clip(CircleShape)
                .background(
                  if (isSelected) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceContainerHigh
                ),
          ) {
            Text(
              text = "Aa",
              fontFamily = previewFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color =
                if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = previewFamily),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color =
              if (isSelected) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
          )
          if (isSelected) {
            Icon(
              imageVector = Icons.Rounded.Check,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        if (index < fonts.lastIndex) {
          HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            modifier = Modifier.padding(horizontal = 16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun IconShapeSection(
  selected: IconShapeOption,
  onSelect: (IconShapeOption) -> Unit,
) {
  val shapes =
    listOf(
      IconShapeOption.SQUIRCLE to R.string.theme_shape_squircle,
      IconShapeOption.CIRCLE to R.string.theme_shape_circle,
      IconShapeOption.ROUNDED_SQUARE to R.string.theme_shape_rounded,
      IconShapeOption.TEARDROP to R.string.theme_shape_teardrop,
    )

  Row(
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    shapes.forEach { (option, labelRes) ->
      val isSelected = option == selected
      val previewShape = shapeForIcon(option)

      Surface(
        color =
          if (isSelected) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(18.dp),
        border =
          BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color =
              if (isSelected) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.outlineVariant,
          ),
        modifier =
          Modifier.weight(1f)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onSelect(option) }
            .testTag("theme_shape_${option.id}"),
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(34.dp)
                .clip(previewShape)
                .background(
                  if (isSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.surfaceContainerHigh
                )
                .border(
                  width = 1.dp,
                  color =
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                  shape = previewShape,
                ),
          ) {
            Icon(
              imageVector = Icons.Rounded.Palette,
              contentDescription = null,
              tint =
                if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp),
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color =
              if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
              else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun HideSystemBarsCard(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .clickable { onCheckedChange(!checked) }
        .testTag("theme_hide_system_bars_row"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
              if (checked) MaterialTheme.colorScheme.primaryContainer
              else MaterialTheme.colorScheme.surfaceContainerHigh
            ),
      ) {
        Icon(
          imageVector = Icons.Rounded.Fullscreen,
          contentDescription = null,
          tint =
            if (checked) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(22.dp),
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.theme_hide_system_bars),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = stringResource(R.string.theme_hide_system_bars_desc),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.testTag("theme_hide_system_bars_switch"),
      )
    }
  }
}

@Composable
private fun WallpaperShortcutCard(
  onClick: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier =
      Modifier.fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(22.dp))
        .clickable(onClick = onClick)
        .testTag("theme_wallpaper_row"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
      ) {
        Icon(
          imageVector = Icons.Rounded.Wallpaper,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(21.dp),
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.theme_wallpaper_change),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = stringResource(R.string.theme_wallpaper_change_desc),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun IconPackShortcutCard(
  activePackLabel: String?,
  onClick: () -> Unit,
) {
  val subtitleText = activePackLabel ?: stringResource(R.string.theme_icon_pack_default)
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier =
      Modifier.fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(22.dp))
        .clickable(onClick = onClick)
        .testTag("theme_icon_pack_row"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
      ) {
        Icon(
          imageVector = Icons.Rounded.GridView,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.size(21.dp),
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.theme_section_icon_pack),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = subtitleText,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
