package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun NotificationOnboardingScreen(
  onOpenSettings: () -> Unit,
  onSkip: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = modifier.fillMaxSize().testTag("notification_onboarding_screen"),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
        Modifier.fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.weight(1f).widthIn(max = 460.dp),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(84.dp)
              .clip(RoundedCornerShape(26.dp))
              .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
          Icon(
            imageVector = Icons.Rounded.NotificationsActive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(40.dp),
          )
          Box(
            modifier =
              Modifier.align(Alignment.TopEnd)
                .padding(14.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
          text = stringResource(R.string.notif_onboarding_title),
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = stringResource(R.string.notif_onboarding_body),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
          color = MaterialTheme.colorScheme.surfaceContainer,
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(16.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.PrivacyTip,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = stringResource(R.string.notif_onboarding_privacy),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Bottom-anchored one-handed action buttons
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
      ) {
        Button(
          onClick = onOpenSettings,
          shape = RoundedCornerShape(16.dp),
          modifier =
            Modifier.fillMaxWidth()
              .height(54.dp)
              .minimumInteractiveComponentSize()
              .testTag("notif_onboarding_enable_button"),
        ) {
          Text(
            text = stringResource(R.string.notif_onboarding_enable),
            style = MaterialTheme.typography.titleSmall,
          )
        }

        OutlinedButton(
          onClick = onSkip,
          shape = RoundedCornerShape(16.dp),
          modifier =
            Modifier.fillMaxWidth()
              .height(50.dp)
              .minimumInteractiveComponentSize()
              .testTag("notif_onboarding_skip_button"),
        ) {
          Text(
            text = stringResource(R.string.notif_onboarding_skip),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}
