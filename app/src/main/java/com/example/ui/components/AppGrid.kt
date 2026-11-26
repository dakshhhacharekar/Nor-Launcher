package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppItem
import com.example.ui.theme.LocalIconShapeOption
import com.example.ui.theme.shapeForIcon

fun computeGridColumns(maxWidth: Dp): Int =
  when {
    maxWidth >= 840.dp -> 6
    maxWidth >= 600.dp -> 5
    else -> 4
  }

@Composable
fun AppGrid(
  apps: List<AppItem>,
  isLoading: Boolean,
  searchQuery: String,
  selectedCategoryTitle: String,
  gridState: LazyGridState,
  onAppClick: (AppItem) -> Unit,
  onAppLongClick: (AppItem) -> Unit,
  labelColor: Color = MaterialTheme.colorScheme.onBackground,
  secondaryTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  badgeBorderColor: Color = MaterialTheme.colorScheme.surface,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val columns = computeGridColumns(maxWidth)

    when {
      isLoading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(
            modifier = Modifier.testTag("apps_loading_indicator"),
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
      apps.isEmpty() -> {
        EmptyGridState(
          isSearch = searchQuery.isNotBlank(),
          categoryTitle = selectedCategoryTitle,
          titleColor = labelColor,
          subtitleColor = secondaryTextColor,
          modifier = Modifier.fillMaxSize(),
        )
      }
      else -> {
        LazyVerticalGrid(
          columns = GridCells.Fixed(columns),
          state = gridState,
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier.fillMaxSize().testTag("app_grid"),
        ) {
          items(items = apps, key = { it.packageName }) { app ->
            AppGridTile(
              app = app,
              onClick = { onAppClick(app) },
              onLongClick = { onAppLongClick(app) },
              labelColor = labelColor,
              badgeBorderColor = badgeBorderColor,
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridTile(
  app: AppItem,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  labelColor: Color = MaterialTheme.colorScheme.onBackground,
  badgeBorderColor: Color = MaterialTheme.colorScheme.surface,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val tileShape = RoundedCornerShape(16.dp)

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier =
      modifier
        .fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clip(tileShape)
        .combinedClickable(
          onClick = onClick,
          onLongClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongClick()
          },
        )
        .padding(horizontal = 4.dp, vertical = 6.dp)
        .testTag("app_tile_${app.packageName}"),
  ) {
    AppIconBadge(
      app = app,
      sizeDp = 54,
      showNotificationDot = true,
      badgeBorderColor = badgeBorderColor,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = app.label,
      style = MaterialTheme.typography.labelMedium,
      color = labelColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
fun AppIconBadge(
  app: AppItem,
  sizeDp: Int,
  showNotificationDot: Boolean = true,
  elevated: Boolean = false,
  badgeBorderColor: Color = MaterialTheme.colorScheme.surface,
  modifier: Modifier = Modifier,
) {
  val iconShape = shapeForIcon(LocalIconShapeOption.current)
  val badgeDesc =
    if (showNotificationDot && app.notificationCount > 0) {
      stringResource(R.string.cd_notification_badge, app.label, app.notificationCount)
    } else {
      stringResource(R.string.cd_app_icon, app.label)
    }

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier.size(sizeDp.dp).semantics { contentDescription = badgeDesc },
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.fillMaxSize()
          .then(
            if (elevated) {
              Modifier.shadow(elevation = 6.dp, shape = iconShape, clip = false)
            } else {
              Modifier
            }
          )
          .clip(iconShape)
          .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
      if (app.icon != null) {
        Image(
          bitmap = app.icon,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Text(
          text = app.label.take(1).uppercase(),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }

    // Step 3 live notification dot / unread count badge
    if (showNotificationDot && app.notificationCount > 0) {
      val badgeLabel = if (app.notificationCount > 99) "99+" else app.notificationCount.toString()
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.align(Alignment.TopEnd)
            .offset(x = 4.dp, y = (-4).dp)
            .widthIn(min = 20.dp)
            .height(20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .border(width = 1.5.dp, color = badgeBorderColor, shape = CircleShape)
            .padding(horizontal = 5.dp)
            .testTag("notif_dot_${app.packageName}"),
      ) {
        Text(
          text = badgeLabel,
          color = MaterialTheme.colorScheme.onPrimary,
          fontSize = 10.sp,
          lineHeight = 12.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun EmptyGridState(
  isSearch: Boolean,
  categoryTitle: String,
  titleColor: Color,
  subtitleColor: Color,
  modifier: Modifier = Modifier,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier.padding(horizontal = 28.dp).testTag("empty_grid_state"),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.size(52.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(Color.White.copy(alpha = 0.14f)),
    ) {
      Icon(
        imageVector = if (isSearch) Icons.Rounded.SearchOff else Icons.Rounded.Apps,
        contentDescription = null,
        tint = titleColor,
        modifier = Modifier.size(26.dp),
      )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text =
        if (isSearch) {
          stringResource(R.string.empty_search_title)
        } else {
          stringResource(R.string.empty_category_title, categoryTitle)
        },
      style = MaterialTheme.typography.titleMedium,
      color = titleColor,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text =
        if (isSearch) {
          stringResource(R.string.empty_search_subtitle)
        } else {
          stringResource(R.string.empty_category_subtitle)
        },
      style = MaterialTheme.typography.bodyMedium,
      color = subtitleColor,
      textAlign = TextAlign.Center,
    )
  }
}
