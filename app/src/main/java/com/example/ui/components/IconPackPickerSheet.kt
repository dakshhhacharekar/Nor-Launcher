package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.InstalledIconPack
import com.example.ui.theme.LocalIconShapeOption
import com.example.ui.theme.shapeForIcon

/**
 * One-handed ModalBottomSheet picker for selecting installed Android icon packs
 * (parsed via `appfilter.xml`) or reverting to system default icons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPackPickerSheet(
  installedPacks: List<InstalledIconPack>,
  selectedPackPackage: String?,
  isLoading: Boolean,
  onSelectPack: (String?) -> Unit,
  onRefresh: () -> Unit,
  onBrowsePlayStore: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.testTag("icon_pack_picker_sheet"),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(max = 560.dp)
          .navigationBarsPadding()
          .padding(bottom = 18.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 4.dp),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.icon_pack_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = stringResource(R.string.icon_pack_sheet_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = onRefresh,
          enabled = !isLoading,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("icon_pack_refresh_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = stringResource(R.string.icon_pack_refresh),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
      ) {
        // System Default option (always first)
        item(key = "system_default_pack") {
          val isDefaultSelected = selectedPackPackage.isNullOrBlank()
          SystemDefaultPackCard(
            selected = isDefaultSelected,
            onClick = { onSelectPack(null) },
          )
        }

        if (isLoading) {
          item(key = "loading_packs") {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
              modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
            ) {
              CircularProgressIndicator(strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = stringResource(R.string.icon_pack_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        } else if (installedPacks.isEmpty()) {
          item(key = "empty_packs") {
            EmptyIconPacksCard(
              onBrowsePlayStore = onBrowsePlayStore,
              onRefresh = onRefresh,
            )
          }
        } else {
          items(items = installedPacks, key = { it.packageName }) { pack ->
            InstalledIconPackCard(
              pack = pack,
              selected = pack.packageName == selectedPackPackage,
              onClick = { onSelectPack(pack.packageName) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SystemDefaultPackCard(
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    color =
      if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
      else MaterialTheme.colorScheme.surfaceContainerHigh,
    shape = RoundedCornerShape(20.dp),
    border =
      BorderStroke(
        width = if (selected) 1.5.dp else 1.dp,
        color =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.outlineVariant,
      ),
    modifier =
      Modifier.fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onClick)
        .testTag("icon_pack_option_default"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(44.dp)
            .clip(shapeForIcon(LocalIconShapeOption.current))
            .background(
              if (selected) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceContainerHighest
            ),
      ) {
        Icon(
          imageVector = Icons.Rounded.Apps,
          contentDescription = null,
          tint =
            if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(22.dp),
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.theme_icon_pack_default),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color =
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = stringResource(R.string.theme_icon_pack_default_desc),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (selected) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(26.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary),
        ) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun InstalledIconPackCard(
  pack: InstalledIconPack,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val iconShape = shapeForIcon(LocalIconShapeOption.current)

  Surface(
    color =
      if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
      else MaterialTheme.colorScheme.surfaceContainerHigh,
    shape = RoundedCornerShape(20.dp),
    border =
      BorderStroke(
        width = if (selected) 1.5.dp else 1.dp,
        color =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.outlineVariant,
      ),
    modifier =
      Modifier.fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onClick)
        .testTag("icon_pack_option_${pack.packageName}"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        if (pack.icon != null) {
          Image(
            bitmap = pack.icon,
            contentDescription = pack.label,
            modifier = Modifier.size(44.dp).clip(iconShape),
          )
        } else {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(44.dp)
                .clip(iconShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
          ) {
            Icon(
              imageVector = Icons.Rounded.GridView,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(22.dp),
            )
          }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = pack.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color =
              if (selected) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = stringResource(R.string.icon_pack_mapped_count, pack.mappedIconCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (selected) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
          ) {
            Icon(
              imageVector = Icons.Rounded.Check,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }

      if (pack.previewIcons.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          pack.previewIcons.forEachIndexed { index, previewBitmap ->
            Image(
              bitmap = previewBitmap,
              contentDescription = null,
              modifier =
                Modifier.size(36.dp)
                  .clip(iconShape)
                  .testTag("icon_pack_preview_${pack.packageName}_$index"),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyIconPacksCard(
  onBrowsePlayStore: () -> Unit,
  onRefresh: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth().testTag("icon_pack_empty_state"),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth().padding(20.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
      ) {
        Icon(
          imageVector = Icons.Rounded.GridView,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(24.dp),
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = stringResource(R.string.icon_pack_empty_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = stringResource(R.string.icon_pack_empty_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        FilledTonalButton(
          onClick = onBrowsePlayStore,
          shape = RoundedCornerShape(14.dp),
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("icon_pack_browse_store_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.ShoppingBag,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.icon_pack_browse_play_store),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        OutlinedButton(
          onClick = onRefresh,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Text(text = stringResource(R.string.icon_pack_refresh))
        }
      }
    }
  }
}
