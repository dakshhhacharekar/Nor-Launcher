package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppItem
import com.example.data.model.BuiltInCategory
import com.example.data.model.CategoryChipItem
import com.example.data.model.CustomCategory
import kotlin.math.ceil

/**
 * Compact, variable-height Translucent Glassmorphism App Drawer:
 * - Adapts its height smoothly to the number of apps in the active category (small for few apps,
 *   taller for larger categories, capped cleanly with scrollable grid).
 * - Tapping the blurred scrim above the glass sheet or swiping down on the header closes it.
 * - Header: Category title + app count + Search + Play Store favicon + 3-Dot launcher menu
 *   (Theming, Gestures, Hidden apps, New category, Notification dots, Home widget).
 */
@Composable
fun GlassyAppDrawer(
  categoryTitle: String,
  apps: List<AppItem>,
  isLoading: Boolean,
  categories: List<CategoryChipItem>,
  selectedCategoryId: String,
  searchQuery: String,
  isSearchExpanded: Boolean,
  isNotificationAccessGranted: Boolean,
  gridState: LazyGridState,
  onToggleSearch: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onClearSearch: () -> Unit,
  onOpenPlayStore: () -> Unit,
  onOpenTheming: () -> Unit,
  onOpenIconPackPicker: () -> Unit,
  onOpenWallpaperPicker: () -> Unit,
  onOpenGestures: () -> Unit,
  onOpenHiddenApps: () -> Unit,
  onCreateCategoryClick: () -> Unit,
  onOpenNotificationSettings: () -> Unit,
  onOpenWidgetPicker: () -> Unit,
  onCustomizeWidgetClick: () -> Unit,
  onCategorySelect: (String) -> Unit,
  onCustomCategoryLongPress: (CustomCategory) -> Unit,
  onAppClick: (AppItem) -> Unit,
  onAppLongClick: (AppItem) -> Unit,
  onDismissDrawer: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var overflowExpanded by remember { mutableStateOf(false) }
  var dragAccumulator by remember { mutableFloatStateOf(0f) }
  val searchFocusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  val drawerHandleCd = stringResource(R.string.cd_drawer_handle)

  LaunchedEffect(isSearchExpanded) {
    if (isSearchExpanded) {
      runCatching { searchFocusRequester.requestFocus() }
    } else {
      focusManager.clearFocus()
    }
  }

  val drawerShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
  val tintBase = MaterialTheme.colorScheme.surfaceContainer

  // Translucent glassmorphism gradient letting the blurred home wallpaper glow through clearly
  val glassFillBrush =
    Brush.verticalGradient(
      colors =
        listOf(
          Color(0xFF0E1526).copy(alpha = 0.50f),
          Color(0xFF131D34).copy(alpha = 0.62f),
          tintBase.copy(alpha = 0.34f).compositeOverSafe(Color(0xFF0B1120).copy(alpha = 0.74f)),
        )
    )

  // Luminous glass specular sheen on top rim
  val glassBorderBrush =
    Brush.verticalGradient(
      colors =
        listOf(
          Color.White.copy(alpha = 0.36f),
          Color.White.copy(alpha = 0.10f),
          Color.Transparent,
        )
    )

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.14f))
        .imePadding()
        .testTag("glassy_app_drawer"),
  ) {
    val columns = computeGridColumns(maxWidth)
    val rowCount =
      if (apps.isEmpty()) 1 else ceil(apps.size / columns.toFloat()).toInt().coerceAtLeast(1)

    // Each category drawer resizes dynamically to fit its rows compactly without wasting vertical space
    val reservedChromeHeight = if (isSearchExpanded || searchQuery.isNotEmpty()) 236.dp else 176.dp
    val availableForGrid = (maxHeight - reservedChromeHeight).coerceAtLeast(120.dp)
    val maxGridHeight = (maxHeight * 0.56f).coerceAtMost(availableForGrid).coerceIn(120.dp, 500.dp)
    val targetGridHeight =
      when {
        isLoading -> 152.dp.coerceAtMost(maxGridHeight)
        apps.isEmpty() -> 156.dp.coerceAtMost(maxGridHeight)
        else -> {
          val rawHeight = (rowCount * 96 + (rowCount - 1) * 14 + 24).dp
          rawHeight.coerceIn(116.dp, maxGridHeight)
        }
      }

    val animatedGridHeight by
      animateDpAsState(
        targetValue = targetGridHeight,
        animationSpec =
          spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
          ),
        label = "drawer_variable_grid_height",
      )

    // Tapping open blurred space above the compact drawer sheet dismisses back to Home
    Box(
      modifier =
        Modifier.fillMaxSize()
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onDismissDrawer,
          )
          .testTag("drawer_scrim_dismiss"),
    )

    // Bottom-anchored compact Glassmorphism Drawer Sheet
    Box(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .fillMaxWidth()
          .widthIn(max = 680.dp)
          .statusBarsPadding()
          .clip(drawerShape)
          .background(glassFillBrush)
          .border(width = 1.dp, brush = glassBorderBrush, shape = drawerShape)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {}, // Absorb clicks inside the glass sheet so they don't dismiss
          )
          .navigationBarsPadding(),
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Swipe-down drag handle + header zone (both support smooth downward swipe dismissal)
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .pointerInput(Unit) {
                detectVerticalDragGestures(
                  onDragStart = { dragAccumulator = 0f },
                  onVerticalDrag = { _, dragAmount ->
                    if (dragAmount > 0f) dragAccumulator += dragAmount
                  },
                  onDragEnd = {
                    if (dragAccumulator > 75f) {
                      onDismissDrawer()
                    }
                    dragAccumulator = 0f
                  },
                  onDragCancel = { dragAccumulator = 0f },
                )
              },
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.fillMaxWidth()
                .semantics { contentDescription = drawerHandleCd }
                .padding(top = 12.dp, bottom = 6.dp)
                .testTag("drawer_drag_handle"),
          ) {
            Box(
              modifier =
                Modifier.width(42.dp)
                  .height(4.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.44f))
            )
          }

          // Header Row: Category Title + subtle count + Search + Play Store favicon + 3-dot Menu
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
              Modifier.fillMaxWidth()
                .padding(start = 22.dp, end = 10.dp, top = 2.dp, bottom = 6.dp)
                .testTag("drawer_header"),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              Text(
                text = categoryTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (!isLoading && apps.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                  contentAlignment = Alignment.Center,
                  modifier =
                    Modifier.clip(CircleShape)
                      .background(Color.White.copy(alpha = 0.15f))
                      .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                  Text(
                    text = apps.size.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.88f),
                  )
                }
              }
            }

            IconButton(
              onClick = onToggleSearch,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("drawer_search_button"),
            ) {
              Icon(
                imageVector = if (isSearchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                contentDescription = stringResource(R.string.cd_toggle_search),
                tint = Color.White.copy(alpha = 0.94f),
                modifier = Modifier.size(23.dp),
              )
            }

            // Play Store favicon button
            IconButton(
              onClick = onOpenPlayStore,
              modifier =
                Modifier.minimumInteractiveComponentSize().testTag("drawer_play_store_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.ShoppingBag,
                contentDescription = stringResource(R.string.cd_open_play_store),
                tint = Color.White.copy(alpha = 0.94f),
                modifier = Modifier.size(22.dp),
              )
            }

            // 3-Dot Launcher Menu button (Hidden Apps, Theming, Gestures, Categories, Dots, Widget)
            Box {
              IconButton(
                onClick = { overflowExpanded = true },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("drawer_more_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.MoreVert,
                  contentDescription = stringResource(R.string.cd_drawer_more_menu),
                  tint = Color.White.copy(alpha = 0.94f),
                  modifier = Modifier.size(23.dp),
                )
              }

              DropdownMenu(
                expanded = overflowExpanded,
                onDismissRequest = { overflowExpanded = false },
                modifier = Modifier.testTag("drawer_overflow_menu"),
              ) {
                DropdownMenuItem(
                  text = { Text(stringResource(R.string.menu_hidden_apps)) },
                  leadingIcon = {
                    Icon(imageVector = Icons.Rounded.VisibilityOff, contentDescription = null)
                  },
                  onClick = {
                    overflowExpanded = false
                    onOpenHiddenApps()
                  },
                  modifier = Modifier.testTag("menu_item_hidden_apps"),
                )

                DropdownMenuItem(
                  text = { Text(stringResource(R.string.menu_theming)) },
                  leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Palette, contentDescription = null)
                  },
                  onClick = {
                    overflowExpanded = false
                    onOpenTheming()
                  },
                  modifier = Modifier.testTag("menu_item_theming"),
                )

                DropdownMenuItem(
                  text = { Text(stringResource(R.string.menu_icon_pack)) },
                  leadingIcon = {
                    Icon(imageVector = Icons.Rounded.GridView, contentDescription = null)
                  },
                  onClick = {
                    overflowExpanded = false
                    onOpenIconPackPicker()
                  },
                  modifier = Modifier.testTag("menu_item_icon_pack"),
                )

                DropdownMenuItem(
                  text = { Text(stringResource(R.string.menu_wallpaper)) },
                  leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Wallpaper, contentDescription = null)
                  },
                  onClick = {
                    overflowExpanded = false
                    onOpenWallpaperPicker()
                  },
                  modifier = Modifier.testTag("menu_item_wallpaper"),
                )

                DropdownMenuItem(
                  text = { Text(stringResource(R.string.menu_gestures)) },
                  leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Gesture, contentDescription = null)
                  },
                  onClick = {
                    overflowExpanded = false
                    onOpenGestures()
                  },
                  modifier = Modifier.testTag("menu_item_gestures"),
                )

                DropdownMenuItem(
                  text = { Text(stringResource(R.string.menu_create_category)) },
                  leadingIcon = {
                    Icon(imageVector = Icons.Rounded.CreateNewFolder, contentDescription = null)
                  },
                  onClick = {
                    overflowExpanded = false
                    onCreateCategoryClick()
                  },
                  modifier = Modifier.testTag("menu_item_create_category"),
                )

                DropdownMenuItem(
                  text = {
                    Column {
                      Text(stringResource(R.string.menu_notification_dots))
                      Text(
                        text =
                          if (isNotificationAccessGranted) {
                            stringResource(R.string.menu_notification_dots_on)
                          } else {
                            stringResource(R.string.menu_notification_dots_off)
                          },
                        style = MaterialTheme.typography.labelSmall,
                        color =
                          if (isNotificationAccessGranted) MaterialTheme.colorScheme.secondary
                          else MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  },
                  leadingIcon = {
                    Icon(imageVector = Icons.Rounded.NotificationsActive, contentDescription = null)
                  },
                  onClick = {
                    overflowExpanded = false
                    onOpenNotificationSettings()
                  },
                  modifier = Modifier.testTag("menu_item_notification_dots"),
                )

                DropdownMenuItem(
                  text = { Text(stringResource(R.string.menu_add_widget)) },
                  leadingIcon = {
                    Icon(imageVector = Icons.Rounded.AddBox, contentDescription = null)
                  },
                  onClick = {
                    overflowExpanded = false
                    onOpenWidgetPicker()
                  },
                  modifier = Modifier.testTag("menu_item_add_widget"),
                )

                DropdownMenuItem(
                  text = { Text(stringResource(R.string.menu_customize_widget)) },
                  leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Widgets, contentDescription = null)
                  },
                  onClick = {
                    overflowExpanded = false
                    onCustomizeWidgetClick()
                  },
                  modifier = Modifier.testTag("menu_item_customize_widget"),
                )
              }
            }
          }
        }

        // Expandable frosted glass search pill
        AnimatedVisibility(
          visible = isSearchExpanded || searchQuery.isNotEmpty(),
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut(),
        ) {
          Surface(
            color = Color.White.copy(alpha = 0.16f),
            shape = CircleShape,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp)
                .height(48.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = stringResource(R.string.search_icon_cd),
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                  Text(
                    text = stringResource(R.string.search_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.68f),
                  )
                }
                BasicTextField(
                  value = searchQuery,
                  onValueChange = onSearchQueryChange,
                  singleLine = true,
                  textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                  cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                  keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                  modifier =
                    Modifier.fillMaxWidth()
                      .focusRequester(searchFocusRequester)
                      .testTag("search_input"),
                )
              }
              if (searchQuery.isNotEmpty()) {
                IconButton(
                  onClick = onClearSearch,
                  modifier = Modifier.testTag("clear_search_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.search_clear_cd),
                    tint = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }
        }

        // Variable-height app grid adapted to the active category's app count
        Box(modifier = Modifier.fillMaxWidth().height(animatedGridHeight)) {
          AppGrid(
            apps = apps,
            isLoading = isLoading,
            searchQuery = searchQuery,
            selectedCategoryTitle = categoryTitle,
            gridState = gridState,
            onAppClick = onAppClick,
            onAppLongClick = onAppLongClick,
            labelColor = Color.White.copy(alpha = 0.96f),
            secondaryTextColor = Color.White.copy(alpha = 0.78f),
            badgeBorderColor = Color(0xFF141D35),
          )
        }

        // Bottom Frosted Glass Category Favicon Nav Pill with active underline indicator
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
          GlassyCategoryFaviconBar(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelect = onCategorySelect,
            onCustomCategoryLongPress = onCustomCategoryLongPress,
            onCreateCategoryClick = onCreateCategoryClick,
            modifier = Modifier.widthIn(max = 560.dp),
          )
        }
      }
    }
  }
}

private fun Color.compositeOverSafe(background: Color): Color {
  val fgA = alpha
  val bgA = background.alpha
  val outA = fgA + bgA * (1f - fgA)
  if (outA <= 0f) return Color.Transparent
  val r = (red * fgA + background.red * bgA * (1f - fgA)) / outA
  val g = (green * fgA + background.green * bgA * (1f - fgA)) / outA
  val b = (blue * fgA + background.blue * bgA * (1f - fgA)) / outA
  return Color(r, g, b, outA)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlassyCategoryFaviconBar(
  categories: List<CategoryChipItem>,
  selectedCategoryId: String,
  onCategorySelect: (String) -> Unit,
  onCustomCategoryLongPress: (CustomCategory) -> Unit,
  onCreateCategoryClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val pillShape = RoundedCornerShape(28.dp)

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(pillShape)
        .background(
          Brush.horizontalGradient(
            colors =
              listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.18f),
              )
          )
        )
        .border(width = 1.dp, color = Color.White.copy(alpha = 0.30f), shape = pillShape)
        .testTag("category_chip_row"),
  ) {
    LazyRow(
      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth(),
    ) {
      items(items = categories, key = { it.id }) { chip ->
        val selected = chip.id == selectedCategoryId
        val iconTint = if (selected) Color.White else Color.White.copy(alpha = 0.72f)

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(18.dp))
              .background(if (selected) Color.White.copy(alpha = 0.16f) else Color.Transparent)
              .combinedClickable(
                onClick = { onCategorySelect(chip.id) },
                onLongClick =
                  if (chip.isCustom) {
                    {
                      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                      onCustomCategoryLongPress(CustomCategory(id = chip.id, name = chip.name))
                    }
                  } else {
                    null
                  },
              )
              .padding(horizontal = 11.dp, vertical = 8.dp)
              .semantics { contentDescription = chip.name }
              .testTag("category_chip_${chip.id}"),
        ) {
          Icon(
            imageVector = drawerCategoryFavicon(chip),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(23.dp),
          )
          Spacer(modifier = Modifier.height(5.dp))
          Box(
            modifier =
              Modifier.width(if (selected) 22.dp else 0.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
          )
        }
      }

      item(key = "create_category_icon") {
        val createCd = stringResource(R.string.cd_create_category)
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .clip(CircleShape)
              .combinedClickable(onClick = onCreateCategoryClick)
              .padding(horizontal = 10.dp, vertical = 8.dp)
              .semantics { contentDescription = createCd }
              .testTag("create_category_chip"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.82f),
            modifier = Modifier.size(22.dp),
          )
        }
      }
    }
  }
}

private fun drawerCategoryFavicon(chip: CategoryChipItem): ImageVector {
  if (chip.isCustom) return Icons.Rounded.Bookmark
  return when (chip.builtIn ?: BuiltInCategory.OTHER) {
    BuiltInCategory.COMMUNICATION -> Icons.Rounded.ChatBubbleOutline
    BuiltInCategory.INTERNET -> Icons.Rounded.Public
    BuiltInCategory.GAMES -> Icons.Rounded.SportsEsports
    BuiltInCategory.MEDIA -> Icons.Rounded.PermMedia
    BuiltInCategory.PRODUCTIVITY -> Icons.Rounded.WorkOutline
    BuiltInCategory.UTILITIES -> Icons.Rounded.Category
    BuiltInCategory.OTHER -> Icons.Rounded.Settings
  }
}
