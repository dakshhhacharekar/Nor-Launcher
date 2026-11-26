package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WallpaperConfig
import com.example.data.model.WallpaperPreset
import com.example.ui.theme.LocalIsDarkTheme
import kotlin.math.roundToInt

/**
 * Bottom sheet for changing the Home screen wallpaper:
 * - Curated built-in live presets (Nordic Peaks, Forest Horizon, Aurora Glass, Sunset Dunes)
 * - Custom photo via Android's zero-permission Photo Picker (PickVisualMedia)
 * - Backdrop dim slider for readable widgets/icons over bright photos
 * - Optional one-tap apply to Android system wallpaper + system wallpaper picker shortcut
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperPickerSheet(
  wallpaperConfig: WallpaperConfig,
  customBitmap: ImageBitmap?,
  onSelectPreset: (WallpaperPreset) -> Unit,
  onPhotoPicked: (Uri) -> Unit,
  onDimPercentChange: (Int) -> Unit,
  onApplyToSystemWallpaper: () -> Unit,
  onOpenSystemWallpaperPicker: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val isDark = LocalIsDarkTheme.current

  val photoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
      if (uri != null) {
        onPhotoPicked(uri)
      }
    }

  val presets =
    buildList {
      if (wallpaperConfig.hasCustomPhoto && customBitmap != null) {
        add(WallpaperPreset.CUSTOM_PHOTO to R.string.wallpaper_preset_custom)
      }
      add(WallpaperPreset.NORDIC_PEAKS to R.string.wallpaper_preset_nordic)
      add(WallpaperPreset.CYBER_MESH to R.string.wallpaper_preset_cyber_mesh)
      add(WallpaperPreset.MATCHA_AURA to R.string.wallpaper_preset_matcha_aura)
      add(WallpaperPreset.MIDNIGHT_VAPOR to R.string.wallpaper_preset_midnight_vapor)
      add(WallpaperPreset.AURORA_GLASS to R.string.wallpaper_preset_aurora)
      add(WallpaperPreset.FOREST_HORIZON to R.string.wallpaper_preset_forest)
      add(WallpaperPreset.SUNSET_DUNES to R.string.wallpaper_preset_sunset)
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.testTag("wallpaper_picker_sheet"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 18.dp)
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        Text(
          text = stringResource(R.string.wallpaper_sheet_title),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = stringResource(R.string.wallpaper_sheet_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Visual preset cards carousel
      LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        items(items = presets, key = { it.first.id }) { (preset, labelRes) ->
          val selected = wallpaperConfig.preset == preset
          val cardShape = RoundedCornerShape(18.dp)
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
              Modifier.width(96.dp)
                .clip(cardShape)
                .clickable { onSelectPreset(preset) }
                .testTag("wallpaper_preset_${preset.id}"),
          ) {
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(132.dp)
                  .clip(cardShape)
                  .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color =
                      if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant,
                    shape = cardShape,
                  ),
            ) {
              WallpaperPresetThumbnail(
                preset = preset,
                customBitmap = customBitmap,
                darkTheme = isDark,
                modifier = Modifier.fillMaxSize(),
              )
              if (selected) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier =
                    Modifier.align(Alignment.TopEnd)
                      .padding(6.dp)
                      .size(24.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primary),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(15.dp),
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = stringResource(labelRes),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
              color =
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              textAlign = TextAlign.Center,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Backdrop readability dim slider
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.wallpaper_dim_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "${wallpaperConfig.dimPercent}%",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Slider(
          value = wallpaperConfig.dimPercent.toFloat(),
          onValueChange = { onDimPercentChange(it.roundToInt()) },
          valueRange = 0f..60f,
          modifier = Modifier.fillMaxWidth().testTag("wallpaper_dim_slider"),
        )
      }

      Spacer(modifier = Modifier.height(6.dp))
      HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(horizontal = 20.dp),
      )
      Spacer(modifier = Modifier.height(14.dp))

      // Zero-permission Photo Picker button
      Button(
        onClick = {
          photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        shape = RoundedCornerShape(16.dp),
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp)
            .minimumInteractiveComponentSize()
            .testTag("wallpaper_pick_photo_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.PhotoLibrary,
          contentDescription = null,
          modifier = Modifier.size(19.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.wallpaper_pick_photo))
      }

      // Apply custom photo to Android system wallpaper (when custom photo exists)
      if (wallpaperConfig.hasCustomPhoto && customBitmap != null) {
        Spacer(modifier = Modifier.height(10.dp))
        FilledTonalButton(
          onClick = onApplyToSystemWallpaper,
          shape = RoundedCornerShape(16.dp),
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 20.dp)
              .minimumInteractiveComponentSize()
              .testTag("wallpaper_apply_system_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.PhoneAndroid,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = stringResource(R.string.wallpaper_apply_system))
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Open Android System Wallpaper Picker
      OutlinedButton(
        onClick = onOpenSystemWallpaperPicker,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp)
            .minimumInteractiveComponentSize()
            .testTag("wallpaper_system_picker_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Wallpaper,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.wallpaper_open_system_picker))
      }
    }
  }
}
