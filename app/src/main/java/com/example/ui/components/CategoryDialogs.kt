package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CustomCategory

@Composable
fun CategoryNameDialog(
  title: String,
  confirmLabel: String,
  initialName: String,
  errorMessage: String?,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var name by rememberSaveable(initialName) { mutableStateOf(initialName) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(text = title) },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = name,
          onValueChange = { if (it.length <= 24) name = it },
          label = { Text(stringResource(R.string.create_category_label)) },
          placeholder = { Text(stringResource(R.string.create_category_placeholder)) },
          singleLine = true,
          isError = errorMessage != null,
          supportingText =
            errorMessage?.let { msg ->
              { Text(text = msg, color = MaterialTheme.colorScheme.error) }
            },
          keyboardOptions =
            KeyboardOptions(
              capitalization = KeyboardCapitalization.Words,
              imeAction = ImeAction.Done,
            ),
          keyboardActions = KeyboardActions(onDone = { onConfirm(name) }),
          modifier = Modifier.fillMaxWidth().testTag("category_name_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(name) },
        modifier = Modifier.testTag("category_name_confirm_button"),
      ) {
        Text(text = confirmLabel)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("category_name_cancel_button"),
      ) {
        Text(text = stringResource(R.string.action_cancel))
      }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCustomCategorySheet(
  category: CustomCategory,
  onDismiss: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.testTag("manage_custom_category_sheet"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)
    ) {
      Text(
        text = stringResource(R.string.manage_category_title, category.name),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
      )
      Spacer(modifier = Modifier.height(6.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Spacer(modifier = Modifier.height(4.dp))

      ActionSheetItem(
        icon = Icons.Rounded.Edit,
        label = stringResource(R.string.manage_category_rename),
        onClick = onRename,
        testTag = "manage_category_rename_action",
      )

      ActionSheetItem(
        icon = Icons.Rounded.Delete,
        label = stringResource(R.string.manage_category_delete),
        tint = MaterialTheme.colorScheme.error,
        onClick = onDelete,
        testTag = "manage_category_delete_action",
      )
    }
  }
}

@Composable
fun DeleteCategoryConfirmDialog(
  category: CustomCategory,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(text = stringResource(R.string.delete_category_confirm_title, category.name)) },
    text = { Text(text = stringResource(R.string.delete_category_confirm_body)) },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
        modifier = Modifier.testTag("confirm_delete_category_button"),
      ) {
        Text(text = stringResource(R.string.action_delete))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("cancel_delete_category_button"),
      ) {
        Text(text = stringResource(R.string.action_cancel))
      }
    },
  )
}
