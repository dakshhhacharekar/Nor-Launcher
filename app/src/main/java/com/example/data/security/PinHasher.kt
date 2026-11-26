package com.example.data.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HashedPin(
  val hashBase64: String,
  val saltBase64: String,
  val iterations: Int,
)

object PinHasher {
  const val MIN_PIN_LENGTH = 4
  const val MAX_PIN_LENGTH = 8
  const val DEFAULT_ITERATIONS = 120_000
  private const val SALT_BYTES = 16
  private const val KEY_LENGTH_BITS = 256
  private const val ALGORITHM = "PBKDF2WithHmacSHA256"

  private val secureRandom = SecureRandom()

  fun isValidPinFormat(pin: String): Boolean =
    pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it in '0'..'9' }

  suspend fun hashPin(pin: String): HashedPin =
    withContext(Dispatchers.Default) {
      val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
      val pinChars = pin.toCharArray()
      try {
        val derived = deriveKey(pinChars, salt, DEFAULT_ITERATIONS)
        HashedPin(
          hashBase64 = Base64.encodeToString(derived, Base64.NO_WRAP),
          saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
          iterations = DEFAULT_ITERATIONS,
        )
      } finally {
        pinChars.fill('\u0000')
      }
    }

  suspend fun verifyPin(pin: String, stored: HashedPin): Boolean =
    withContext(Dispatchers.Default) {
      if (!isValidPinFormat(pin) || stored.iterations <= 0) return@withContext false

      val expectedHash =
        runCatching { Base64.decode(stored.hashBase64, Base64.NO_WRAP) }.getOrNull()
          ?: return@withContext false
      val salt =
        runCatching { Base64.decode(stored.saltBase64, Base64.NO_WRAP) }.getOrNull()
          ?: return@withContext false

      val pinChars = pin.toCharArray()
      try {
        val computed = deriveKey(pinChars, salt, stored.iterations)
        MessageDigest.isEqual(computed, expectedHash)
      } finally {
        pinChars.fill('\u0000')
      }
    }

  private fun deriveKey(pinChars: CharArray, salt: ByteArray, iterations: Int): ByteArray {
    val spec = PBEKeySpec(pinChars, salt, iterations, KEY_LENGTH_BITS)
    return try {
      val factory = SecretKeyFactory.getInstance(ALGORITHM)
      factory.generateSecret(spec).encoded
    } finally {
      spec.clearPassword()
    }
  }
}
