package com.example.data.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability {
  AVAILABLE,
  NOT_ENROLLED,
  UNAVAILABLE,
}

object BiometricAuthHelper {
  private const val ALLOWED_AUTHENTICATORS = BIOMETRIC_STRONG or BIOMETRIC_WEAK

  fun checkAvailability(context: Context): BiometricAvailability {
    val manager = BiometricManager.from(context)
    return when (manager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
      BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
      else -> BiometricAvailability.UNAVAILABLE
    }
  }

  fun authenticate(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    negativeButtonText: String,
    onSuccess: () -> Unit,
    onUsePin: () -> Unit,
    onLockout: () -> Unit,
    onCanceled: () -> Unit,
  ) {
    val executor = ContextCompat.getMainExecutor(activity)
    val callback =
      object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
          onSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
          when (errorCode) {
            BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onUsePin()
            BiometricPrompt.ERROR_LOCKOUT,
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> onLockout()
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_CANCELED -> onCanceled()
            else -> onUsePin()
          }
        }
      }

    val promptInfo =
      BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setNegativeButtonText(negativeButtonText)
        .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
        .setConfirmationRequired(false)
        .build()

    runCatching { BiometricPrompt(activity, executor, callback).authenticate(promptInfo) }
      .onFailure { onUsePin() }
  }
}
