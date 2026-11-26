package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.widgets.LauncherWidgetHostManager
import com.example.ui.screens.LauncherScreen
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.NorLauncherTheme
import com.example.ui.viewmodel.LauncherViewModel

class MainActivity : FragmentActivity() {

  private val viewModel: LauncherViewModel by viewModels { LauncherViewModel.Factory }
  private var hideSystemBarsEnabled: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      val themeConfig = uiState.themeConfig

      NorLauncherTheme(themeConfig = themeConfig) {
        val isDarkTheme = LocalIsDarkTheme.current

        LaunchedEffect(themeConfig.hideSystemBars, isDarkTheme) {
          hideSystemBarsEnabled = themeConfig.hideSystemBars
          applySystemBarsVisibility(
            hideBars = themeConfig.hideSystemBars,
            darkTheme = isDarkTheme,
          )
        }

        LauncherScreen(viewModel = viewModel)
      }
    }
  }

  override fun onStart() {
    super.onStart()
    viewModel.widgetHostManager.startListening()
  }

  override fun onResume() {
    super.onResume()
    // Re-check Notification Access & Accessibility Service state after returning from Settings.
    viewModel.refreshSystemAccessStates()
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus && hideSystemBarsEnabled) {
      applySystemBarsVisibility(hideBars = true, darkTheme = null)
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.action == Intent.ACTION_MAIN) {
      viewModel.onHomePressed()
    }
  }

  @Deprecated("Required for AppWidgetHost.startAppWidgetConfigureActivityForResult on Android 12+")
  @Suppress("DEPRECATION")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      LauncherWidgetHostManager.REQUEST_CONFIGURE_WIDGET -> {
        viewModel.onWidgetConfigureResult(
          resultOk = resultCode == Activity.RESULT_OK,
          isReconfigure = false,
        )
      }
      LauncherWidgetHostManager.REQUEST_RECONFIGURE_WIDGET -> {
        viewModel.onWidgetConfigureResult(
          resultOk = resultCode == Activity.RESULT_OK,
          isReconfigure = true,
        )
      }
    }
  }

  override fun onStop() {
    super.onStop()
    viewModel.widgetHostManager.stopListening()
    // Re-lock Hidden Apps when leaving the launcher or turning the screen off,
    // without locking during orientation/configuration changes.
    if (!isChangingConfigurations) {
      viewModel.lockHiddenApps()
    }
  }

  private fun applySystemBarsVisibility(hideBars: Boolean, darkTheme: Boolean?) {
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if (darkTheme != null) {
      controller.isAppearanceLightStatusBars = !darkTheme
      controller.isAppearanceLightNavigationBars = !darkTheme
    }
    if (hideBars) {
      controller.hide(WindowInsetsCompat.Type.systemBars())
    } else {
      controller.show(WindowInsetsCompat.Type.systemBars())
    }
  }
}
