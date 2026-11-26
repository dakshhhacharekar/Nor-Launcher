package com.example.data.model

import androidx.compose.ui.graphics.ImageBitmap

data class InstalledIconPack(
  val packageName: String,
  val label: String,
  val icon: ImageBitmap?,
  val mappedIconCount: Int,
  val previewIcons: List<ImageBitmap> = emptyList(),
)
