package com.example.data.search

import com.example.data.model.CalculatorResult
import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

/**
 * Pure, safe recursive-descent arithmetic evaluator for typed Smart Search expressions.
 * Supports +, -, *, ×, /, ÷, ^, parentheses, unary signs, decimals, percentage suffix (e.g. 18% * 240),
 * and implicit multiplication (e.g. 2(3+4)).
 */
object MathExpressionEvaluator {

  private val allowedCharsRegex = Regex("""^[\d\s+\-*/×÷%^().,]+$""")
  private val operatorChars = setOf('+', '-', '*', '×', '/', '÷', '%', '^', '(', ')')

  fun evaluate(rawQuery: String): CalculatorResult? {
    val trimmed = rawQuery.trim()
    if (trimmed.length < 2 || trimmed.length > 96) return null
    if (!allowedCharsRegex.matches(trimmed)) return null

    // Require at least one digit AND at least one operator/paren so plain phone numbers or years don't trigger.
    val hasDigit = trimmed.any { it.isDigit() }
    val hasOperator = trimmed.any { it in operatorChars }
    if (!hasDigit || !hasOperator) return null

    // Normalize common symbols & strip thousands commas only when surrounded by digits properly;
    // keep '.' as decimal point.
    val normalized =
      trimmed
        .replace('×', '*')
        .replace('÷', '/')
        .replace(" ", "")

    // Reject phone-number-like patterns such as "(555) 123-4567" or "555-1234"
    if (looksLikePhoneNumber(trimmed)) return null

    val value = runCatching { Parser(normalized).parse() }.getOrNull() ?: return null
    if (!value.isFinite()) return null

    val formatted = formatNumber(value) ?: return null
    return CalculatorResult(expression = trimmed, formattedResult = formatted)
  }

  private fun looksLikePhoneNumber(input: String): Boolean {
    val digitsOnly = input.count { it.isDigit() }
    val hasOnlyPhonePunctuation =
      input.all { it.isDigit() || it == '-' || it == ' ' || it == '(' || it == ')' || it == '+' }
    if (!hasOnlyPhonePunctuation) return false
    // e.g., +1 555-1234 or 555-123-4567 (7+ digits with hyphens/parens and no * / ^ %)
    val hasPlusMiddle = input.indexOf('+', startIndex = 1) >= 0
    return digitsOnly >= 7 && !hasPlusMiddle && input.contains('-')
  }

  private fun formatNumber(value: Double): String? {
    if (abs(value) > 1e15) {
      return String.format(Locale.US, "%.6g", value)
    }
    val rounded =
      runCatching { BigDecimal(value, MathContext(12)).stripTrailingZeros() }.getOrNull()
        ?: return null
    val clean = if (rounded.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else rounded
    val symbols = DecimalFormatSymbols(Locale.US)
    val df =
      DecimalFormat("#,##0.##########", symbols).apply {
        isGroupingUsed = abs(clean.toDouble()) >= 10_000.0
      }
    return df.format(clean)
  }

  private class Parser(private val src: String) {
    private var pos = 0

    fun parse(): Double {
      val result = parseAddSub()
      if (pos != src.length) throw IllegalArgumentException("Unexpected trailing tokens")
      return result
    }

    private fun parseAddSub(): Double {
      var left = parseMulDiv()
      while (pos < src.length) {
        when (src[pos]) {
          '+' -> {
            pos++
            left += parseMulDiv()
          }
          '-' -> {
            pos++
            left -= parseMulDiv()
          }
          else -> break
        }
      }
      return left
    }

    private fun parseMulDiv(): Double {
      var left = parsePower()
      while (pos < src.length) {
        when (src[pos]) {
          '*' -> {
            pos++
            left *= parsePower()
          }
          '/' -> {
            pos++
            val denom = parsePower()
            if (denom == 0.0) throw ArithmeticException("Division by zero")
            left /= denom
          }
          '(' -> {
            // Implicit multiplication: 2(3+4)
            left *= parsePower()
          }
          else -> break
        }
      }
      return left
    }

    private fun parsePower(): Double {
      val base = parseUnary()
      if (pos < src.length && src[pos] == '^') {
        pos++
        val exp = parsePower()
        return base.pow(exp)
      }
      return base
    }

    private fun parseUnary(): Double {
      if (pos < src.length) {
        if (src[pos] == '+') {
          pos++
          return parseUnary()
        }
        if (src[pos] == '-') {
          pos++
          return -parseUnary()
        }
      }
      return parsePostfix()
    }

    private fun parsePostfix(): Double {
      var value = parsePrimary()
      while (pos < src.length && src[pos] == '%') {
        pos++
        value /= 100.0
      }
      return value
    }

    private fun parsePrimary(): Double {
      if (pos >= src.length) throw IllegalArgumentException("Unexpected end")
      if (src[pos] == '(') {
        pos++
        val inside = parseAddSub()
        if (pos >= src.length || src[pos] != ')') {
          throw IllegalArgumentException("Missing closing parenthesis")
        }
        pos++
        return inside
      }

      val start = pos
      var seenDot = false
      while (pos < src.length) {
        val c = src[pos]
        if (c.isDigit()) {
          pos++
        } else if (c == '.' && !seenDot) {
          seenDot = true
          pos++
        } else {
          break
        }
      }
      if (pos == start) throw IllegalArgumentException("Expected number at $pos")
      return src.substring(start, pos).toDouble()
    }
  }
}
